package com.jkbff.ao.tyrlib.chat;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import aoChatLib.Crypto;

import com.jkbff.ao.tyrlib.packets.BaseClientPacket;
import com.jkbff.ao.tyrlib.packets.BaseServerPacket;
import com.jkbff.ao.tyrlib.packets.client.LoginRequest;
import com.jkbff.ao.tyrlib.packets.client.LoginSelect;
import com.jkbff.ao.tyrlib.packets.client.Ping;
import com.jkbff.ao.tyrlib.packets.server.CharacterList;
import com.jkbff.ao.tyrlib.packets.server.FriendUpdate;
import com.jkbff.ao.tyrlib.packets.server.LoginError;
import com.jkbff.ao.tyrlib.packets.server.LoginOk;
import com.jkbff.ao.tyrlib.packets.server.LoginSeed;
import com.jkbff.ao.tyrlib.packets.server.Pong;

public class AOSocket extends Thread {

    private ChatPacketListener chatPacketListener;
    private ChatPacketSender chatPacketSender;
    private ChatPacketHandler chatPacketHandler;
    private String username;
    private String password;
    private String character;
    private String server;
    private int portNumber;
    private Socket socket;
    private STATUS loginStatus = STATUS.WAITING_FOR_SEED;
    
    private Map<Long, Friend> friendlist = new HashMap<Long, Friend>();
    
    private long lastReceivedPing = 0;
    public static String PING_PAYLOAD = "abcdefghijklmnopqrstuvwxyzabcdefghi";
    private int pingInterval = 60000;
    
    private Logger log = Logger.getLogger(this.getClass());
    
    private Long characterId = null;
    
    volatile boolean shouldStop = false;

    enum STATUS {
        WAITING_FOR_SEED, WAITING_FOR_CHAR_LIST, WAITING_FOR_LOGIN_OK, LOGGED_ON
    };
    
    public AOSocket(ChatPacketHandler chatPacketHandler) {
    	this.chatPacketHandler = chatPacketHandler;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(server, portNumber);

            chatPacketListener = new ChatPacketListener(socket.getInputStream(), this);
            chatPacketListener.setName("chatPacketListener");
            chatPacketListener.start();

            chatPacketSender = new ChatPacketSender(socket.getOutputStream(), this);
            chatPacketSender.setName("chatPacketSender");
            chatPacketSender.start();
            
            // send pings periodically to keep the connection alive
            lastReceivedPing = System.currentTimeMillis();
            while (!shouldStop) {
            	if (loginStatus == STATUS.LOGGED_ON) {
            		sendPacket(new Ping(PING_PAYLOAD));
            	}

            	synchronized (this) {
            		try {
    					this.wait(pingInterval);
    				} catch (InterruptedException e) {
    					log.error(e);
    				}
            	}
            	
            	if (System.currentTimeMillis() - lastReceivedPing > (2 * pingInterval)) {
            		log.error("ping reply not received past two times");
            		shutdown();
            	}
            }
        } catch (UnknownHostException e) {
            log.error("Could not connect to chat server " + server + ":" + portNumber, e);
        } catch (IOException e) {
        	log.error("Could not connect to chat server " + server + ":" + portNumber, e);
        } finally {
        	stopAllThreads();
        	chatPacketHandler.shutdownEvent();
        	try {
        		if (socket != null) {
        			socket.close();
        		}
        	} catch (IOException e) {
        		log.error(e);
        	}
        }
    }
    
    public void shutdown() {
    	shouldStop = true;
    	
    	// wake up from waiting to send next ping
    	synchronized (this) {
    		notify();
    	}
    }
    
    private void stopAllThreads() {
    	log.info(character + " shutting down.");
    	
    	// threads have up to five seconds each to shutdown
    	long start = System.currentTimeMillis();
    	try {
    		if (chatPacketListener != null) {
    			chatPacketListener.join(5000);
    		}
    		if (chatPacketSender != null) {
    			chatPacketSender.join(5000);
    		}
		} catch (InterruptedException e) {
			log.error("", e);
		}
		log.info("Shut down time for " + character + ": " + (System.currentTimeMillis() - start));
    }

    void processIncomingPacket(BaseServerPacket packet) {
    	log.debug("SERVER " + packet);
    	
        // If logged on, dispatch packet to handlers, otherwise, complete login sequence
        if (loginStatus == STATUS.LOGGED_ON) {
        	if (packet instanceof Pong) {
        		lastReceivedPing = System.currentTimeMillis();
        	}
        	if (packet instanceof FriendUpdate) {
        		FriendUpdate friendUpdate = (FriendUpdate)packet;
        		friendlist.put(friendUpdate.getCharId(), new Friend(friendUpdate.getCharId(), friendUpdate.getOnline() == 0 ? false : true, friendUpdate.getStatus()));
        	}
        	
            processPacket(packet);
        } else if (packet instanceof LoginSeed) {
            LoginSeed loginSeed = (LoginSeed) packet;

            String randomPrefix = Crypto.randomHexString(8);
            String loginString = username + "|" + loginSeed.getSeed() + "|" + password;

            String key = Crypto.generateKey(randomPrefix, loginString);

            LoginRequest loginRequest = new LoginRequest(0, username, key);
            sendPacket(loginRequest);
            loginStatus = STATUS.WAITING_FOR_CHAR_LIST;
        } else if (packet instanceof CharacterList) {
            CharacterList characterListPacket = (CharacterList) packet;

            for (CharacterList.LoginUser loginUser : characterListPacket.getLoginUsers()) {
                if (character.equalsIgnoreCase(loginUser.getName())) {
                	characterId = loginUser.getUserId();
                    break;
                }
            }

            if (characterId == null) {
                throw new RuntimeException("Could not find character with name '" + character + "' on account '" + username + "'");
            }

            LoginSelect selectCharacterPacket = new LoginSelect(characterId);
            sendPacket(selectCharacterPacket);
            loginStatus = STATUS.WAITING_FOR_LOGIN_OK;
        } else if (packet instanceof LoginOk) {
            loginStatus = STATUS.LOGGED_ON;

            // this is sent to dispatcher so logged in event can be handled
            processPacket(packet);
        } else if (packet instanceof LoginError) {
        	shutdown();
            throw new RuntimeException(((LoginError)packet).getMessage());
        }
    }
    
    private void processPacket(BaseServerPacket packet) {
   		chatPacketHandler.processPacket(packet, this);
    }

    public void sendPacket(BaseClientPacket packet) {
    	log.debug("CLIENT " + packet);
        chatPacketSender.sendPacket(packet);
    }
    
    public Boolean isOnline(long charId) {
    	Friend friend = friendlist.get(charId);
    	if (friend == null) {
    		return null;
    	} else {
    		return friend.online;
    	}
    }
    
    public Map<Long, Friend> getFriendlist() { return friendlist; }
    
    public String getCharacter() { return character; }
    public Long getCharacterId() { return characterId; }
    public int getPingInterval() { return pingInterval; }
    public void setPingInterval(int pingInterval) { this.pingInterval = pingInterval; }
    public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }
	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }
	public String getServer() { return server; }
	public void setServer(String server) { this.server = server; }
	public int getPortNumber() { return portNumber; }
	public void setPortNumber(int portNumber) { this.portNumber = portNumber; }
	public void setCharacter(String character) { this.character = character; }

	private class Friend {
    	private long charid;
    	private boolean online;
    	private String status;
    	
    	private Friend(long charid, boolean online, String status) {
    		this.charid = charid;
    		this.online = online;
    		this.status = status;
    	}
    }
}

package com.jkbff.ao.tyrlib.packets.client;

import java.io.DataInputStream;
import java.io.IOException;

import sk.sigp.aobot.client.types.AbstractType;
import sk.sigp.aobot.client.types.Int;
import sk.sigp.aobot.client.types.Text;

/**
 * @description Client sends a request to login. 
 * @expects 
 * @author Jason
 *
 */
public class LoginRequest extends BaseClientPacket {
	
	public static final int TYPE = 2;

	protected final Int unknownInt;
	protected final Text username;
	protected final Text key;
	
	public LoginRequest(DataInputStream input) {
		unknownInt = new Int(input);
		username = new Text(input);
		key = new Text(input);
	}
	
	public LoginRequest(int unknownInt, String username, String key) {
		this.unknownInt = new Int(unknownInt);
		this.username = new Text(username);
		this.key = new Text(key);
	}
	
	public int getUnknownInt() {
		return unknownInt.getData();
	}
	
	public String getUsername() {
		return username.getData();
	}
	
	public String getKey() {
		return key.getData();
	}

	public int getPacketType() {
		return LoginRequest.TYPE;
	}

	@Override
	public AbstractType[] getParameters() {
		return new AbstractType[]{unknownInt, username, key};
	}
}

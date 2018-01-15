package com.jkbff.ao.tyrlib.packets.client;

import sk.sigp.aobot.client.types.Text;
import sk.sigp.aobot.client.types.TextArray;

import java.io.DataInputStream;
import java.io.IOException;

public class ChatCommandPacket extends BaseClientPacket {
	
	public static final int TYPE = 120;

	protected final TextArray command;
	
	public ChatCommandPacket(DataInputStream input) {
		this.command = new TextArray(input);
	}
	
	public ChatCommandPacket(String[] command) {
		this.command = new TextArray(command);
	}
	
	public Text[] getCommand() {
		return command.getData();
	}
	
	public byte[] getBytes() throws IOException {
		return getBytes(command);
	}
	
	public int getPacketType() {
		return ChatCommandPacket.TYPE;
	}
	
	public String toString() {
		StringBuffer output = new StringBuffer()
			.append(TYPE).append(" ").append(this.getClass().getSimpleName());
		
		int i = 0;
		for (Text text: getCommand()) {
			output.append("\n\tCommand #").append(i++).append(": ").append(text);
		}
		
		return output.toString();
	}
}

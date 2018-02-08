package com.jkbff.ao.tyrlib.packets.server;

import java.io.DataInputStream;
import java.io.IOException;

import sk.sigp.aobot.client.types.CharacterId;

public class PrivateChannelKicked extends BaseServerPacket {

	public static final int TYPE = 51;

	protected final CharacterId privateChannelId;

	public PrivateChannelKicked(DataInputStream input) {
		this.privateChannelId = new CharacterId(input);
	}
	
	public PrivateChannelKicked(long privateChannelId) {
		this.privateChannelId = new CharacterId(privateChannelId);
	}
	
	public long getPrivateChannelId() {
		return this.privateChannelId.getData();
	}
	
	public int getPacketType() {
		return PrivateChannelKicked.TYPE;
	}
	
	public byte[] getBytes() throws IOException {
		return getBytes(privateChannelId);
	}
	
	public String toString() {
		String output = new StringBuffer()
			.append(TYPE).append(" ").append(this.getClass().getSimpleName())
			.append("\n\tPrivateChannelId: ").append(privateChannelId)
			.toString();
	
		return output;
	}
}

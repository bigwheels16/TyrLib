package com.jkbff.ao.tyrlib.packets.server;

import java.io.DataInputStream;
import java.io.IOException;

import sk.sigp.aobot.client.types.Text;
import sk.sigp.aobot.client.types.CharacterId;

import com.jkbff.ao.tyrlib.packets.BaseServerPacket;

public class CharacterUpdate extends BaseServerPacket {
	
	public static final int TYPE = 20;
	
	private CharacterId charId;
	private Text characterName;

	public CharacterUpdate(DataInputStream input) {
		this.charId = new CharacterId(input);
		this.characterName = new Text(input);
	}
	
	public CharacterUpdate(long charId, String characterName) {
		this.charId = new CharacterId(charId);
		this.characterName = new Text(characterName);
	}
	
	public long getCharId() {
		return this.charId.getLongData();
	}
	
	public String getCharacterName() {
		return this.characterName.getStringData();
	}
	
	public int getPacketType() {
		return CharacterUpdate.TYPE;
	}
	
	public byte[] getBytes() throws IOException {
		return getBytes(charId, characterName);
	}
	
	public String toString() {
		String output = new StringBuffer()
			.append(TYPE).append(" ").append(this.getClass().getSimpleName())
			.append("\n\tCharId: ").append(charId)
			.append("\n\tCharaterName: ").append(characterName)
		.toString();
	
		return output;
	}
}
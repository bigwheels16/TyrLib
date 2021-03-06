package com.jkbff.ao.tyrlib.packets.server;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import com.jkbff.ao.tyrlib.chat.MMDBParser;
import com.jkbff.ao.tyrlib.packets.ExtendedMessageParser;
import sk.sigp.aobot.client.types.AbstractType;
import sk.sigp.aobot.client.types.ChannelId;
import sk.sigp.aobot.client.types.Text;
import sk.sigp.aobot.client.types.CharacterId;

import com.jkbff.ao.tyrlib.packets.ExtendedMessage;

public class PublicChannelMessage extends BaseServerPacket {

	public static final int TYPE = 65;

	protected final ChannelId channelId;
	protected final CharacterId charId;
	protected final Text message;
	protected final Text raw;

	public PublicChannelMessage(DataInputStream input) {
		this.channelId = new ChannelId(input);
		this.charId = new CharacterId(input);
		this.message = new Text(input);
		this.raw = new Text(input);
	}
	
	public PublicChannelMessage(long channelId, long charId, String message, String raw) {
		this.channelId = new ChannelId(channelId);
		this.charId = new CharacterId(charId);
		this.message = new Text(message);
		this.raw = new Text(raw);
	}
	
	public long getChannelId() {
		return channelId.getData();
	}

	public long getCharId() {
		return charId.getData();
	}

	public String getMessage() {
		return message.getData();
	}

	public String getRaw() {
		return raw.getData();
	}

	public int getPacketType() {
		return PublicChannelMessage.TYPE;
	}
	
	public ExtendedMessage getExtendedMessage(MMDBParser mmdbParser) throws IOException {
		String parseMessage = message.getData();
		if (charId.getData() == 0 && parseMessage.startsWith("~&") && parseMessage.endsWith("~")) {
			// remove leading ~& and trailing ~
			parseMessage = parseMessage.substring(2, parseMessage.length() - 1);

			ExtendedMessageParser extendedMessageParser = new ExtendedMessageParser(mmdbParser);

			return extendedMessageParser.parse(new DataInputStream(new ByteArrayInputStream(parseMessage.getBytes("UTF-8"))));
		} else {
			return null;
		}
	}

	@Override
	public AbstractType[] getParameters() {
		return new AbstractType[]{channelId, charId, message, raw};
	}
}

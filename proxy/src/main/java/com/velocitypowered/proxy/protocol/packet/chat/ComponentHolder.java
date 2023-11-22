package com.velocitypowered.proxy.protocol.packet.chat;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class ComponentHolder {

	public static ComponentHolder EMPTY = new ComponentHolder(null, Component.empty());
	static {
		EMPTY.json = "{\"text\":\"\"}";
		EMPTY.binaryTag = StringBinaryTag.stringBinaryTag("");
	}

	private final ProtocolVersion version;
	private @MonotonicNonNull Component component;
	private @MonotonicNonNull String json;
	private @MonotonicNonNull BinaryTag binaryTag;

	public ComponentHolder(ProtocolVersion version, Component component) {
		this.version = version;
		this.component = component;
	}

	public ComponentHolder(ProtocolVersion version, String json) {
		this.version = version;
		this.json = json;
	}

	public ComponentHolder(ProtocolVersion version, BinaryTag binaryTag) {
		this.version = version;
		this.binaryTag = binaryTag;
	}

	public Component getComponent() {
		if (component == null) {
			if (json != null) {
				component = ProtocolUtils.getJsonChatSerializer(version).deserialize(json);
			} else if (binaryTag != null) {
				//TODO component = deserialize(binaryTag);
				throw new UnsupportedOperationException("binary tag -> component not implemented yet");
			}
		}
		return component;
	}

	public String getJson() {
		if (json == null) {
			json = ProtocolUtils.getJsonChatSerializer(version).serialize(getComponent());
		}
		return json;
	}

	public BinaryTag getBinaryTag() {
		if (binaryTag == null) {
			//TODO binaryTag = serialize(getComponent());
			throw new UnsupportedOperationException("component -> binary tag not implemented yet");
		}
		return binaryTag;
	}

	public static ComponentHolder read(ByteBuf buf, ProtocolVersion version) {
		if (version.compareTo(ProtocolVersion.MINECRAFT_1_20_3) >= 0) {
			return new ComponentHolder(version, ProtocolUtils.readBinaryTag(buf, version, BinaryTagIO.reader()));
		} else {
			return new ComponentHolder(version, ProtocolUtils.readString(buf));
		}
	}

	public void write(ByteBuf buf, ProtocolVersion version) {
		if (version.compareTo(ProtocolVersion.MINECRAFT_1_20_3) >= 0) {
			ProtocolUtils.writeBinaryTag(buf, version, getBinaryTag());
		} else {
			ProtocolUtils.writeString(buf, getJson());
		}
	}
}

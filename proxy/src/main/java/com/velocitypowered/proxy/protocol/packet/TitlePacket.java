package com.velocitypowered.proxy.protocol.packet;

import com.google.common.primitives.Ints;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.util.DurationUtils;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.kyori.adventure.title.Title;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TitlePacket implements Packet {

  public static final Decoder<TitlePacket> DECODER = (buf, direction, version) -> {
    throw new UnsupportedOperationException();
  };

  public static TitlePacket hide(final ProtocolVersion version) {
    return version.gte(ProtocolVersion.MINECRAFT_1_11)
      ? Instances.HIDE
      : Instances.HIDE_OLD;
  }

  public static TitlePacket reset(final ProtocolVersion version) {
    return version.gte(ProtocolVersion.MINECRAFT_1_11)
      ? Instances.RESET
      : Instances.RESET_OLD;
  }

  public static TitlePacket times(final ProtocolVersion version, final Title.Times times) {
    final int action = version.gte(ProtocolVersion.MINECRAFT_1_11)
      ? SET_TIMES
      : SET_TIMES_OLD;
    return new TitlePacket(
      action,
      (int) DurationUtils.toTicks(times.fadeIn()),
      (int) DurationUtils.toTicks(times.stay()),
      (int) DurationUtils.toTicks(times.fadeOut())
    );
  }

  public static final int SET_TITLE = 0;
  public static final int SET_SUBTITLE = 1;
  public static final int SET_ACTION_BAR = 2;
  public static final int SET_TIMES = 3;
  public static final int HIDE = 4;
  public static final int RESET = 5;

  public static final int SET_TIMES_OLD = 2;
  public static final int HIDE_OLD = 3;
  public static final int RESET_OLD = 4;

  private final int action;
  private final @Nullable String component;
  private final int fadeIn;
  private final int stay;
  private final int fadeOut;

  private TitlePacket(final int action) {
    checkAction(action, HIDE, RESET, HIDE_OLD, RESET_OLD);
    this.action = action;
    this.component = null;
    this.fadeIn = -1;
    this.stay = -1;
    this.fadeOut = -1;
  }

  public TitlePacket(final int action, final String component) {
    checkAction(action, SET_TITLE, SET_SUBTITLE, SET_ACTION_BAR);
    this.action = action;
    this.component = component;
    this.fadeIn = -1;
    this.stay = -1;
    this.fadeOut = -1;
  }

  public TitlePacket(final int action, final int fadeIn, final int stay, final int fadeOut) {
    checkAction(action, SET_TIMES, SET_TIMES_OLD);
    this.action = action;
    this.component = null;
    this.fadeIn = fadeIn;
    this.stay = stay;
    this.fadeOut = fadeOut;
  }

  private static void checkAction(final int action, final int... validActions) {
    if (!Ints.contains(validActions, action)) {
      throw new IllegalArgumentException("Invalid action " + action + ", expected one of: " + Arrays.toString(validActions));
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    ProtocolUtils.writeVarInt(buf, action);
    if (version.gte(ProtocolVersion.MINECRAFT_1_11)) {
      // 1.11+ shifted the action enum by 1 to handle the action bar
      switch (action) {
        case SET_TITLE:
        case SET_SUBTITLE:
        case SET_ACTION_BAR:
          if (component == null) {
            throw new IllegalStateException("No component found for " + action);
          }
          ProtocolUtils.writeString(buf, component);
          break;
        case SET_TIMES:
          buf.writeInt(fadeIn);
          buf.writeInt(stay);
          buf.writeInt(fadeOut);
          break;
        case HIDE:
        case RESET:
          break;
        default:
          throw new UnsupportedOperationException("Unknown action " + action);
      }
    } else {
      switch (action) {
        case SET_TITLE:
        case SET_SUBTITLE:
          if (component == null) {
            throw new IllegalStateException("No component found for " + action);
          }
          ProtocolUtils.writeString(buf, component);
          break;
        case SET_TIMES_OLD:
          buf.writeInt(fadeIn);
          buf.writeInt(stay);
          buf.writeInt(fadeOut);
          break;
        case HIDE_OLD:
        case RESET_OLD:
          break;
        default:
          throw new UnsupportedOperationException("Unknown action " + action);
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public int getAction() {
    return action;
  }

  public @Nullable String getComponent() {
    return component;
  }

  public int getFadeIn() {
    return fadeIn;
  }

  public int getStay() {
    return stay;
  }

  public int getFadeOut() {
    return fadeOut;
  }

  @Override
  public String toString() {
    return "TitlePacket{"
      + "action=" + action
      + ", component='" + component + '\''
      + ", fadeIn=" + fadeIn
      + ", stay=" + stay
      + ", fadeOut=" + fadeOut
      + '}';
  }

  public static final class Instances {
    public static final TitlePacket HIDE = new TitlePacket(TitlePacket.HIDE);
    public static final TitlePacket RESET = new TitlePacket(TitlePacket.RESET);

    public static final TitlePacket HIDE_OLD = new TitlePacket(TitlePacket.HIDE_OLD);
    public static final TitlePacket RESET_OLD = new TitlePacket(TitlePacket.RESET_OLD);

    private Instances() {
    }
  }
}

package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;

/**
 * This event is fired when a plugin message is sent to the proxy, either from a client ({@link com.velocitypowered.api.proxy.Player})
 * or a server ({@link com.velocitypowered.api.proxy.ServerConnection}).
 */
public final class PluginMessageEvent implements ResultedEvent<PluginMessageEvent.ForwardResult> {
    private final ChannelMessageSource source;
    private final ChannelMessageSink target;
    private final ChannelIdentifier identifier;
    private final byte[] data;
    private ForwardResult result;

    public PluginMessageEvent(ChannelMessageSource source, ChannelMessageSink target, ChannelIdentifier identifier, byte[] data) {
        this.source = Preconditions.checkNotNull(source, "source");
        this.target = Preconditions.checkNotNull(target, "target");
        this.identifier = Preconditions.checkNotNull(identifier, "identifier");
        this.data = Preconditions.checkNotNull(data, "data");
        this.result = ForwardResult.forward();
    }

    @Override
    public ForwardResult getResult() {
        return result;
    }

    @Override
    public void setResult(@NonNull ForwardResult result) {
        this.result = Preconditions.checkNotNull(result, "result");
    }

    public ChannelMessageSource getSource() {
        return source;
    }

    public ChannelMessageSink getTarget() {
        return target;
    }

    public ChannelIdentifier getIdentifier() {
        return identifier;
    }

    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    public ByteArrayDataInput dataAsDataStream() {
        return ByteStreams.newDataInput(data);
    }

    @Override
    public String toString() {
        return "PluginMessageEvent{" +
                "source=" + source +
                ", target=" + target +
                ", identifier=" + identifier +
                ", data=" + Arrays.toString(data) +
                ", result=" + result +
                '}';
    }

    /**
     * A result determining whether or not to forward this message on.
     */
    public static final class ForwardResult implements ResultedEvent.Result {
        private static final ForwardResult ALLOWED = new ForwardResult(true);
        private static final ForwardResult DENIED = new ForwardResult(false);

        private final boolean allowed;

        private ForwardResult(boolean b) {
            this.allowed = b;
        }

        @Override
        public boolean isAllowed() {
            return allowed;
        }

        @Override
        public String toString() {
            return allowed ? "forward to sink" : "handled message at proxy";
        }

        public static ForwardResult forward() {
            return ALLOWED;
        }

        public static ForwardResult handled() {
            return DENIED;
        }
    }
}

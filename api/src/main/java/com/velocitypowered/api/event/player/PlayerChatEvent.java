/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.Player;
import java.util.Objects;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired when a player types in a chat message.
 */
public final class PlayerChatEvent implements ResultedEvent<PlayerChatEvent.ChatResult> {

  private final Player player;
  private final String message;
  private ChatResult result;

  /**
   * Constructs a PlayerChatEvent.
   *
   * @param player the player sending the message
   * @param message the message being sent
   */
  public PlayerChatEvent(Player player, String message) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.message = Preconditions.checkNotNull(message, "message");
    this.result = ChatResult.allowed();
  }

  /**
   * Gets the player sending this chat message.
   *
   * @return player sending this chat message
   */
  public Player getPlayer() {
    return player;
  }

  /**
   * Gets the message being sent by the player.
   *
   * @return message being sent by the player
   */
  public String getMessage() {
    return message;
  }

  @Override
  public ChatResult getResult() {
    return result;
  }

  @Override
  public void setResult(ChatResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  @Override
  public String toString() {
    return "PlayerChatEvent{"
            + "player=" + player
            + ", message=" + message
            + ", result=" + result
            + '}';
  }

  /**
   * Represents the result of the {@link PlayerChatEvent}.
   */
  public static final class ChatResult implements ResultedEvent.Result {

    private static final ChatResult GLOBAL = new ChatResult(Destination.GLOBAL);
    private static final ChatResult ALLOWED = new ChatResult(Destination.SERVER);
    private static final ChatResult DENIED = new ChatResult(Destination.NONE);

    private final @Nullable Component message;
    private final @NonNull ChatRenderer renderer;
    private final boolean dirty;
    private final Destination destination;

    private ChatResult(@NonNull Destination destination) {
      this.message = null;
      this.renderer = ChatRenderer.DEFAULT;
      this.dirty = false;
      this.destination = Preconditions.checkNotNull(destination, "destination");
    }

    private ChatResult(@NonNull Destination destination, @Nullable String message) {
      this.message = message == null ? null
              : LegacyComponentSerializer.legacySection().deserialize(message);
      this.renderer = ChatRenderer.DEFAULT;
      this.dirty = false;
      this.destination = Preconditions.checkNotNull(destination, "destination");
    }

    private ChatResult(@NonNull Destination destination, @Nullable Component message,
                       @NonNull ChatRenderer renderer) {
      this.message = message;
      this.renderer = Preconditions.checkNotNull(renderer, "renderer");
      this.dirty = message != null;
      this.destination = Preconditions.checkNotNull(destination, "destination");
    }

    private ChatResult(@NonNull Destination destination, @Nullable String message,
                       @NonNull ChatRenderer renderer) {
      this.message = message == null ? null
              : LegacyComponentSerializer.legacySection().deserialize(message);
      this.renderer = Preconditions.checkNotNull(renderer, "renderer");
      this.dirty = false;
      this.destination = Preconditions.checkNotNull(destination, "destination");
    }

    /**
     * Returns if the overridden chat {@link #message() message} is considered dirty. This is
     * determined by if it was created with {@link #withMessage(String)} or
     * {@link #withMessage(Component)}.
     *
     * <p>If the latter, this will return true and the chat message will not be detectable by
     * proxied servers, and thus will bypass their event listeners and may not render as expected.
     *
     * <p>If the former, this will return false, and the message sent will be purely plain text and
     * therefore detectable as a chat message <b>only if the {@link #renderer() renderer}
     * and {@link #destination() destination} are both default.</b>
     *
     * @return if the chat {@link #message() message} is dirty
     */
    public boolean isDirty() {
      return dirty;
    }

    /**
     * Gets the message which will be treated as the player's input.
     *
     * <p>A {@code null} value indicates that the {@link PlayerChatEvent#getMessage()} will be used.
     *
     * @return player's input message
     */
    public @Nullable String getMessage() {
      return message == null ? null : PlainTextComponentSerializer.plainText().serialize(message);
    }

    /**
     * Gets the message which will be treated as the player's input.
     *
     * <p>A {@code null} value indicates that the {@link PlayerChatEvent#getMessage()} will be used.
     *
     * @return player's input message
     */
    public @Nullable Component message() {
      return message;
    }

    /**
     * Allows the message to be sent, but silently replaced with another.
     *
     * @param message the message to use instead
     * @return a result with a new message
     */
    public static @NonNull ChatResult message(@NonNull String message) {
      return new ChatResult(Destination.SERVER, Preconditions.checkNotNull(message, "message"));
    }

    /**
     * Allows the message to be sent, but silently replaced with another.
     *
     * <p>Setting this value will prevent proxied servers from detecting the chat message as a real
     * chat message.
     *
     * @param message the message to use instead
     * @return a result with a new message
     * @see #isDirty()
     */
    public static @NonNull ChatResult message(@NonNull Component message) {
      return new ChatResult(Destination.SERVER, Preconditions.checkNotNull(message, "message"),
              ChatRenderer.DEFAULT);
    }

    /**
     * Gets the renderer that will be used to format the message.
     *
     * @return renderer that will be used to format the message
     */
    public @NonNull ChatRenderer renderer() {
      return renderer;
    }

    /**
     * Allows the message to be sent, but with a custom chat renderer.
     *
     * <p>Setting this to a value besides {@link ChatRenderer#DEFAULT} will prevent proxied servers
     * from detecting the chat message as a real chat message.
     *
     * @param renderer the renderer to format the message
     * @return a result with a new renderer
     * @see #isDirty()
     */
    public static @NonNull ChatResult renderer(@NonNull ChatRenderer renderer) {
      return new ChatResult(Destination.SERVER, (String) null,
              Preconditions.checkNotNull(renderer, "renderer"));
    }

    /**
     * Allows the message to be sent, but with a custom chat renderer.
     *
     * <p>Setting this to a value besides {@link ChatRenderer#DEFAULT} will prevent proxied servers
     * from detecting the chat message as a real chat message.
     *
     * @param renderer the renderer to format the message
     * @return a result with a new renderer
     * @see #isDirty()
     */
    public static @NonNull ChatResult renderer(@NonNull ViewerUnaware renderer) {
      return new ChatResult(Destination.SERVER, (String) null,
              Preconditions.checkNotNull(renderer, "renderer").asRenderer());
    }

    /**
     * Gets where the chat message will be sent to.
     *
     * @return where the chat message will be sent to
     */
    public @NonNull Destination destination() {
      return destination;
    }

    /**
     * Allows the message to be sent, but with a custom destination.
     *
     * <p>Setting this to a value besides {@link Destination#SERVER} will prevent proxied servers
     * from detecting the chat message as a real chat message.
     *
     * @param destination where to send the chat message
     * @return a result with a new destination
     * @see #isDirty()
     */
    public static @NonNull ChatResult destination(@NonNull Destination destination) {
      return new ChatResult(destination);
    }

    /**
     * Returns a copy of this result with the message silently changed and the {@link #isDirty()
     * dirty} bit unset.
     *
     * @param message the message to use instead
     * @return copy of this result with a new message
     */
    public @NonNull ChatResult withMessage(@Nullable String message) {
      return new ChatResult(destination, message, renderer);
    }

    /**
     * Returns a copy of this result with the message silently changed and the {@link #isDirty()
     * dirty} bit set.
     *
     * @param message the message to use instead
     * @return copy of this result with a new message
     */
    public @NonNull ChatResult withMessage(@Nullable Component message) {
      return new ChatResult(destination, message, renderer);
    }

    /**
     * Returns a copy of this result with the chat renderer changed.
     *
     * <p>Setting this to a value besides {@link ChatRenderer#DEFAULT} will prevent proxied servers
     * from detecting the chat message as a real chat message.
     *
     * @param renderer the chat renderer to use instead
     * @return copy of this result with a new chat renderer
     * @see #isDirty()
     */
    public @NonNull ChatResult withRenderer(@NonNull ChatRenderer renderer) {
      return new ChatResult(destination, message, renderer);
    }

    /**
     * Returns a copy of this result with the chat renderer changed.
     *
     * <p>Setting this to a value besides {@link ChatRenderer#DEFAULT} will prevent proxied servers
     * from detecting the chat message as a real chat message.
     *
     * @param renderer the chat renderer to use instead
     * @return copy of this result with a new chat renderer
     * @see #isDirty()
     */
    public @NonNull ChatResult withRenderer(@NonNull ViewerUnaware renderer) {
      return new ChatResult(destination, message, Preconditions.checkNotNull(renderer, "renderer")
              .asRenderer());
    }

    /**
     * Returns a copy of this result with the destination changed.
     *
     * <p>Setting this to a value besides {@link Destination#SERVER} will prevent proxied servers
     * from detecting the chat message as a real chat message.
     *
     * @param destination the destination to use instead
     * @return copy of this result with a new chat renderer
     * @see #isDirty()
     */
    public @NonNull ChatResult withDestination(@NonNull Destination destination) {
      return new ChatResult(destination, message, renderer);
    }

    @Override
    public boolean isAllowed() {
      return destination != Destination.NONE;
    }

    @Override
    public String toString() {
      return destination != Destination.NONE ? "allowed" : "denied";
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ChatResult)) {
        return false;
      }
      ChatResult that = (ChatResult) o;
      return destination == that.destination
              && Objects.equals(message(), that.message())
              && renderer().equals(that.renderer());
    }

    @Override
    public int hashCode() {
      return Objects.hash(message(), renderer(), destination());
    }

    /**
     * Allows the message to be sent to all proxied servers without modification.
     *
     * <p>This will prevent proxied servers from detecting the chat message as a real chat message.
     *
     * @return the global result
     * @see #isDirty()
     */
    public static ChatResult global() {
      return GLOBAL;
    }

    /**
     * Allows the message to be sent, without modification.
     *
     * @return the allowed result
     */
    public static @NonNull ChatResult allowed() {
      return ALLOWED;
    }

    /**
     * Prevents the message from being sent.
     *
     * @return the denied result
     */
    public static @NonNull ChatResult denied() {
      return DENIED;
    }
  }

  /**
   * A chat renderer is responsible for rendering chat messages sent by {@link Player}s to the
   * server.
   */
  @FunctionalInterface
  public interface ChatRenderer {
    /**
     * A chat renderer which replicates vanilla behavior as closely as it can.
     */
    ChatRenderer DEFAULT = viewerUnaware((source, msg) ->
            Component.translatable("chat.type.text",
                    Component.text(source.getUsername()).hoverEvent(source)
                            .insertion(source.getUsername()), msg));

    /**
     * Renders a chat message. This is called once for each receiving {@link Audience}.
     * An {@link Component#empty() empty} result will skip sending any message.
     *
     * @param source the {@link Player} who sent the message
     * @param message the message the player sent
     * @param viewer the receiving {@link Audience}
     * @return a rendered chat message
     */
    @NonNull Component render(@NonNull Player source, @NonNull Component message,
                              @NonNull Audience viewer);

    /**
     * Creates a new viewer-unaware {@link ChatRenderer}, which will render the chat message a
     * single time and display the same rendered message to every viewing {@link Audience}.
     *
     * @param renderer the viewer unaware renderer
     * @return a new {@link ChatRenderer}
     */
    static @NonNull ChatRenderer viewerUnaware(ViewerUnaware renderer) {
      return new ChatRenderer() {
        private @MonotonicNonNull Component result;

        @Override
        public @NonNull Component render(@NonNull Player source,
                                         @NonNull Component message,
                                         @NonNull Audience viewer) {
          if (result == null) {
            result = renderer.render(source, message);
          }
          return result;
        }
      };
    }
  }

  /**
   * Similar to {@link ChatRenderer}, but without knowledge of the message viewer.
   *
   * @see ChatRenderer#viewerUnaware(ViewerUnaware)
   */
  @FunctionalInterface
  public interface ViewerUnaware {
    /**
     * Renders a chat message. An {@link Component#empty() empty} result will skip sending any
     * message, though you should consider using {@link ChatResult#denied()} instead.
     *
     * @param source the {@link Player} who sent the message
     * @param message the message the player sent
     * @return a rendered chat message
     */
    @NonNull Component render(@NonNull Player source, @NonNull Component message);

    /**
     * Creates a new viewer-unaware {@link ChatRenderer}, which will render the chat message a
     * single time and display the same rendered message to every viewing {@link Audience}.
     *
     * @return a new {@link ChatRenderer}
     */
    default ChatRenderer asRenderer() {
      return ChatRenderer.viewerUnaware(this);
    }
  }

  /**
   * Specifies where the chat message will be broadcast to.
   */
  public enum Destination {
    /**
     * Cancels the chat message.
     */
    NONE,
    /**
     * Sets the chat message to broadcast to the proxied server.
     */
    SERVER,
    /**
     * Sets the chat message to broadcast to all proxied servers.
     *
     * <p>This will prevent proxied servers from detecting the chat message as a real chat message.
     * @see ChatResult#isDirty()
     */
    GLOBAL
  }
}

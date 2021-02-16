package com.velocitypowered.proxy.protocol.packet;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import com.velocitypowered.proxy.protocol.packet.brigadier.ArgumentPropertyRegistry;
import com.velocitypowered.proxy.util.collect.IdentityHashStrategy;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AvailableCommands implements MinecraftPacket {
  private static final Command<CommandSource> PLACEHOLDER_COMMAND = source -> 0;

  private static final byte NODE_TYPE_ROOT = 0x00;
  private static final byte NODE_TYPE_LITERAL = 0x01;
  private static final byte NODE_TYPE_ARGUMENT = 0x02;

  private static final byte FLAG_NODE_TYPE = 0x03;
  private static final byte FLAG_EXECUTABLE = 0x04;
  private static final byte FLAG_IS_REDIRECT = 0x08;
  private static final byte FLAG_HAS_SUGGESTIONS = 0x10;

  private @MonotonicNonNull RootCommandNode<CommandSource> rootNode;

  /**
   * Returns the root node.
   * @return the root node
   */
  public RootCommandNode<CommandSource> getRootNode() {
    if (rootNode == null) {
      throw new IllegalStateException("Packet not yet deserialized");
    }
    return rootNode;
  }

  @Override
  public void decode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    int commands = ProtocolUtils.readVarInt(buf);
    WireNode[] wireNodes = new WireNode[commands];
    for (int i = 0; i < commands; i++) {
      wireNodes[i] = deserializeNode(buf, i);
    }

    // Iterate over the deserialized nodes and attempt to form a graph. We also resolve any cycles
    // that exist.
    Queue<WireNode> nodeQueue = new ArrayDeque<>(Arrays.asList(wireNodes));
    while (!nodeQueue.isEmpty()) {
      boolean cycling = false;

      for (Iterator<WireNode> it = nodeQueue.iterator(); it.hasNext(); ) {
        WireNode node = it.next();
        if (node.toNode(wireNodes)) {
          cycling = true;
          it.remove();
        }
      }

      if (!cycling) {
        // Uh-oh. We can't cycle. This is bad.
        throw new IllegalStateException("Stopped cycling; the root node can't be built.");
      }
    }

    int rootIdx = ProtocolUtils.readVarInt(buf);
    rootNode = (RootCommandNode<CommandSource>) wireNodes[rootIdx].built;
  }

  @Override
  public void encode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    // Assign all the children an index.
    Deque<CommandNode<CommandSource>> childrenQueue = new ArrayDeque<>(ImmutableList.of(rootNode));
    Object2IntMap<CommandNode<CommandSource>> idMappings = new Object2IntLinkedOpenCustomHashMap<>(
        IdentityHashStrategy.instance());
    while (!childrenQueue.isEmpty()) {
      CommandNode<CommandSource> child = childrenQueue.poll();
      if (!idMappings.containsKey(child)) {
        idMappings.put(child, idMappings.size());
        childrenQueue.addAll(child.getChildren());
      }
    }

    // Now serialize the children.
    ProtocolUtils.writeVarInt(buf, idMappings.size());
    for (CommandNode<CommandSource> child : idMappings.keySet()) {
      serializeNode(child, buf, idMappings);
    }
    ProtocolUtils.writeVarInt(buf, idMappings.getInt(rootNode));
  }

  private static void serializeNode(CommandNode<CommandSource> node, ByteBuf buf,
      Object2IntMap<CommandNode<CommandSource>> idMappings) {
    byte flags = 0;
    if (node.getRedirect() != null) {
      flags |= FLAG_IS_REDIRECT;
    }
    if (node.getCommand() != null) {
      flags |= FLAG_EXECUTABLE;
    }

    if (node instanceof LiteralCommandNode<?>) {
      flags |= NODE_TYPE_LITERAL;
    } else if (node instanceof ArgumentCommandNode<?, ?>) {
      flags |= NODE_TYPE_ARGUMENT;
      if (((ArgumentCommandNode<CommandSource, ?>) node).getCustomSuggestions() != null) {
        flags |= FLAG_HAS_SUGGESTIONS;
      }
    } else if (!(node instanceof RootCommandNode<?>)) {
      throw new IllegalArgumentException("Unknown node type " + node.getClass().getName());
    }

    buf.writeByte(flags);
    ProtocolUtils.writeVarInt(buf, node.getChildren().size());
    for (CommandNode<CommandSource> child : node.getChildren()) {
      ProtocolUtils.writeVarInt(buf, idMappings.getInt(child));
    }
    if (node.getRedirect() != null) {
      ProtocolUtils.writeVarInt(buf, idMappings.getInt(node.getRedirect()));
    }

    if (node instanceof ArgumentCommandNode<?, ?>) {
      ProtocolUtils.writeString(buf, node.getName());
      ArgumentPropertyRegistry.serialize(buf,
              ((ArgumentCommandNode<CommandSource, ?>) node).getType());

      if (((ArgumentCommandNode<CommandSource, ?>) node).getCustomSuggestions() != null) {
        SuggestionProvider<CommandSource> provider = ((ArgumentCommandNode<CommandSource, ?>) node)
                .getCustomSuggestions();
        String name = "minecraft:ask_server";
        if (provider instanceof ProtocolSuggestionProvider) {
          name = ((ProtocolSuggestionProvider) provider).name;
        }
        ProtocolUtils.writeString(buf, name);
      }
    } else if (node instanceof LiteralCommandNode<?>) {
      ProtocolUtils.writeString(buf, node.getName());
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  private static WireNode deserializeNode(ByteBuf buf, int idx) {
    byte flags = buf.readByte();
    int[] children = ProtocolUtils.readIntegerArray(buf);
    int redirectTo = -1;
    if ((flags & FLAG_IS_REDIRECT) > 0) {
      redirectTo = ProtocolUtils.readVarInt(buf);
    }

    switch (flags & FLAG_NODE_TYPE) {
      case NODE_TYPE_ROOT:
        return new WireNode(idx, flags, children, redirectTo, null);
      case NODE_TYPE_LITERAL:
        return new WireNode(idx, flags, children, redirectTo, LiteralArgumentBuilder
            .literal(ProtocolUtils.readString(buf)));
      case NODE_TYPE_ARGUMENT:
        String name = ProtocolUtils.readString(buf);
        ArgumentType<?> argumentType = ArgumentPropertyRegistry.deserialize(buf);

        RequiredArgumentBuilder<CommandSource, ?> argumentBuilder = RequiredArgumentBuilder
            .argument(name, argumentType);
        if ((flags & FLAG_HAS_SUGGESTIONS) != 0) {
          argumentBuilder.suggests(new ProtocolSuggestionProvider(ProtocolUtils.readString(buf)));
        }

        return new WireNode(idx, flags, children, redirectTo, argumentBuilder);
      default:
        throw new IllegalArgumentException("Unknown node type " + (flags & FLAG_NODE_TYPE));
    }
  }

  private static class WireNode {
    private final int idx;
    private final byte flags;
    private final int[] children;
    private final int redirectTo;
    private final @Nullable ArgumentBuilder<CommandSource, ?> args;
    private @MonotonicNonNull CommandNode<CommandSource> built;
    private boolean validated;

    private WireNode(int idx, byte flags, int[] children, int redirectTo,
        @Nullable ArgumentBuilder<CommandSource, ?> args) {
      this.idx = idx;
      this.flags = flags;
      this.children = children;
      this.redirectTo = redirectTo;
      this.args = args;
      this.validated = false;
    }

    void validate(WireNode[] wireNodes) {
      // Ensure all children exist. Note that we delay checking if the node has been built yet;
      // that needs to come after this node is built.
      for (int child : children) {
        if (child < 0 || child >= wireNodes.length) {
          throw new IllegalStateException("Node points to non-existent index " + child);
        }
      }

      if (redirectTo != -1) {
        if (redirectTo < 0 || redirectTo >= wireNodes.length) {
          throw new IllegalStateException("Redirect node points to non-existent index "
              + redirectTo);
        }
      }

      this.validated = true;
    }

    boolean toNode(WireNode[] wireNodes) {
      if (!this.validated) {
        this.validate(wireNodes);
      }

      if (this.built == null) {
        int type = flags & FLAG_NODE_TYPE;
        if (type == NODE_TYPE_ROOT) {
          this.built = new RootCommandNode<>();
        } else {
          if (args == null) {
            throw new IllegalStateException("Non-root node without args builder!");
          }

          // Add any redirects
          if (redirectTo != -1) {
            WireNode redirect = wireNodes[redirectTo];
            if (redirect.built != null) {
              args.redirect(redirect.built);
            } else {
              // Redirect node does not yet exist
              return false;
            }
          }

          // If executable, add an empty command
          if ((flags & FLAG_EXECUTABLE) != 0) {
            args.executes(PLACEHOLDER_COMMAND);
          }

          this.built = args.build();
        }
      }

      for (int child : children) {
        if (wireNodes[child].built == null) {
          // The child is not yet deserialized. The node can't be built now.
          return false;
        }
      }

      // Associate children with nodes
      for (int child : children) {
        CommandNode<CommandSource> childNode = wireNodes[child].built;
        if (!(childNode instanceof RootCommandNode)) {
          built.addChild(childNode);
        }
      }

      return true;
    }

    @Override
    public String toString() {
      MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this)
          .add("idx", idx)
          .add("flags", flags)
          .add("children", children)
          .add("redirectTo", redirectTo);

      if (args != null) {
        if (args instanceof LiteralArgumentBuilder) {
          helper.add("argsLabel",
                  ((LiteralArgumentBuilder<CommandSource>) args).getLiteral());
        } else if (args instanceof RequiredArgumentBuilder) {
          helper.add("argsName",
                  ((RequiredArgumentBuilder<CommandSource, ?>) args).getName());
        }
      }

      return helper.toString();
    }
  }

  /**
   * A placeholder {@link SuggestionProvider} used internally to preserve the suggestion provider
   * name.
   */
  public static class ProtocolSuggestionProvider implements SuggestionProvider<CommandSource> {

    private final String name;

    public ProtocolSuggestionProvider(String name) {
      this.name = name;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSource> context,
        SuggestionsBuilder builder) throws CommandSyntaxException {
      return builder.buildFuture();
    }
  }
}

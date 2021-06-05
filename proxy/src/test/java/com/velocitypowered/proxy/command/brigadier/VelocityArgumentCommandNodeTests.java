package com.velocitypowered.proxy.command.brigadier;

import static com.velocitypowered.proxy.command.brigadier.VelocityArgumentBuilder.velocityArgument;
import static org.junit.jupiter.api.Assertions.*;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class VelocityArgumentCommandNodeTests {

  private static final StringArrayArgumentType STRING_ARRAY = StringArrayArgumentType.INSTANCE;

  private CommandContextBuilder<Object> contextBuilder;

  @BeforeEach
  void setUp() {
    final CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
    this.contextBuilder = new CommandContextBuilder<>(dispatcher, new Object(),
            dispatcher.getRoot(), 0);
  }

  @Test
  void testParse() throws CommandSyntaxException {
    final VelocityArgumentCommandNode<Object, String[]> node =
            velocityArgument("foo", STRING_ARRAY).build();
    final StringReader reader = new StringReader("hello world");
    node.parse(reader, this.contextBuilder);

    final StringRange expectedRange = StringRange.between(0, reader.getTotalLength());

    assertFalse(reader.canRead());

    assertFalse(this.contextBuilder.getNodes().isEmpty());
    assertSame(node, this.contextBuilder.getNodes().get(0).getNode());
    assertEquals(expectedRange, this.contextBuilder.getNodes().get(0).getRange());

    assertTrue(this.contextBuilder.getArguments().containsKey("foo"));
    final ParsedArgument<Object, String[]> parsed =
            (ParsedArgument<Object, String[]>) this.contextBuilder.getArguments().get("foo");
    assertArrayEquals(new String[] { "hello", "world" }, parsed.getResult());
    assertEquals(expectedRange, parsed.getRange());
  }

  @Test
  void testDefaultSuggestions() throws CommandSyntaxException {
    final VelocityArgumentCommandNode<Object, String[]> node =
            velocityArgument("foo", STRING_ARRAY).build();
    final Suggestions result = node.listSuggestions(
            this.contextBuilder.build(""), new SuggestionsBuilder("", 0)).join();

    assertTrue(result.isEmpty());
  }

  // This only tests delegation to the given SuggestionsProvider; suggestions merging
  // and filtering is already tested in Brigadier.
  @Test
  void testCustomSuggestions() throws CommandSyntaxException {
    final VelocityArgumentCommandNode<Object, String[]> node =
            velocityArgument("foo", STRING_ARRAY)
              .suggests((context, builder) -> {
                builder.suggest("bar");
                builder.suggest("baz");
                return builder.buildFuture();
              })
            .build();
    final Suggestions result = node.listSuggestions(
            this.contextBuilder.build(""), new SuggestionsBuilder("", 0)).join();

    assertEquals("bar", result.getList().get(0).getText());
    assertEquals("baz", result.getList().get(1).getText());
  }
}

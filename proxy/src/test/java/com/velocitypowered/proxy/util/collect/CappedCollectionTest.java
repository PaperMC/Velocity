package com.velocitypowered.proxy.util.collect;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CappedCollectionTest {

  @Test
  void basicVerification() {
    Collection<String> coll = CappedCollection.newCappedSet(1);
    assertTrue(coll.add("coffee"), "did not add single item");
    assertThrows(IllegalStateException.class, () -> coll.add("tea"),
        "item was added to collection although it is too full");
    assertEquals(1, coll.size(), "collection grew in size unexpectedly");
  }

  @Test
  void testAddAll() {
    Set<String> doesFill1 = ImmutableSet.of("coffee", "tea");
    Set<String> doesFill2 = ImmutableSet.of("chocolate");
    Set<String> overfill = ImmutableSet.of("Coke", "Pepsi");

    Collection<String> coll = CappedCollection.newCappedSet(3);
    assertTrue(coll.addAll(doesFill1), "did not add items");
    assertTrue(coll.addAll(doesFill2), "did not add items");
    assertThrows(IllegalStateException.class, () -> coll.addAll(overfill),
        "items added to collection although it is too full");
    assertEquals(3, coll.size(), "collection grew in size unexpectedly");
  }

  @Test
  void handlesSetBehaviorCorrectly() {
    Set<String> doesFill1 = ImmutableSet.of("coffee", "tea");
    Set<String> doesFill2 = ImmutableSet.of("coffee", "chocolate");
    Set<String> overfill = ImmutableSet.of("coffee", "Coke", "Pepsi");

    Collection<String> coll = CappedCollection.newCappedSet(3);
    assertTrue(coll.addAll(doesFill1), "did not add items");
    assertTrue(coll.addAll(doesFill2), "did not add items");
    assertThrows(IllegalStateException.class, () -> coll.addAll(overfill),
        "items added to collection although it is too full");

    assertFalse(coll.addAll(doesFill1), "added items?!?");

    assertEquals(3, coll.size(), "collection grew in size unexpectedly");
  }
}
package com.velocitypowered.api.proxy;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Indicates an {@link Audience} that is on the proxy. This interface contains no-op default methods
 * that are used to bridge compatibility issues with the new adventure API. This interface will go
 * away in Velocity 2.0.0.
 *
 * @deprecated Only used to handle compatibility problems, will go away in Velocity 2.0.0
 */
@Deprecated
public interface ProxyAudience extends Audience {

  @Override
  void sendMessage(@NonNull Component message);

  @Override
  default void sendActionBar(@NonNull Component message) {

  }

  @Override
  default void showTitle(@NonNull Title title) {

  }

  @Override
  default void clearTitle() {

  }

  @Override
  default void resetTitle() {

  }

  @Override
  default void showBossBar(@NonNull BossBar bar) {

  }

  @Override
  default void hideBossBar(@NonNull BossBar bar) {

  }

  @Override
  default void playSound(@NonNull Sound sound) {

  }

  @Override
  default void playSound(@NonNull Sound sound, double x, double y, double z) {

  }

  @Override
  default void stopSound(@NonNull SoundStop stop) {

  }

  @Override
  default void openBook(@NonNull Book book) {

  }
}

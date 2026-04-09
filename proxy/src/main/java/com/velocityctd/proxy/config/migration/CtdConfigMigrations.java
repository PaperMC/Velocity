/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocityctd.proxy.config.migration;

import com.velocitypowered.proxy.config.migration.ConfigurationMigration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CtdConfigMigrations {

  public static List<ConfigurationMigration> createCtdMigrations() {
    return List.of(
        // root
        migration(
            "Whether chat signing should be enforced. If disabled, backend servers MUST disable chat signing.",
            "enforce-chat-signing",
            true
        ),
        migration(
            "Should tell client that proxy doesn't report chat messages? (useful for NoChatReports mod).",
            "prevents-chat-reports",
            false
        ),
        migration(
            "If false, disables processing of header and footer translations for better performance.",
            "translate-header-footer",
            true
        ),
        migration(
            "If true, a message is pasted in console displaying whether a user joined on an unsupported version.\n"
                + "This corresponds with the \"minimum-version\" and \"modern-forwarding-needs-new-client\" values.",
            "log-minimum-version",
            false
        ),
        migration(
            "Modify the minimum version, so the proxy blocks out users on the wrong version, rather than the backend server.\n"
                + "Modern forwarding supports 1.13, at minimum. Set this to 1.13 or above if you are using modern forwarding.",
            "minimum-version",
            "1.7.2"
        ),
        migration(
            "Modify the maximum version, so the proxy blocks out users on the wrong version, rather than the backend server.\n"
                + "Set to UNBOUNDED for no maximum version (default behavior).",
            "maximum-version",
            "UNBOUNDED"
        ),
        migration(
            "If true, disables handling of inbound Forge handshakes.",
            "disable-forge",
            false
        ),
        migration(
            "If enabled (default is false), kick-existing-players will only kick the existing session when\n"
                + "the new connection originates from the same IP address. A duplicate UUID from a different IP\n"
                + "is denied instead of displacing the existing player. This makes kick-existing-players safe for\n"
                + "offline mode by restricting it to reconnect-after-drop scenarios.\n"
                + "It is recommended to set kick-existing-players-check-ip to true when enabling kick-existing-players\n"
                + "in offline mode.",
            "kick-existing-players-check-ip",
            false
        ),
        migration(
            "If false, disables logging for offline player connections.",
            "log-offline-connections",
            true
        ),
        migration(
            "Enables logging of player connections and by default, still displays\n"
                + "player disconnections and initial connections.",
            "log-player-connections",
            true
        ),
        migration(
            "Enables logging of player disconnection and by default, still displays\n"
                + "player connections and initial connections.",
            "log-player-disconnections",
            true
        ),

        // [commands]
        migration("Velocity Command Registration", "commands.server-enabled", true),
        migration(null, "commands.glist-enabled", true),
        migration(null, "commands.send-enabled", true),
        migration("Velocity-CTD Command Registration", "commands.alert-enabled", true),
        migration(null, "commands.alertraw-enabled", true),
        migration(null, "commands.find-enabled", true),
        migration(null, "commands.hub-enabled", true),
        migration(null, "commands.ping-enabled", true),
        migration(null, "commands.plist-enabled", true),
        migration(null, "commands.transfer-enabled", true),
        migration(
            "Whether to use the default \"/server\" output, or whether to override it "
                + "with the \"velocity.command.server.usage\" key.",
            "commands.override-server-command-usage",
            false
        ),

        // [servers]
        migration(
            "Sends you to the first available fallback server, the least populated\n"
                + " fallback server, or the most populated fallback server.\n"
                + " Available options: \"first_available\", \"least_populated\", \"most_populated\"",
            "servers.dynamic-fallbacks-filter",
            "first_available"
        ),
        migration(
            "The list of aliases for the \"/server\" command when the queue system is enabled.",
            "servers.server-aliases",
            List.of("joinqueue", "queue")
        ),

        // [command-aliases]
        migration(
            "What commands should have aliases for simpler execution that\n"
                + " do not already have a more advanced function or implementation.",
            "command-aliases.hub",
            List.of("lobby", "return")
        ),

        // [proxy-command-aliases]
        migration(
            "Proxy command aliases create new commands that execute other commands when invoked.\n"
                + " This is similar to Bukkit's commands.yml functionality.\n"
                + " Adding multiple aliases executes multiple commands.",
            "proxy-command-aliases.examplealias",
            List.of("velocity help")
        ),

        // [advanced]
        migration(
            "Enables the execution of illegal characters in chat and only allows\n"
                + " or denies illegal characters that are executed through the proxy.",
            "advanced.allow-illegal-characters-in-chat",
            false
        ),
        migration(
            "Modifies the server brand that displays in your debug menu.\n"
                + " Supports placeholders: {protocol-min}, {protocol-max}, {protocol}, {backend-brand},\n"
                + " {backend-brand-custom}, {proxy-brand}, {proxy-brand-custom}, {proxy-version},\n"
                + " {proxy-vendor}, {server-connected}.",
            "advanced.server-brand",
            "{backend-brand} ({proxy-brand})"
        ),
        migration(
            "Modifies the brand and server version that displays in the multiplayer menu and status pingers.\n"
                + " Supports placeholders: {protocol-min}, {protocol-max}, {protocol}, {proxy-brand},\n"
                + " {proxy-brand-custom}, {proxy-version}, {proxy-vendor}, {player-count}, {max-players}.",
            "advanced.fallback-version-ping",
            "{proxy-brand} {protocol-min}-{protocol-max}"
        ),
        migration(
            "Instead of \"fallback-version-ping\" exclusively returning when the user is on an unsupported\n"
                + " version, it is returned regardless of their version and can be used to customize\n"
                + " the player count/max line freely.",
            "advanced.always-fallback-ping",
            false
        ),
        migration(
            "Replaces what is returned for both the server brand and fallback version pinger.",
            "advanced.custom-brand-proxy",
            "Velocity"
        ),
        migration(
            "Replaces what is returned as the server brand for the user's client.",
            "advanced.custom-brand-backend",
            "Paper"
        ),

        // [redis]
        migration(
            "Should Redis be used to communicate between multiple Velocity proxies?",
            "redis.enabled",
            false
        ),
        migration(
            "What address should be used to link all Velocity functions to Redis?",
            "redis.host",
            "127.0.0.1"
        ),
        migration(null, "redis.port", 6379),
        migration(
            "Leave the username blank if you do not have a defined username for your Redis database.",
            "redis.username",
            ""
        ),
        migration(null, "redis.password", ""),
        migration(null, "redis.use-ssl", false),
        migration(
            "Maximum number of maintained connections to the Redis server.",
            "redis.max-concurrent-connections",
            50
        ),
        migration(
            "The ID of this proxy, only needed for multi-proxy setups.\n"
                + " Leave blank if you do not use Redis. Your server will not start if this is blank and Redis is on.",
            "redis.proxy-id",
            ""
        ),

        // [queue]
        migration(
            "Whether the queue system is enabled. This will fully unregister\n"
                + " all permissions, commands, and this feature as a whole.",
            "queue.enabled",
            false
        ),
        migration(
            "The list of IDs of the proxy (in order of priority) that should handle and maintain the queue.\n"
                + " Only necessary in multi-proxy setup, leave blank otherwise.",
            "queue.master-proxy-ids",
            List.of("")
        ),
        migration(
            "The list of aliases for the \"/leavequeue\" command. The command will not be registered if this list is empty.",
            "queue.leave-queue-aliases",
            List.of("leavequeue", "dequeue")
        ),
        migration(
            "The list of aliases for the \"/queueadmin\" command. The command will not be registered if this list is empty.",
            "queue.queue-admin-aliases",
            List.of("queueadmin", "qadmin")
        ),
        migration(
            "The list of servers that should not have the queue system enabled.",
            "queue.no-queue-servers",
            List.of("lobby")
        ),
        migration(
            "If the disconnect reason contains any part of this filter, it will remove the player from the queue immediately.\n"
                + " This is case-sensitive.",
            "queue.banned-reason",
            List.of("banned")
        ),
        migration(
            "Whether the user should be capable of entering multiple queues at once.",
            "queue.allow-multi-queue",
            false
        ),
        migration(
            "How long the queue system should wait before sending each user to a server (in seconds).",
            "queue.send-delay",
            1.0
        ),
        migration(
            "How long the queue should wait before resuming sending players to servers after a backend comes back online.",
            "queue.queue-delay",
            0.0
        ),
        migration(
            "How long the queue system should wait before updating or sending a new action bar, chat message, or title/subtitle.",
            "queue.message-delay",
            1.0
        ),
        migration(
            "How often to ping backend servers to check if they're online.",
            "queue.backend-ping-interval",
            1.0
        ),
        migration(
            "The number of tries a user should be sent to a server before being removed from the queue.",
            "queue.max-send-retries",
            10
        ),
        migration(
            "Whether the player should be removed from their previous queue when switching servers.",
            "queue.remove-player-on-server-switch",
            true
        ),
        migration(
            "The server that players will be moved to when they enter ANY queue.\n"
                + " When a player queues for any server, they will be automatically sent to this server.\n"
                + " While in a queue and on this server, any server switch attempts will be blocked unless\n"
                + " the player has the velocity.queue.server-switch.bypass permission.\n"
                + " This is mutually exclusive with auto-queue-servers. Leave empty to disable.",
            "queue.queue-server",
            ""
        ),
        migration(
            "The server queue(s) a player is automatically entered into on their first proxy join.\n"
                + " Can be configured as a single string or a list of strings.",
            "queue.queue-on-join",
            List.of()
        ),
        migration(
            "Whether the kick message or indicator should be shown when you have failed to queue and join a specific server.",
            "queue.forward-kick-reason",
            true
        ),
        migration(
            "Whether users can enter a queue that is paused.",
            "queue.allow-paused-queue-joining",
            false
        ),
        migration(
            "Whether users should be automatically added back to the queue of their previously connected server.",
            "queue.queue-on-shutdown",
            true
        ),
        migration(
            "If true, players sent via BungeeCord Messaging channels are queued if the server has queueing enabled.\n"
                + " If false, they bypass the queue and are sent directly to the backend.",
            "queue.override-bungee-messaging",
            true
        ),

        // [proxy-addresses]
        migration(
            "Determines which fallback proxy to send players to.\n"
                + " Available options: \"first_found\", \"most_empty\", \"least_empty\", \"none\".",
            "proxy-addresses.dynamic-proxy-filter",
            "most_empty"
        ),

        // [forced-hosts]
        migration(
            "Whether to use the configured forced hosts as fallback (try) servers\n"
                + "if a player joins through a forced host that's configured.",
            "forced-hosts.forced-host-as-fallback",
            true
        ),

        new CtdAutoQueueServersMigration()
    );
  }

  private static ConfigurationMigration migration(String comment, String key, Object defaultValue) {
    if (comment != null) {
      comment = Stream.of(comment.split("\n"))
          .map(line -> !line.startsWith(" ") ? (" " + line) : line)
          .collect(Collectors.joining("\n"));
    }

    return new CtdSimpleMigration(key, defaultValue, comment);
  }
}

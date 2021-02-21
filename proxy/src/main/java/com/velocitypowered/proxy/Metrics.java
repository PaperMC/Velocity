package com.velocitypowered.proxy;

import com.velocitypowered.proxy.config.VelocityConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bstats.MetricsBase;
import org.bstats.charts.CustomChart;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bstats.config.MetricsConfig;
import org.bstats.json.JsonObjectBuilder;

public class Metrics {

  private MetricsBase metricsBase;

  private Metrics(Logger logger, int serviceId, boolean defaultEnabled) {
    File configFile = Paths.get("plugins").resolve("bStats").resolve("config.txt").toFile();
    MetricsConfig config;
    try {
      config = new MetricsConfig(configFile, defaultEnabled);
    } catch (IOException e) {
      logger.error("Failed to create bStats config", e);
      return;
    }

    metricsBase = new MetricsBase(
        "server-implementation",
        config.getServerUUID(),
        serviceId,
        config.isEnabled(),
        this::appendPlatformData,
        jsonObjectBuilder -> { /* NOP */ },
        null,
        () -> true,
        logger::warn,
        logger::info,
        config.isLogErrorsEnabled(),
        config.isLogSentDataEnabled(),
        config.isLogResponseStatusTextEnabled()
    );

    if (!config.didExistBefore()) {
      // Send an info message when the bStats config file gets created for the first time
      logger.info("Velocity and some of its plugins collect metrics"
          + " and send them to bStats (https://bStats.org).");
      logger.info("bStats collects some basic information for plugin"
          + " authors, like how many people use");
      logger.info("their plugin and their total player count."
          + " It's recommended to keep bStats enabled, but");
      logger.info("if you're not comfortable with this, you can opt-out"
          + " by editing the config.txt file in");
      logger.info("the '/plugins/bStats/' folder and setting enabled to false.");
    }
  }

  /**
   * Adds a custom chart.
   *
   * @param chart The chart to add.
   */
  public void addCustomChart(CustomChart chart) {
    metricsBase.addCustomChart(chart);
  }

  private void appendPlatformData(JsonObjectBuilder builder) {
    builder.appendField("osName", System.getProperty("os.name"));
    builder.appendField("osArch", System.getProperty("os.arch"));
    builder.appendField("osVersion", System.getProperty("os.version"));
    builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
  }

  static class VelocityMetrics {

    private static final Logger logger = LogManager.getLogger(Metrics.class);

    static void startMetrics(VelocityServer server, VelocityConfiguration.Metrics metricsConfig) {
      Metrics metrics = new Metrics(logger, 4752, metricsConfig.isEnabled());

      metrics.addCustomChart(
          new SingleLineChart("players", server::getPlayerCount)
      );
      metrics.addCustomChart(
          new SingleLineChart("managed_servers", () -> server.getAllServers().size())
      );
      metrics.addCustomChart(
          new SimplePie("online_mode",
              () -> server.getConfiguration().isOnlineMode() ? "online" : "offline")
      );
      metrics.addCustomChart(new SimplePie("velocity_version",
          () -> server.getVersion().getVersion()));

      metrics.addCustomChart(new DrilldownPie("java_version", () -> {
        Map<String, Map<String, Integer>> map = new HashMap<>();
        String javaVersion = System.getProperty("java.version");
        Map<String, Integer> entry = new HashMap<>();
        entry.put(javaVersion, 1);

        // http://openjdk.java.net/jeps/223
        // Java decided to change their versioning scheme and in doing so modified the
        // java.version system property to return $major[.$minor][.$security][-ea], as opposed to
        // 1.$major.0_$identifier we can handle pre-9 by checking if the "major" is equal to "1",
        // otherwise, 9+
        String majorVersion = javaVersion.split("\\.")[0];
        String release;

        int indexOf = javaVersion.lastIndexOf('.');

        if (majorVersion.equals("1")) {
          release = "Java " + javaVersion.substring(0, indexOf);
        } else {
          // of course, it really wouldn't be all that simple if they didn't add a quirk, now
          // would it valid strings for the major may potentially include values such as -ea to
          // denote a pre release
          Matcher versionMatcher = Pattern.compile("\\d+").matcher(majorVersion);
          if (versionMatcher.find()) {
            majorVersion = versionMatcher.group(0);
          }
          release = "Java " + majorVersion;
        }
        map.put(release, entry);

        return map;
      }));
    }
  }

}
package com.velocitypowered.proxy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import io.netty.handler.codec.http.HttpHeaderNames;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;

/**
 * bStats collects some data for plugin authors.
 * <p/>
 * Check out https://bStats.org/ to learn more about bStats!
 */
public class Metrics {

  // The version of this bStats class
  private static final int B_STATS_METRICS_REVISION = 2;

  // The url to which the data is sent
  private static final String URL = "https://bstats.org/submitData/server-implementation";

  // Should failed requests be logged?
  private static boolean logFailedRequests = false;

  // The logger for the failed requests
  private static Logger logger = LogManager.getLogger(Metrics.class);

  // The name of the server software
  private final String name;

  // The plugin ID for the server software as assigned by bStats.
  private final int pluginId;

  // The uuid of the server
  private final String serverUuid;

  // A list with all custom charts
  private final List<CustomChart> charts = new ArrayList<>();

  private final VelocityServer server;

  /**
   * Class constructor.
   * @param name              The name of the server software.
   * @param pluginId          The plugin ID for the server software as assigned by bStats.
   * @param serverUuid        The uuid of the server.
   * @param logFailedRequests Whether failed requests should be logged or not.
   * @param server            The Velocity server instance.
   */
  private Metrics(String name, int pluginId, String serverUuid, boolean logFailedRequests,
      VelocityServer server) {
    this.name = name;
    this.pluginId = pluginId;
    this.serverUuid = serverUuid;
    Metrics.logFailedRequests = logFailedRequests;
    this.server = server;

    // Start submitting the data
    startSubmitting();
  }

  /**
   * Adds a custom chart.
   *
   * @param chart The chart to add.
   */
  public void addCustomChart(CustomChart chart) {
    if (chart == null) {
      throw new IllegalArgumentException("Chart cannot be null!");
    }
    charts.add(chart);
  }

  /**
   * Starts the Scheduler which submits our data every 30 minutes.
   */
  private void startSubmitting() {
    final Timer timer = new Timer(true);
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        submitData();
      }
    }, 1000, 1000 * 60 * 30);
    // Submit the data every 30 minutes, first time after 5 minutes to give other plugins enough
    // time to start.
    //
    // WARNING: Changing the frequency has no effect but your plugin WILL be blocked/deleted!
    // WARNING: Just don't do it!
  }

  /**
   * Gets the plugin specific data.
   *
   * @return The plugin specific data.
   */
  private JsonObject getPluginData() {
    JsonObject data = new JsonObject();

    data.addProperty("pluginName", name); // Append the name of the server software
    data.addProperty("id", pluginId);
    data.addProperty("metricsRevision", B_STATS_METRICS_REVISION);
    JsonArray customCharts = new JsonArray();
    for (CustomChart customChart : charts) {
      // Add the data of the custom charts
      JsonObject chart = customChart.getRequestJsonObject();
      if (chart == null) { // If the chart is null, we skip it
        continue;
      }
      customCharts.add(chart);
    }
    data.add("customCharts", customCharts);

    return data;
  }

  /**
   * Gets the server specific data.
   *
   * @return The server specific data.
   */
  private JsonObject getServerData() {
    // OS specific data
    String osName = System.getProperty("os.name");
    String osArch = System.getProperty("os.arch");
    String osVersion = System.getProperty("os.version");
    int coreCount = Runtime.getRuntime().availableProcessors();

    JsonObject data = new JsonObject();

    data.addProperty("serverUUID", serverUuid);

    data.addProperty("osName", osName);
    data.addProperty("osArch", osArch);
    data.addProperty("osVersion", osVersion);
    data.addProperty("coreCount", coreCount);

    return data;
  }

  /**
   * Collects the data and sends it afterwards.
   */
  private void submitData() {
    final JsonObject data = getServerData();

    JsonArray pluginData = new JsonArray();
    pluginData.add(getPluginData());
    data.add("plugins", pluginData);

    try {
      // We are still in the Thread of the timer, so nothing get blocked :)
      sendData(data);
    } catch (Exception e) {
      // Something went wrong! :(
      if (logFailedRequests) {
        logger.warn("Could not submit stats of {}", name, e);
      }
    }
  }

  /**
   * Sends the data to the bStats server.
   *
   * @param data The data to send.
   * @throws Exception If the request failed.
   */
  private void sendData(JsonObject data) throws Exception {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null!");
    }

    // Compress the data to save bandwidth
    ListenableFuture<Response> future = server.getAsyncHttpClient()
        .preparePost(URL)
        .addHeader(HttpHeaderNames.CONTENT_ENCODING, "gzip")
        .addHeader(HttpHeaderNames.ACCEPT, "application/json")
        .addHeader(HttpHeaderNames.CONTENT_TYPE, "application/json")
        .setBody(createResponseBody(data))
        .execute();
    future.addListener(() -> {
      if (logFailedRequests) {
        try {
          Response r = future.get();
          if (r.getStatusCode() != 429) {
            logger.error("Got HTTP status code {} when sending metrics to bStats",
                r.getStatusCode());
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
          logger.error("Unable to send metrics to bStats", e);
        }
      }
    }, null);
  }

  private static byte[] createResponseBody(JsonObject object) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try (Writer writer =
        new BufferedWriter(
            new OutputStreamWriter(
                new GZIPOutputStream(os), StandardCharsets.UTF_8
            )
        )
    ) {
      VelocityServer.GSON.toJson(object, writer);
    } catch (IOException e) {
      throw e;
    }
    return os.toByteArray();
  }

  /**
   * Represents a custom chart.
   */
  public abstract static class CustomChart {

    // The id of the chart
    final String chartId;

    /**
     * Class constructor.
     *
     * @param chartId The id of the chart.
     */
    CustomChart(String chartId) {
      if (chartId == null || chartId.isEmpty()) {
        throw new IllegalArgumentException("ChartId cannot be null or empty!");
      }
      this.chartId = chartId;
    }

    private JsonObject getRequestJsonObject() {
      JsonObject chart = new JsonObject();
      chart.addProperty("chartId", chartId);
      try {
        JsonObject data = getChartData();
        if (data == null) {
          // If the data is null we don't send the chart.
          return null;
        }
        chart.add("data", data);
      } catch (Throwable t) {
        if (logFailedRequests) {
          logger.warn("Failed to get data for custom chart with id {}", chartId, t);
        }
        return null;
      }
      return chart;
    }

    protected abstract JsonObject getChartData() throws Exception;

  }

  /**
   * Represents a custom simple pie.
   */
  public static class SimplePie extends CustomChart {

    private final Callable<String> callable;

    /**
     * Class constructor.
     *
     * @param chartId  The id of the chart.
     * @param callable The callable which is used to request the chart data.
     */
    public SimplePie(String chartId, Callable<String> callable) {
      super(chartId);
      this.callable = callable;
    }

    @Override
    protected JsonObject getChartData() throws Exception {
      JsonObject data = new JsonObject();
      String value = callable.call();
      if (value == null || value.isEmpty()) {
        // Null = skip the chart
        return null;
      }
      data.addProperty("value", value);
      return data;
    }
  }

  /**
   * Represents a custom advanced pie.
   */
  public static class AdvancedPie extends CustomChart {

    private final Callable<Map<String, Integer>> callable;

    /**
     * Class constructor.
     *
     * @param chartId  The id of the chart.
     * @param callable The callable which is used to request the chart data.
     */
    public AdvancedPie(String chartId, Callable<Map<String, Integer>> callable) {
      super(chartId);
      this.callable = callable;
    }

    @Override
    protected JsonObject getChartData() throws Exception {
      Map<String, Integer> map = callable.call();
      if (map == null || map.isEmpty()) {
        // Null = skip the chart
        return null;
      }

      JsonObject data = new JsonObject();
      JsonObject values = new JsonObject();
      boolean allSkipped = true;
      for (Map.Entry<String, Integer> entry : map.entrySet()) {
        if (entry.getValue() == 0) {
          continue; // Skip this invalid
        }
        allSkipped = false;
        values.addProperty(entry.getKey(), entry.getValue());
      }
      if (allSkipped) {
        // Null = skip the chart
        return null;
      }
      data.add("values", values);
      return data;
    }
  }

  /**
   * Represents a custom drilldown pie.
   */
  public static class DrilldownPie extends CustomChart {

    private final Callable<Map<String, Map<String, Integer>>> callable;

    /**
     * Class constructor.
     *
     * @param chartId  The id of the chart.
     * @param callable The callable which is used to request the chart data.
     */
    public DrilldownPie(String chartId, Callable<Map<String, Map<String, Integer>>> callable) {
      super(chartId);
      this.callable = callable;
    }

    @Override
    public JsonObject getChartData() throws Exception {
      Map<String, Map<String, Integer>> map = callable.call();
      if (map == null || map.isEmpty()) {
        // Null = skip the chart
        return null;
      }
      boolean reallyAllSkipped = true;
      JsonObject data = new JsonObject();
      JsonObject values = new JsonObject();
      for (Map.Entry<String, Map<String, Integer>> entryValues : map.entrySet()) {
        JsonObject value = new JsonObject();
        boolean allSkipped = true;
        for (Map.Entry<String, Integer> valueEntry : map.get(entryValues.getKey()).entrySet()) {
          value.addProperty(valueEntry.getKey(), valueEntry.getValue());
          allSkipped = false;
        }
        if (!allSkipped) {
          reallyAllSkipped = false;
          values.add(entryValues.getKey(), value);
        }
      }
      if (reallyAllSkipped) {
        // Null = skip the chart
        return null;
      }
      data.add("values", values);
      return data;
    }
  }

  /**
   * Represents a custom single line chart.
   */
  public static class SingleLineChart extends CustomChart {

    private final Callable<Integer> callable;

    /**
     * Class constructor.
     *
     * @param chartId  The id of the chart.
     * @param callable The callable which is used to request the chart data.
     */
    public SingleLineChart(String chartId, Callable<Integer> callable) {
      super(chartId);
      this.callable = callable;
    }

    @Override
    protected JsonObject getChartData() throws Exception {
      JsonObject data = new JsonObject();
      int value = callable.call();
      if (value == 0) {
        // Null = skip the chart
        return null;
      }
      data.addProperty("value", value);
      return data;
    }

  }

  /**
   * Represents a custom multi line chart.
   */
  public static class MultiLineChart extends CustomChart {

    private final Callable<Map<String, Integer>> callable;

    /**
     * Class constructor.
     *
     * @param chartId  The id of the chart.
     * @param callable The callable which is used to request the chart data.
     */
    public MultiLineChart(String chartId, Callable<Map<String, Integer>> callable) {
      super(chartId);
      this.callable = callable;
    }

    @Override
    protected JsonObject getChartData() throws Exception {
      Map<String, Integer> map = callable.call();
      if (map == null || map.isEmpty()) {
        // Null = skip the chart
        return null;
      }
      JsonObject data = new JsonObject();
      JsonObject values = new JsonObject();
      boolean allSkipped = true;
      for (Map.Entry<String, Integer> entry : map.entrySet()) {
        if (entry.getValue() == 0) {
          continue; // Skip this invalid
        }
        allSkipped = false;
        values.addProperty(entry.getKey(), entry.getValue());
      }
      if (allSkipped) {
        // Null = skip the chart
        return null;
      }
      data.add("values", values);
      return data;
    }

  }

  /**
   * Represents a custom simple bar chart.
   */
  public static class SimpleBarChart extends CustomChart {

    private final Callable<Map<String, Integer>> callable;

    /**
     * Class constructor.
     *
     * @param chartId  The id of the chart.
     * @param callable The callable which is used to request the chart data.
     */
    public SimpleBarChart(String chartId, Callable<Map<String, Integer>> callable) {
      super(chartId);
      this.callable = callable;
    }

    @Override
    protected JsonObject getChartData() throws Exception {
      JsonObject data = new JsonObject();
      JsonObject values = new JsonObject();
      Map<String, Integer> map = callable.call();
      if (map == null || map.isEmpty()) {
        // Null = skip the chart
        return null;
      }
      for (Map.Entry<String, Integer> entry : map.entrySet()) {
        JsonArray categoryValues = new JsonArray();
        categoryValues.add(entry.getValue());
        values.add(entry.getKey(), categoryValues);
      }
      data.add("values", values);
      return data;
    }

  }

  /**
   * Represents a custom advanced bar chart.
   */
  public static class AdvancedBarChart extends CustomChart {

    private final Callable<Map<String, int[]>> callable;

    /**
     * Class constructor.
     *
     * @param chartId  The id of the chart.
     * @param callable The callable which is used to request the chart data.
     */
    public AdvancedBarChart(String chartId, Callable<Map<String, int[]>> callable) {
      super(chartId);
      this.callable = callable;
    }

    @Override
    protected JsonObject getChartData() throws Exception {
      JsonObject values = new JsonObject();
      Map<String, int[]> map = callable.call();
      if (map == null || map.isEmpty()) {
        // Null = skip the chart
        return null;
      }
      boolean allSkipped = true;
      for (Map.Entry<String, int[]> entry : map.entrySet()) {
        if (entry.getValue().length == 0) {
          continue; // Skip this invalid
        }
        allSkipped = false;
        JsonArray categoryValues = new JsonArray();
        for (int categoryValue : entry.getValue()) {
          categoryValues.add(categoryValue);
        }
        values.add(entry.getKey(), categoryValues);
      }
      if (allSkipped) {
        // Null = skip the chart
        return null;
      }
      JsonObject data = new JsonObject();
      data.add("values", values);
      return data;
    }

  }

  static class VelocityMetrics {
    static void startMetrics(VelocityServer server, VelocityConfiguration.Metrics metricsConfig) {
      if (!metricsConfig.isFromConfig()) {
        // Log an informational message.
        logger.info("Velocity collects metrics and sends them to bStats (https://bstats.org).");
        logger.info("bStats collects some basic information like how many people use Velocity and");
        logger.info("their player count. This has no impact on performance and this data does not");
        logger.info("identify your server in any way. However, you may opt-out by editing your");
        logger.info("velocity.toml and setting enabled = false in the [metrics] section.");
      }

      // Load the data
      String serverUuid = metricsConfig.getId();
      boolean logFailedRequests = metricsConfig.isLogFailure();
      // Only start Metrics, if it's enabled in the config
      if (metricsConfig.isEnabled()) {
        Metrics metrics = new Metrics("Velocity", 4752, serverUuid, logFailedRequests, server);

        metrics.addCustomChart(
            new Metrics.SingleLineChart("players", server::getPlayerCount)
        );
        metrics.addCustomChart(
            new Metrics.SingleLineChart("managed_servers", () -> server.getAllServers().size())
        );
        metrics.addCustomChart(
            new Metrics.SimplePie("online_mode",
                () -> server.getConfiguration().isOnlineMode() ? "online" : "offline")
        );
        metrics.addCustomChart(new Metrics.SimplePie("velocity_version",
            () -> server.getVersion().getVersion()));

        metrics.addCustomChart(new Metrics.DrilldownPie("java_version", () -> {
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
}

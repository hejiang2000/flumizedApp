package cn.gbase;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.flume.Channel;
import org.apache.flume.Event;
import org.apache.flume.Transaction;
import org.apache.flume.node.Application;
import org.apache.flume.node.MaterializedConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * App
 *
 */
public class App implements Runnable {
  static final Logger logger = LoggerFactory.getLogger(App.class);

  String agentName;
  int channelsEmptyCheckCount = 10;
  int channelsEmptyCheckDelaySec = 1;

  public App(String[] args) {
    this.agentName = "agent";
  }

  public void run() {
    MaterializedConfiguration configuration = buildConfiguration(agentName, buildProperties());
    startApplication(configuration);

    if (waitForChannelsEmpty(configuration, channelsEmptyCheckCount, channelsEmptyCheckDelaySec)) {
      System.exit(0);
    }
  }

  Map<String, String> buildProperties() {
    Map<String, String> properties = new HashMap<String, String>();

    String sourceName = "source";
    String channelName = "channel";
    String sinkName = "sink";

    properties.put(agentName + ".sources", sourceName);
    properties.put(agentName + ".channels", channelName);
    properties.put(agentName + ".sinks", sinkName);

    properties.put(agentName + ".sources." + sourceName + ".channels", channelName);
    properties.put(agentName + ".sinks." + sinkName + ".channel", channelName);

    properties.put(agentName + ".sources." + sourceName + ".type",
        "org.apache.flume.source.StressSource");
    properties.put(agentName + ".sources." + sourceName + ".size", "500");
    properties.put(agentName + ".sources." + sourceName + ".maxTotalEvents", "10");

    properties.put(agentName + ".channels." + channelName + ".type", "memory");
    properties.put(agentName + ".channels." + channelName + ".capacity", "200");

    properties.put(agentName + ".sinks." + sinkName + ".type", "logger");

    return properties;
  }

  MaterializedConfiguration buildConfiguration(String agentName, Map<String, String> properties) {
    return new MemoryConfigurationProvider(agentName, properties).getConfiguration();
  }

  Application startApplication(MaterializedConfiguration configuration) {
    final Application app = new Application();
    app.handleConfigurationEvent(configuration);

    Runtime.getRuntime().addShutdownHook(new Thread("agent-shutdown-hook") {
      @Override
      public void run() {
        app.stop();
      }
    });

    logger.info("Application started.");
    return app;
  }

  boolean areChannelsEmpty(MaterializedConfiguration configuration) {
    for (Entry<String, Channel> entry : configuration.getChannels().entrySet()) {
      Channel channel = entry.getValue();
      Transaction tx = channel.getTransaction();
      tx.begin();
      Event event = channel.take();
      tx.rollback();
      tx.close();

      if (event != null) {
        return false;
      }
    }

    return true;
  }

  boolean waitForChannelsEmpty(MaterializedConfiguration configuration, int channelsEmptyCheckCount,
      int channelsEmptyCheckDelaySec) {
    int emptyCheckCounter = 0;

    while (true) {
      if (!areChannelsEmpty(configuration)) {
        emptyCheckCounter = 0;
      } else if (++emptyCheckCounter > channelsEmptyCheckCount) {
        logger.info("All channels are empty for a while, the application is about to quit...");
        return true;
      }

      try {
        Thread.sleep(channelsEmptyCheckDelaySec * 1000L);
      } catch (InterruptedException e) {
        logger.info("Interrupted", e);
        return false;
      }
    }
  }

  public static void main(String[] args) {
    new App(args).run();
  }

}

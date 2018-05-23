package cn.gbase;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.flume.Channel;
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
	private static final Logger logger = LoggerFactory.getLogger(App.class);
	
	private String agentName;
	
	public App(String[] args) {
		this.agentName = "agent";
	}
	
	public void run() {
		Map<String, String> properties = getProperties();

		MemoryConfigurationProvider provider = new MemoryConfigurationProvider(agentName, properties);
		MaterializedConfiguration configuration = provider.getConfiguration();
		
		final Application app = new Application();
		app.handleConfigurationEvent(configuration);

		logger.info("Application started.");
		
		Runtime.getRuntime().addShutdownHook(new Thread("agent-shutdown-hook") {
			@Override
			public void run() {
				app.stop();
			}
		});
		
		waitForExit(configuration);
	}

	public void waitForExit(MaterializedConfiguration configuration) {
		int doneCounter = 0;
		
		while (true) {
			boolean done = true;

			for (Entry<String, Channel> entry : configuration.getChannels().entrySet()) {
				Channel channel = entry.getValue();
				Transaction tx = channel.getTransaction();
				tx.begin();
				if (channel.take() != null) {
					done = false;
					tx.rollback();
					tx.close();
					break;
				}
				tx.rollback();
				tx.close();
			}
			
			if (done && ++doneCounter > 10) {
				logger.info("All channels are empty for a while, the application is about to quit...");
				System.exit(0);
			}

			if (!done) {
				doneCounter = 0;
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.info("Interrupted", e);
				break;
			}
		}
	}
	
	public Map<String, String> getProperties() {
		Map<String, String> properties = new HashMap<String, String>();

		String sourceName  = "source";
		String channelName = "channel";
		String sinkName    = "sink";
		
		properties.put(agentName + ".sources" , sourceName);
		properties.put(agentName + ".channels", channelName);
		properties.put(agentName + ".sinks"   , sinkName);

		properties.put(agentName + ".sources."  + sourceName  + ".channels", channelName);
		properties.put(agentName + ".sinks."    + sinkName    + ".channel" , channelName);

		properties.put(agentName + ".sources."  + sourceName  + ".type", "org.apache.flume.source.StressSource");
		properties.put(agentName + ".sources."  + sourceName  + ".size", "500");
		properties.put(agentName + ".sources."  + sourceName  + ".maxTotalEvents", "10");
		
		properties.put(agentName + ".channels." + channelName + ".type", "memory");
		properties.put(agentName + ".channels." + channelName + ".capacity", "200");
		
		properties.put(agentName + ".sinks."    + sinkName    + ".type", "logger");

		return properties;
	}

	public static void main(String[] args) {
		new App(args).run();
	}

}

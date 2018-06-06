package cn.gbase;

import java.util.Map;

import org.apache.flume.conf.FlumeConfiguration;
import org.apache.flume.node.AbstractConfigurationProvider;

public class MemoryConfigurationProvider extends AbstractConfigurationProvider {
  private final Map<String, String> properties;

  MemoryConfigurationProvider(String name, Map<String, String> properties) {
    super(name);
    this.properties = properties;
  }

  @Override
  protected FlumeConfiguration getFlumeConfiguration() {
    return new FlumeConfiguration(properties);
  }

}

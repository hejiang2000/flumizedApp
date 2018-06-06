package cn.gbase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.flume.node.Application;
import org.apache.flume.node.MaterializedConfiguration;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {
  /**
   * Rigorous Test :-)
   */
  @Test
  public void testBuildProperties() {
    App app = new App(null);
    Map<String, String> props = app.buildProperties();
    
    assertNotNull("Fail to build properties", props);
  }
  
  @Test
  public void testBuildConfiguration() {
    App app = new App(null);
    Map<String, String> props = app.buildProperties();
    MaterializedConfiguration conf = app.buildConfiguration(app.agentName, props);
    
    assertNotNull("Fail to build configuration", conf);
  }
  
  @Test
  public void testStartApplication() {
    App app = new App(null);
    Map<String, String> props = app.buildProperties();
    props.put("agent.sources.source.maxTotalEvents", "0");
    
    MaterializedConfiguration conf = app.buildConfiguration(app.agentName, props);
    Application application = app.startApplication(conf);
    
    assertNotNull("Fail to start application", application);
  }
  
  @Test
  public void testAreChannelsEmpty() {
    App app = new App(null);
    Map<String, String> props = app.buildProperties();
    MaterializedConfiguration conf = app.buildConfiguration(app.agentName, props);
    boolean empty = app.areChannelsEmpty(conf);
    
    assertTrue("Fail to check channels empty", empty);
  }
  
  @Test
  public void testWaitForChannelsEmpty() {
    App app = new App(null);
    Map<String, String> props = app.buildProperties();
    MaterializedConfiguration conf = app.buildConfiguration(app.agentName, props);
    app.startApplication(conf);
    
    boolean empty = app.waitForChannelsEmpty(conf, 3, 1);
    
    assertTrue("Fail to wait channels empty", empty);
  }
}

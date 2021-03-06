package com.gemstone.gemfire.distributed;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.gemstone.gemfire.distributed.AbstractLauncher.Status;
import com.gemstone.gemfire.distributed.LocatorLauncher.Builder;
import com.gemstone.gemfire.distributed.LocatorLauncher.LocatorState;
import com.gemstone.gemfire.distributed.internal.DistributionManager;
import com.gemstone.gemfire.distributed.internal.InternalDistributedSystem;
import com.gemstone.gemfire.distributed.internal.InternalLocator;
import com.gemstone.gemfire.distributed.internal.membership.InternalDistributedMember;
import com.gemstone.gemfire.internal.AvailablePortHelper;
import com.gemstone.gemfire.internal.SocketCreator;
import com.gemstone.gemfire.internal.util.StopWatch;

import dunit.DistributedTestCase;
import dunit.Host;
import dunit.SerializableCallable;
import dunit.SerializableRunnable;

/**
 * Extracted from LocatorLauncherLocalJUnitTest.
 * 
 * @author Kirk Lund
 * @since 8.0
 */
public class HostedLocatorsDUnitTest extends DistributedTestCase {

  protected static final int TIMEOUT_MILLISECONDS = 5 * 60 * 1000; // 5 minutes

  protected transient volatile int locatorPort;
  protected transient volatile LocatorLauncher launcher;
  
  public void setUp() throws Exception {
    disconnectAllFromDS();
  }
  
  public void tearDown2() throws Exception {
    disconnectAllFromDS();
  }
  
  public HostedLocatorsDUnitTest(String name) {
    super(name);
  }

  public void testGetAllHostedLocators() throws Exception {
    final InternalDistributedSystem system = getSystem();
    final String dunitLocator = system.getConfig().getLocators();
    assertNotNull(dunitLocator);
    assertFalse(dunitLocator.isEmpty());

    final int[] ports = AvailablePortHelper.getRandomAvailableTCPPorts(4);
    
    final String uniqueName = getUniqueName();
    for (int i = 0 ; i < 4; i++) {
      final int whichvm = i;
      Host.getHost(0).getVM(whichvm).invoke(new SerializableCallable() {
        @Override
        public Object call() throws Exception {
          try {
            System.setProperty("gemfire.locators", dunitLocator);
            System.setProperty("gemfire.mcast-port", "0");
            
            final String name = uniqueName + "-" + whichvm;
            final File subdir = new File(name);
            subdir.mkdir();
            assertTrue(subdir.exists() && subdir.isDirectory());
            
            final Builder builder = new Builder()
                .setMemberName(name)
                .setPort(ports[whichvm])
                .setRedirectOutput(true)
                .setWorkingDirectory(name);
    
            launcher = builder.build();
            assertEquals(Status.ONLINE, launcher.start().getStatus());
            waitForLocatorToStart(launcher, TIMEOUT_MILLISECONDS, 10, true);
            return null;
          } finally {
            System.clearProperty("gemfire.locators");
            System.clearProperty("gemfire.mcast-port");
          }
        }
      });
    }
    
    final String host = SocketCreator.getLocalHost().getHostAddress();
    
    final Set<String> locators = new HashSet<String>();
    locators.add(host + "[" + dunitLocator.substring(dunitLocator.indexOf("[")+1, dunitLocator.indexOf("]")) + "]");
    for (int port : ports) {
      locators.add(host +"[" + port + "]");
    }

    // validation within non-locator
    final DistributionManager dm = (DistributionManager)system.getDistributionManager();
    
    final Set<InternalDistributedMember> locatorIds = dm.getLocatorDistributionManagerIds();
    assertEquals(5, locatorIds.size());
    
    final Map<InternalDistributedMember, Collection<String>> hostedLocators = dm.getAllHostedLocators();
    assertTrue(!hostedLocators.isEmpty());
    assertEquals(5, hostedLocators.size());
    
    for (InternalDistributedMember member : hostedLocators.keySet()) {
      assertEquals(1, hostedLocators.get(member).size());
      final String hostedLocator = hostedLocators.get(member).iterator().next();
      assertTrue(locators + " does not contain " + hostedLocator, locators.contains(hostedLocator));
    }

    // validate fix for #46324
    for (int whichvm = 0 ; whichvm < 4; whichvm++) {
      Host.getHost(0).getVM(whichvm).invoke(new SerializableRunnable() {
        @Override
        public void run() {
          final DistributionManager dm = (DistributionManager)InternalDistributedSystem.getAnyInstance().getDistributionManager();
          final InternalDistributedMember self = dm.getDistributionManagerId();
          
          final Set<InternalDistributedMember> locatorIds = dm.getLocatorDistributionManagerIds();
          assertTrue(locatorIds.contains(self));
          
          final Map<InternalDistributedMember, Collection<String>> hostedLocators = dm.getAllHostedLocators();
          assertTrue("hit bug #46324: " + hostedLocators + " is missing " + InternalLocator.getLocatorStrings() + " for " + self, hostedLocators.containsKey(self));
        }
      });
    }
    
    // validation with locators
    for (int whichvm = 0 ; whichvm < 4; whichvm++) {
      Host.getHost(0).getVM(whichvm).invoke(new SerializableRunnable() {
        @Override
        public void run() {
          final DistributionManager dm = (DistributionManager)InternalDistributedSystem.getAnyInstance().getDistributionManager();
          
          final Set<InternalDistributedMember> locatorIds = dm.getLocatorDistributionManagerIds();
          assertEquals(5, locatorIds.size());
          
          final Map<InternalDistributedMember, Collection<String>> hostedLocators = dm.getAllHostedLocators();
          assertTrue(!hostedLocators.isEmpty());
          assertEquals(5, hostedLocators.size());
          
          for (InternalDistributedMember member : hostedLocators.keySet()) {
            assertEquals(1, hostedLocators.get(member).size());
            final String hostedLocator = hostedLocators.get(member).iterator().next();
            assertTrue(locators + " does not contain " + hostedLocator, locators.contains(hostedLocator));
          }
        }
      });
    }
  }

  protected void waitForLocatorToStart(final LocatorLauncher launcher, int timeout, int interval, boolean throwOnTimeout) throws Exception {
    assertEventuallyTrue("waiting for process to start: " + launcher.status(), new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        try {
          final LocatorState LocatorState = launcher.status();
          return (LocatorState != null && Status.ONLINE.equals(LocatorState.getStatus()));
        }
        catch (RuntimeException e) {
          return false;
        }
      }
    }, timeout, interval);
  }

  protected static void assertEventuallyTrue(final String message, final Callable<Boolean> callable, final int timeout, final int interval) throws Exception {
    boolean done = false;
    for (StopWatch time = new StopWatch(true); !done && time.elapsedTimeMillis() < timeout; done = (callable.call())) {
      Thread.sleep(interval);
    }
    assertTrue(message, done);
  }
}

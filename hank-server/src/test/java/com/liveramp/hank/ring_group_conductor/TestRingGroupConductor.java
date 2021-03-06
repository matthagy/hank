/**
 *  Copyright 2011 LiveRamp
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.liveramp.hank.ring_group_conductor;

import com.liveramp.hank.config.RingGroupConductorConfigurator;
import com.liveramp.hank.coordinator.*;
import com.liveramp.hank.coordinator.mock.MockCoordinator;
import com.liveramp.hank.coordinator.mock.MockDomain;
import com.liveramp.hank.coordinator.mock.MockDomainGroup;
import com.liveramp.hank.coordinator.mock.MockDomainVersion;
import com.liveramp.hank.test.coordinator.*;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class TestRingGroupConductor extends TestCase {
  public class MockRingGroupUpdateTransitionFunction implements RingGroupUpdateTransitionFunction {
    public RingGroup calledWithRingGroup;

    @Override
    public void manageTransitions(RingGroup ringGroup) {
      calledWithRingGroup = ringGroup;
    }
  }

  public void testTriggersUpdates() throws Exception {
    final MockDomain domain = new MockDomain("domain") {
      @Override
      public DomainVersion getVersion(int version) {
        return new MockDomainVersion(0, 0l);
      }
    };

    final MockDomainGroup domainGroup = new MockDomainGroup("myDomainGroup") {
      @Override
      public Set<DomainGroupDomainVersion> getDomainVersions() {
        SortedSet<DomainGroupDomainVersion> result = new TreeSet<DomainGroupDomainVersion>();
        result.add(new DomainGroupDomainVersion(domain, 1));
        return result;
      }
    };

    final MockHostDomainPartition mockHostDomainPartition = new MockHostDomainPartition(0, 0);

    final MockHost mockHost = new MockHost(new PartitionServerAddress("locahost", 12345)) {
      @Override
      public Set<HostDomain> getAssignedDomains() throws IOException {
        return Collections.singleton((HostDomain) new MockHostDomain(domain) {
          @Override
          public HostDomainPartition addPartition(int partitionNumber) {
            return null;
          }

          @Override
          public Set<HostDomainPartition> getPartitions() {
            return Collections.singleton((HostDomainPartition) mockHostDomainPartition);
          }
        });
      }
    };

    final MockRing mockRing = new MockRing(null, null, 1) {
      @Override
      public Set<Host> getHosts() {
        return Collections.singleton((Host) mockHost);
      }
    };

    final MockRingGroup mockRingGroup = new MockRingGroup(null, "myRingGroup", Collections.<Ring>emptySet()) {
      @Override
      public DomainGroup getDomainGroup() {
        return domainGroup;
      }

      @Override
      public Set<Ring> getRings() {
        return Collections.singleton((Ring) mockRing);
      }

      @Override
      public RingGroupConductorMode getRingGroupConductorMode() {
        return RingGroupConductorMode.ACTIVE;
      }
    };

    RingGroupConductorConfigurator mockConfig = new RingGroupConductorConfigurator() {
      @Override
      public long getSleepInterval() {
        return 100;
      }

      @Override
      public RingGroupConductorMode getInitialMode() {
        return RingGroupConductorMode.ACTIVE;
      }

      @Override
      public int getMinRingFullyServingObservations() {
        return 0;
      }

      @Override
      public String getRingGroupName() {
        return "myRingGroup";
      }

      @Override
      public Coordinator createCoordinator() {
        return new MockCoordinator() {
          @Override
          public RingGroup getRingGroup(String ringGroupName) {
            return mockRingGroup;
          }
        };
      }
    };
    MockRingGroupUpdateTransitionFunction mockTransFunc = new MockRingGroupUpdateTransitionFunction();
    RingGroupConductor daemon = new RingGroupConductor(mockConfig, mockTransFunc);
    daemon.processUpdates(mockRingGroup);

    assertNotNull(mockTransFunc.calledWithRingGroup);
  }

  public void testKeepsExistingUpdatesGoing() throws Exception {
    final MockDomainGroup domainGroup = new MockDomainGroup("myDomainGroup") {
      @Override
      public Set<DomainGroupDomainVersion> getDomainVersions() {
        SortedSet<DomainGroupDomainVersion> result = new TreeSet<DomainGroupDomainVersion>();
        return result;
      }
    };

    final MockRingGroup mockRingGroup = new MockRingGroup(null, "myRingGroup", Collections.<Ring>emptySet()) {
      @Override
      public DomainGroup getDomainGroup() {
        return domainGroup;
      }
    };

    RingGroupConductorConfigurator mockConfig = new RingGroupConductorConfigurator() {
      @Override
      public long getSleepInterval() {
        return 100;
      }

      @Override
      public RingGroupConductorMode getInitialMode() {
        return RingGroupConductorMode.ACTIVE;
      }

      @Override
      public int getMinRingFullyServingObservations() {
        return 0;
      }

      @Override
      public String getRingGroupName() {
        return "myRingGroup";
      }

      @Override
      public Coordinator createCoordinator() {
        return new MockCoordinator() {
          @Override
          public RingGroup getRingGroup(String ringGroupName) {
            return mockRingGroup;
          }
        };
      }
    };
    MockRingGroupUpdateTransitionFunction mockTransFunc = new MockRingGroupUpdateTransitionFunction();
    RingGroupConductor daemon = new RingGroupConductor(mockConfig, mockTransFunc);
    daemon.processUpdates(mockRingGroup);

    assertEquals(mockRingGroup, mockTransFunc.calledWithRingGroup);
  }
}

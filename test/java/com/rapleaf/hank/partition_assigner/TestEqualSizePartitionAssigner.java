package com.rapleaf.hank.partition_assigner;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.rapleaf.hank.BaseTestCase;
import com.rapleaf.hank.coordinator.Domain;
import com.rapleaf.hank.coordinator.DomainGroup;
import com.rapleaf.hank.coordinator.DomainGroupVersion;
import com.rapleaf.hank.coordinator.DomainVersion;
import com.rapleaf.hank.coordinator.Host;
import com.rapleaf.hank.coordinator.HostDomain;
import com.rapleaf.hank.coordinator.HostDomainPartition;
import com.rapleaf.hank.coordinator.MockDomainGroup;
import com.rapleaf.hank.coordinator.MockDomainGroupVersion;
import com.rapleaf.hank.coordinator.MockHost;
import com.rapleaf.hank.coordinator.MockHostDomain;
import com.rapleaf.hank.coordinator.MockHostDomainPartition;
import com.rapleaf.hank.coordinator.MockRing;
import com.rapleaf.hank.coordinator.MockRingGroup;
import com.rapleaf.hank.coordinator.PartitionServerAddress;
import com.rapleaf.hank.coordinator.Ring;
import com.rapleaf.hank.coordinator.RingGroup;
import com.rapleaf.hank.coordinator.RingState;
import com.rapleaf.hank.coordinator.VersionOrAction;
import com.rapleaf.hank.coordinator.mock.MockDomain;
import com.rapleaf.hank.coordinator.mock.MockDomainVersion;

public class TestEqualSizePartitionAssigner extends BaseTestCase {
  private static final DomainVersion version = new MockDomainVersion(0, new Long(0));
  private static final Domain domain = new MockDomain("TestDomain", 20, null, null, null, version);

  private static HashSet<Integer> unassigned = new HashSet<Integer>();
  private static HashSet<Integer> partsOn1 = new HashSet<Integer>();
  private static HashSet<Integer> partsOn2 = new HashSet<Integer>();
  private static HashSet<Integer> partsOn3 = new HashSet<Integer>();

  static {
    for (int i = 0; i < 10; i++) {
      unassigned.add(i);
    }

    for (int i = 10; i < 17; i++) {
      partsOn1.add(i);
    }

    for (int i = 17; i < 20; i++) {
      partsOn2.add(i);
    }
  }

  private static final DomainGroup domainGroup = new MockDomainGroup("TestDomainGroup") {
    @Override
    public Domain getDomain(int domainId) {
      return domain;
    }

    @Override
    public DomainGroupVersion getLatestVersion() {
      return dgv;
    }
  };

  private static final DomainGroupVersion dgv = new MockDomainGroupVersion(null, domainGroup, 0);

  private static final PartitionServerAddress pda1 = new PartitionServerAddress("host1", 12345);
  private static final PartitionServerAddress pda2 = new PartitionServerAddress("host2", 12345);
  private static final PartitionServerAddress pda3 = new PartitionServerAddress("host3", 12345);

  private static final HashSet<PartitionServerAddress> addresses = new HashSet<PartitionServerAddress>();
  static {
    addresses.add(pda1);
    addresses.add(pda2);
    addresses.add(pda3);
  }

  private final static Host host1 = new MockHost(pda1) {
    @Override
    public HostDomain getHostDomain(Domain domain) {
      return hostDomain1;
    }
  };

  private final static Host host2 = new MockHost(pda2) {
    @Override
    public HostDomain getHostDomain(Domain domain) {
      return hostDomain2;
    }
  };

  private final static Host host3 = new MockHost(pda3) {
    @Override
    public HostDomain getHostDomain(Domain domain) {
      return hostDomain3;
    }
  };

  private final static HashSet<Host> hosts = new HashSet<Host>();

  static {
    hosts.add(host1);
    hosts.add(host2);
    hosts.add(host3);
  }

  static {
    try {
      for (Host host : hosts) {
        host.addDomain(domain);
      }
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  private final static HostDomain hostDomain1 = new MockHostDomain(0, 0, 0, 1) {
    @Override
    public Set<HostDomainPartition> getPartitions() {
      Set<HostDomainPartition> partitions = new HashSet<HostDomainPartition>();

      for (Integer partNum : partsOn1) {
        partitions.add(new MockHostDomainPartition(partNum, 0, 1));
      }

      return partitions;
    }

    @Override
    public HostDomainPartition addPartition(int partNum, int initialVersion) {
      partsOn1.add(partNum);
      return null;
    }
  };

  private final static HostDomain hostDomain2 = new MockHostDomain(0, 0, 0, 1) {
    @Override
    public Set<HostDomainPartition> getPartitions() {
      Set<HostDomainPartition> partitions = new HashSet<HostDomainPartition>();

      for (Integer partNum : partsOn2) {
        partitions.add(new MockHostDomainPartition(partNum, 0, 1));
      }

      return partitions;
    }

    @Override
    public HostDomainPartition addPartition(int partNum, int initialVersion) {
      partsOn2.add(partNum);
      return null;
    }
  };

  private final static HostDomain hostDomain3 = new MockHostDomain(0, 0, 0, 1) {
    @Override
    public Set<HostDomainPartition> getPartitions() {
      Set<HostDomainPartition> partitions = new HashSet<HostDomainPartition>();

      for (Integer partNum : partsOn3) {
        partitions.add(new MockHostDomainPartition(partNum, 0, 1));
      }

      return partitions;
    }

    @Override
    public HostDomainPartition addPartition(int partNum, int initialVersion) {
      partsOn3.add(partNum);
      return null;
    }
  };

  private static final HashSet<Ring> rings = new HashSet<Ring>();
  private static final RingGroup ringGroup = new MockRingGroup(domainGroup, "TestRingGroup", rings) {
    @Override
    public Ring getRing(int ringNumber) {
      return ring;
    }
  };

  private static final Ring ring = new MockRing(addresses, ringGroup, 0, RingState.UP) {
    @Override
    public Set<Host> getHosts() {
      return hosts;
    }

    @Override
    public Set<Integer> getUnassignedPartitions(Domain domain) {
      return unassigned;
    }
  };

  static {
    rings.add(ring);
  }

  static {
    try {
      domainGroup.createNewVersion(new HashMap<Domain, VersionOrAction>() {{put(domain, new VersionOrAction(0));}});
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  public void testPartitioner() throws Exception {
    // Initially unbalanced
    assertEquals(false, assignmentsBalanced(ring, domain));

    // Balance
    PartitionAssigner partitionAssigner = new EqualSizePartitionAssigner();

    partitionAssigner.assign(ringGroup, 0, domain);

    // Now balanced
    assertEquals(true, assignmentsBalanced(ring, domain));

    // No dups
    assertEquals(true, noDuplicates(ring, domain));
  }

  private boolean assignmentsBalanced(Ring ring, Domain domain) throws IOException {
    HostDomain maxHostDomain = getMaxHostDomain(ring, domain);
    HostDomain minHostDomain = getMinHostDomain(ring, domain);
    int maxDistance = Math.abs(maxHostDomain.getPartitions().size()
        - minHostDomain.getPartitions().size());
    return maxDistance <= 1;
  }

  private HostDomain getMinHostDomain(Ring ring, Domain domain) throws IOException {
    HostDomain minHostDomain = null;
    int minNumPartitions = Integer.MAX_VALUE;
    for (Host host : ring.getHosts()) {
      HostDomain hostDomain = host.getHostDomain(domain);
      int numPartitions = hostDomain.getPartitions().size();
      if (numPartitions < minNumPartitions) {
        minHostDomain = hostDomain;
        minNumPartitions = numPartitions;
      }
    }

    return minHostDomain;
  }

  private HostDomain getMaxHostDomain(Ring ring, Domain domain) throws IOException {
    HostDomain maxHostDomain = null;
    int maxNumPartitions = Integer.MIN_VALUE;
    for (Host host : ring.getHosts()) {
      HostDomain hostDomain = host.getHostDomain(domain);
      int numPartitions = hostDomain.getPartitions().size();
      if (numPartitions > maxNumPartitions) {
        maxHostDomain = hostDomain;
        maxNumPartitions = numPartitions;
      }
    }

    return maxHostDomain;
  }

  private boolean noDuplicates(Ring ring, Domain domainId) throws IOException {
    HashSet<Integer> partNums = new HashSet<Integer>();

    for (Host host : ring.getHosts()) {
      HostDomain hostDomain = host.getHostDomain(domainId);
      for (HostDomainPartition hdp : hostDomain.getPartitions()) {
        int partNum = hdp.getPartNum();
        if (partNums.contains(partNum))
          return false;
        partNums.add(partNum);
      }
    }

    return true;
  }
}

package test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import strategy.nodes.p2p.NodeConnectionStrategy;
import strategy.nodes.p2p.NodeConnectionStrategyImpl;

class TestNodeConectionStrategy {
	private NodeConnectionStrategy strategy;

	@Test
	void test() {
		strategy = new NodeConnectionStrategyImpl(15);
		int id0 = 0;
		int id1 = 1;
		int id2= 2;
		Optional<Set<Integer>> result;
		
		result = strategy.findNewConnections(id0, 0, 0, Optional.empty());
		assertTrue(result.isEmpty());
		
		result = strategy.findNewConnections(id1, 10, 10, Optional.empty());
		assertTrue(result.isPresent());
		assertTrue(result.get().contains(id0));
		
		result = strategy.findNewConnections(id2, -2, -2, Optional.empty());
		assertTrue(result.isPresent());
		assertEquals(1, result.get().size());
		assertTrue(result.get().contains(id0));
		
		var set = new HashSet<Integer>();
		set.add(id1);
		set.add(id2);
		result = strategy.findNewConnections(id0, 0, 1, Optional.of(set));
		assertTrue(result.isEmpty());
		
		result = strategy.findNewConnections(id0, 100, 100, Optional.of(new HashSet<Integer>()));
		assertTrue(result.isEmpty());
		
		result = strategy.findNewConnections(id0, 1, 1, Optional.empty());
		assertTrue(result.isPresent());
		assertEquals(2, result.get().size());
		assertTrue(result.get().contains(id1));
		assertTrue(result.get().contains(id2));
		
		result = strategy.findNewConnections(id0, 0, 1, Optional.empty());
		assertTrue(result.isEmpty());
	}

}

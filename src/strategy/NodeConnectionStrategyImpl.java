package strategy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class NodeConnectionStrategyImpl implements NodeConnectionStrategy{
	private final static double epsilon = 0.000001d;
	private final double radius;
	private final Map<Integer, NodePosition> positionMap;
	
	public NodeConnectionStrategyImpl(double radius) {
		this.radius = radius;
		positionMap = new HashMap<>();
	}

	@Override
	public Optional<Set<Integer>> findNewConnections(int id, int x, int y, Optional<Set<Integer>> connectedNodes) {
		
		if(!positionMap.containsKey(id)) { // new node
			addNode(id, x, y);
		}else if(positionMap.get(id).getDistance(x, y) < epsilon) { // node does not change its coordinates
			return Optional.empty();
		}
		
		// node's coordinates are changed
		NodePosition node = positionMap.get(id);
		node.setX(x);
		node.setY(y);
		
		
		Set<Integer> nodesInRadius = positionMap.entrySet().stream()
			.map(e -> e.getValue())
			.filter(v -> v.getDistance(x, y) <= radius)
			.filter(v -> v.getId() != id)
			.map(m -> m.getId())
			.collect(Collectors.toSet());
		if(connectedNodes.isPresent()) {
			node.updateConnectedNodes(connectedNodes.get());
		}
		
		return node.getNotConnectedNodesId(nodesInRadius);
		
	}


	@Override
	public void removeNode(int id) {
		positionMap.remove(id);
		
	}
	
	private void addNode(int id, int x, int y) {
		positionMap.put(id, new NodePosition(id, x, y));
		
	}
	
	private class NodePosition{
		private final int id;
		private int x;
		private int y;
		private Set<Integer> connectedNoded;
		
		public NodePosition(int id, int x, int y) {
			this.id = id;
			this.x = x;
			this.y = y;
			connectedNoded = new HashSet<>();
		}
		
		public int getId() {return this.id;}
		public void setX(int x) { this.x = x;}
		public void setY(int y) { this.y = y;}
		
		/**
		 * @param x coordinate
		 * @param y coordinate
		 * @return distance between node's coordinates and input coordinates
		 */
		public double getDistance(int x, int y) {
			return Math.hypot((double)(this.x - x), (double)(this.y-y));
		}
		
		public void updateConnectedNodes(Set<Integer> newConnectedNodesSet) {
			this.connectedNoded = newConnectedNodesSet;
		}
		
		public Optional<Set<Integer>> getNotConnectedNodesId(Set<Integer> nodesInRadius) {
			if(!nodesInRadius.isEmpty()) {
				Set<Integer> notConnectedNodesId = nodesInRadius.stream()
														.filter(f -> !connectedNoded.contains(f))
														.collect(Collectors.toSet());
				if(!notConnectedNodesId.isEmpty()) {
					updateConnectedNodes(nodesInRadius);
					return Optional.of(notConnectedNodesId);
				}
			}
			
			return Optional.empty();
		}
	}
}

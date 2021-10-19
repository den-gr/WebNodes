package strategy.nodes.p2p;

import java.util.Optional;
import java.util.Set;

/**
 * 
 * Encapsulate the logic of connections between nodes
 *
 */
public interface NodeConnectionStrategy {
	
	/**
	 * If current coordinates of a node are changed then start recalculate connections between nodes 
	 * @param id of a node
	 * @param x coordinate of the node
	 * @param y coordinate of the node
	 * @param connectedNodes a optional set of all nodes that already are connected with the node that has id given in input
	 * @return a list with id of nodes that must be connected to the current node. 
	 * 	if there are not any nodes to connect then return Optional.empty()
	 */
	Optional<Set<Integer>> findNewConnections(int id, int x, int y, Optional<Set<Integer>> connectedNodes);
	
	/**
	 * @param id of the node that must be removed
	 */
	void removeDisconnectedNode(int id);
}

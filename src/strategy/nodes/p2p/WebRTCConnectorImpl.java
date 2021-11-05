package strategy.nodes.p2p;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

import io.vertx.core.http.ServerWebSocket;

public class WebRTCConnectorImpl implements WebRTCConnector {
	//Standard messages
	private final Iterator<Integer> channelIdGenerator = Stream.iterate(0, i -> i + 1).iterator();
	
	private final Map<Integer, ServerWebSocket> clientsWebSocketMap; // a reference to the map, <id, websocket>
	private final NodeConnectionStrategy nodeConnectionStrategy;
	
	public WebRTCConnectorImpl(Map<Integer, ServerWebSocket> clientsWebSocketMap, double nodeRadius) {
		this.clientsWebSocketMap = clientsWebSocketMap;
		this.nodeConnectionStrategy = new NodeConnectionStrategyImpl(nodeRadius);
		System.out.println("New radius: " + nodeRadius);
	}


	@Override
	public void elaborateNewNodeState(JSONObject json) {
		if(json.has("id") && json.has("x") && json.has("y")) {
			int id = json.getInt("id");
			Set<Integer> updatedConnectedNodesSet = null;
			if(json.has("connected_nodes_id")) {//If in node's state is an array with all connected nodes
				updatedConnectedNodesSet = new HashSet<>();
				var jsonArrList = json.getJSONArray("connected_nodes_id").toList();
				
				for (Object  obj : jsonArrList) {
					if(obj instanceof Integer) {
						updatedConnectedNodesSet.add((Integer)obj);
					}else {
						System.out.println("Incorrect type of array value");
					}
				}
				
			}
			var nodesIdToConnect = nodeConnectionStrategy.findNewConnections(json.getInt("id"), json.getInt("x"), json.getInt("y"), 
									updatedConnectedNodesSet == null ? Optional.empty() : Optional.of(updatedConnectedNodesSet));

			
			if(nodesIdToConnect.isPresent()) { // if there are a new nodes that must be connected
				System.out.println(">>> Node " + id + " must connect the new nodes: " + nodesIdToConnect);
				clientsWebSocketMap
					.get(id)
					.writeTextMessage(getConnectionsAvailableMsg(nodesIdToConnect.get()));
			}
			
		}else {
			System.out.println("State JSON has not coordinates.");
		}
		
	}

//	private int getIdFromWebSocket(ServerWebSocket webSocket) {
//		return clientsWebSocketMap.entrySet()
//					.stream()
//					.filter(e -> e.getValue().equals(webSocket))
//					.findFirst()
//					.map(m -> m.getKey())
//					.get();
//	}
	
	
	/**
	 * @param nodesToConnect a set of nodes id, that must be connected 
	 * @return json string where
	 * 		   - type : "connection_available"
	 * 		   - to_be_connected : a json array with objects that have two field: "node_id" and "channel_id"
	 */
	private String getConnectionsAvailableMsg(Set<Integer> nodesToConnect) {
		JSONObject connectionsAvailableMsg = new JSONObject().put("type", "connection_available");
		JSONArray arr = new JSONArray();
		nodesToConnect.forEach(node_id -> {
			JSONObject obj = new JSONObject();
			obj.put("node_id", node_id); //id of node to connect
			obj.put("channel_id", channelIdGenerator.next()); //unique id of the channel, it can be useful for detecting and avoiding duplicate connections 
			arr.put(obj);
		});
		connectionsAvailableMsg.put("to_be_connected", arr);
		return connectionsAvailableMsg.toString();
	}

	@Override
	public void notifyNodeDisconnection(int id) {
		nodeConnectionStrategy.removeDisconnectedNode(id);
	}

}

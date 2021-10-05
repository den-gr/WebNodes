package strategy.nodes.p2p;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.json.JSONObject;

import io.vertx.core.http.ServerWebSocket;

public class WebRTCConnectorImpl implements WebRTCConnector {
	//Standard messages
	private final static JSONObject connectionsAvailableMsg = new JSONObject().put("type", "connection_available");
	
	private final Map<Integer, ServerWebSocket> clientsWebSocketMap; // this is a reference
	private final Map<Integer, List<Integer>> toBeConnected; // <nodeId, list of nodes' id that must be connected to the node> 
	private final Map<Integer, List<Integer>> senderMap; //<nodeId that receive offer, nodeId that send webRTC offer>
	private final NodeConnectionStrategy nodeConnectionStrategy;
	
	public WebRTCConnectorImpl(Map<Integer, ServerWebSocket> clientsWebSocketMap, double nodeRadius) {
		this.clientsWebSocketMap = clientsWebSocketMap;
		this.senderMap = new HashMap<>();
		this.toBeConnected = new HashMap<>();
		this.nodeConnectionStrategy = new NodeConnectionStrategyImpl(nodeRadius);
		System.out.println("New radius: " + nodeRadius);
	}

	@Override
	public void elaborateSignalingMsg(ServerWebSocket SenderWebSocket, JSONObject json) {
		int senderId = getIdFromWebSocket(SenderWebSocket);
		if(json.has("desc")) {
			
			String type = json.getJSONObject("desc").get("type").toString();
			
			if(type.equals("offer")) { // A client offers a connection
				//System.out.println("This is offer from " + getIdFromWebSocket(SenderWebSocket));
				int destinationID = toBeConnected.get(senderId).get(0);
				clientsWebSocketMap.get(destinationID).writeTextMessage(json.toString());
				
				//Make receiver to know who has offered the connection
				if(!senderMap.containsKey(destinationID)) {
					senderMap.put(destinationID, new LinkedList<>());
				}
				senderMap.get(destinationID).add(senderId);
				
			}else if(type.equals("answer")) { //Accepting of connection
				//System.out.println("This is answer from " + getIdFromWebSocket(SenderWebSocket));
				clientsWebSocketMap.get(senderMap.get(senderId).get(0)).writeTextMessage(json.toString());
				senderMap.get(senderId).remove(0);
			}else {
				System.out.println("Wrong type of msg");
			}	
		}else if(json.has("candidate")) {
			String candidate = json.getString("candidate");
			//System.out.println("This is candidate form : "  + getIdFromWebSocket(SenderWebSocket) + "\n" + candidate);
			
			//At moment accepts only host candidate
			if(candidate.contains("host")) {
				int destinationID = toBeConnected.get(senderId).get(0);
				System.out.println(" candidate");
				//route second signaling message 
				clientsWebSocketMap.get(destinationID).writeTextMessage(json.toString());
				
				//Finish with this peer, remove it from list
				toBeConnected.get(senderId).remove(0);
				
				if(!toBeConnected.get(senderId).isEmpty()) {
					//there are another nodes to be connected
					//System.out.println("Send available in elaborate");
					SenderWebSocket.writeTextMessage(getConnectionsAvailableMsg());
				}else {
					toBeConnected.remove(senderId);
				}
			}
		}
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

			
			if(nodesIdToConnect.isPresent()) { // if there are new nodes that must be connected
				System.out.println(">>>Nodes ID to connect:" + nodesIdToConnect + " | for node " + id);
				if(toBeConnected.containsKey(id)){
					toBeConnected.get(id).addAll(nodesIdToConnect.get());
				}else {
					toBeConnected.put(id, new LinkedList<>(nodesIdToConnect.get()));
					System.out.println("Send available in new nodes to connect");
					clientsWebSocketMap.get(id).writeTextMessage(getConnectionsAvailableMsg());
				}
			}
			
		}else {
			System.out.println("State JSON has not coordinates.");
		}
		
	}
	
	private int getIdFromWebSocket(ServerWebSocket webSocket) {
		return clientsWebSocketMap.entrySet()
					.stream()
					.filter(e -> e.getValue().equals(webSocket))
					.findFirst()
					.map(m -> m.getKey())
					.get();
	}
	
	private String getConnectionsAvailableMsg() {
		return connectionsAvailableMsg.toString();
	}

	@Override
	public void notifyNodeDisconnection(int id) {
		senderMap.remove(id);
		toBeConnected.remove(id);
		toBeConnected.entrySet().stream()
			.map(e -> e.getValue())
			.forEach(list -> list.remove(Integer.valueOf(id)));
		nodeConnectionStrategy.removeDisconnectedNode(id);
	}

}

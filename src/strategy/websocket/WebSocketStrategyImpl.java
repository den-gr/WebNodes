package strategy.websocket;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.ServerWebSocket;
import strategy.nodes.p2p.WebRTCConnector;
import strategy.nodes.p2p.WebRTCConnectorImpl;
import utils.ThermometerNodeDataGenerator;

public class WebSocketStrategyImpl implements WebSocketStrategy {
	private final static String CHANEL_M = "generator";
	private final static int COLUMNS = 3;
	private final static int STEP_X = 20;
	private final static int STEP_Y = 20;
	private final static int RADIUS = 50;
	
	
	private final List<ServerWebSocket> generatorList;
	private final List<ServerWebSocket> managerList;
	private final Map<Integer, ServerWebSocket> clientsWebSocketMap;
	private final Map<Integer, JSONObject> clientsConfigurationMap;
	private final ThermometerNodeDataGenerator nodesDataGenerator;
	
	private final EventBus eBus;
	
	private final JSONObject nodesConfiguration;
	private final WebRTCConnector webRTCConnector;
	
		
	public WebSocketStrategyImpl(EventBus ebus) {
		this.eBus = ebus;
		generatorList = new LinkedList<>();
		managerList = new LinkedList<>();
		clientsWebSocketMap = new HashMap<>();
		clientsConfigurationMap = new HashMap<>();
		nodesDataGenerator = new ThermometerNodeDataGenerator(COLUMNS, STEP_X, STEP_Y);
		webRTCConnector = new WebRTCConnectorImpl(clientsWebSocketMap, RADIUS);
		
		nodesConfiguration = new JSONObject();
		nodesConfiguration.put("type", "nodes_configuration");
		nodesConfiguration.put("list", new JSONArray());
	}

	@Override
	public void TextMessageHandler(String message, ServerWebSocket webSocket) {
		try {
			JSONObject json = new JSONObject(message);
			if(json.has("type")) {
				switch(json.getString("type")) {
					case "node_state":
						eBus.publish(CHANEL_M, json.toString());
						webRTCConnector.elaborateNewNodeState(json);
						System.out.println("State msg: " +  json.toString());
						break;
					case "node_setup_demand":
						nodeSetup(webSocket);
						System.out.println("Node setup msg: " +  json.toString());
						break;
					case "generator_setup_demand":
						generatorList.add(webSocket);
						nodeGeneratorSetup(webSocket);
						System.out.println("generator_setup_demand msg: " +  json.toString());
						break;
					case "manager_setup_demand":
						managerList.add(webSocket);
						eBus.consumer(CHANEL_M, m ->
							webSocket.writeTextMessage(m.body().toString()));
						if(!clientsConfigurationMap.isEmpty()) {
							webSocket.writeTextMessage(createNodesConfigurationMsg(clientsConfigurationMap.values()));
						}
						getAllStates();
						System.out.println("Node manager msg: " +  json.toString());
						break;
					case "move_node":
						routeMessageToClient(webSocket, json);
						break;
					case "disconnect_node":
						routeMessageToClient(webSocket, json);
						break;
					case "node_configuration":
						saveNodeConfiguration(json);
						break;
					case "change_node_state":
						routeMessageToClient(webSocket, json);
						break;
					default:
						System.out.println("Type of message is not recognized: " + json.toString());
						webSocket.writeTextMessage(createErrorMsg("Type of message is not recognized"));
				}
				
			}else if(json.has("desc") || json.has("candidate")) {
				webRTCConnector.elaborateSignalingMsg(webSocket, json);
			}else {
				System.out.println("JSON message must have a type");
			}
		}catch (JSONException ex) {
			System.out.println("The message is not a JSON");
		}
		
	}

	@Override
	public void CloseHandler(ServerWebSocket webSocket) {
		System.out.println("Client disconnected "+ webSocket.textHandlerID());
        if(generatorList.contains(webSocket)) {
        	generatorList.remove(webSocket);
        }else if(managerList.contains(webSocket)) {
        	managerList.remove(webSocket);
        }else if(clientsWebSocketMap.values().contains(webSocket)) {
        	int id = clientsWebSocketMap.entrySet()
        				.stream()
        				.filter(e -> e.getValue().equals(webSocket))
        				.findFirst()
        				.map(m -> m.getKey())
        				.get();
        	clientsWebSocketMap.remove(id);
        	clientsConfigurationMap.remove(id);
        	webRTCConnector.notifyNodeDisconnection(id);
        	eBus.publish(CHANEL_M, createNodeDisconnectionMsg(id));
        }else {
        	System.out.println("Type of disconnected client is not recognized");
        }
	}
	
	private void routeMessageToClient(ServerWebSocket webSocket, JSONObject json) {
		if(managerList.contains(webSocket)) {
			if(clientsWebSocketMap.containsKey(json.getInt("id"))) {
				clientsWebSocketMap.get(json.getInt("id")).writeTextMessage(json.toString());
			}else {
				webSocket.writeTextMessage(
						createErrorMsg("Node with id "+ json.getInt("id") +" does not exists"));
			}
			
		}
	}

	private void nodeGeneratorSetup(ServerWebSocket webSocket) {
		//to do 
		JSONObject obj = new JSONObject();
		obj.put("type", "generator_setup");
		obj.put("node_quantity", 2);
		
		webSocket.writeTextMessage(obj.toString());
		
	}
	
	private void nodeSetup(ServerWebSocket webSocket) {
		var nodeData = nodesDataGenerator.getNewNodeData();
		JSONObject obj = new JSONObject();
		obj.put("type", "node_setup");
		obj.put("id", nodeData.id);
		obj.put("x", nodeData.x);
		obj.put("y", nodeData.y);
		obj.put("radius", RADIUS);
		clientsWebSocketMap.put(nodeData.id, webSocket);
		webSocket.writeTextMessage(obj.toString());
	}
	
	private void getAllStates() {
		JSONObject obj = new JSONObject();
		obj.put("type", "notify_state");
		for (ServerWebSocket ws : clientsWebSocketMap.values()) {
			ws.writeTextMessage(obj.toString());
		}
	}
	
	private void saveNodeConfiguration(JSONObject json) {
		if(json.getString("type").equals("node_configuration") && json.has("id")) {
			json.remove("type");
			clientsConfigurationMap.put(json.getInt("id"), json);
			
			var list = new LinkedList<JSONObject>();
			list.add(json);
			
			eBus.publish(CHANEL_M, createNodesConfigurationMsg(list));
		}else {
			System.out.println("Fail saving node configuration");
		}
		
	}
	
	/* Create standard messages */
	
	private String createErrorMsg(String errorMsg) {
		JSONObject obj = new JSONObject();
		obj.put("type", "error");
		obj.put("error_msg", errorMsg);
		return obj.toString();
	}
	
	private String createNodeDisconnectionMsg(int id) {
		JSONObject obj = new JSONObject();
		obj.put("type", "node_disconnection");
		obj.put("id", id);
		return obj.toString();
	}
	
	private String createNodesConfigurationMsg(Collection<JSONObject> confList) {
		return new JSONObject()
				.put("type", "nodes_configuration")
				.put("conf", new JSONArray(confList)).toString();
	}

}

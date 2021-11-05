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
import utils.NodeDataGenerator;

public class WebSocketStrategyImpl implements WebSocketStrategy {
	private final static String CHANEL_M = "manager";
	private final static int COLUMNS = 3;
	private final static int STEP_X = 20;
	private final static int STEP_Y = 20;
	private final static int RADIUS = 50;
	
	
	private final List<ServerWebSocket> generatorList;
	private final List<ServerWebSocket> managerList;
	private final Map<Integer, ServerWebSocket> clientsWebSocketMap;
	private final Map<Integer, JSONObject> clientsConfigurationMap;
	private NodeDataGenerator nodesDataGenerator;
	
	private final EventBus eBus;
	
	private final JSONObject nodesConfiguration;
	private  WebRTCConnector webRTCConnector;
	
		
	public WebSocketStrategyImpl(EventBus ebus) {
		this.eBus = ebus;
		generatorList = new LinkedList<>();
		managerList = new LinkedList<>();
		clientsWebSocketMap = new HashMap<>();
		clientsConfigurationMap = new HashMap<>();
		nodesDataGenerator = new NodeDataGenerator(COLUMNS, STEP_X, STEP_Y, RADIUS, null);
		webRTCConnector = new WebRTCConnectorImpl(clientsWebSocketMap, RADIUS);
		
		nodesConfiguration = new JSONObject();
		nodesConfiguration.put("type", "nodes_configuration");
		nodesConfiguration.put("list", new JSONArray());
	}

	@Override
	public void textMessageHandler(String message, ServerWebSocket webSocket) {
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
					case "disconnect_node":
					case "change_node_state":
					case "stop_sersors_rilevation":
						routeMessageToClient(webSocket, json);
						break;
					case "node_configuration":
						saveNodeConfiguration(json);
						break;
					case "set_configuration":
						setConfiguration(webSocket, json);
						break;
					case "signaling":
						routeMessageToClient(webSocket, json);
						break;
					default:
						System.out.println("Type of message is not recognized: " + json.toString());
						webSocket.writeTextMessage(createErrorMsg("Type of message is not recognized"));
				}
			}else {
				webSocket.writeTextMessage(createErrorMsg("JSON message must have a type"));
				System.out.println("JSON message must have a type: " +  json.toString());
			}
		}catch (JSONException ex) {
			webSocket.writeTextMessage(createErrorMsg("The message is not a JSON"));
			System.out.println("The message is not a JSON: " + message + " | \n Exception: " + ex.getMessage());
		}
		
	}

	@Override
	public void closeHandler(ServerWebSocket webSocket) {
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
	
	/**
	 * Route a message to an client
	 * @param webSocket of sender
	 * @param json that must contains an id of client
	 */
	private void routeMessageToClient(ServerWebSocket webSocket, JSONObject json) {
		if( json.has("id") && clientsWebSocketMap.containsKey(json.getInt("id"))) {
			clientsWebSocketMap.get(json.getInt("id")).writeTextMessage(json.toString());
		}else if(json.has("receiver_id") &&  clientsWebSocketMap.containsKey(json.getInt("receiver_id"))){
			clientsWebSocketMap.get(json.getInt("receiver_id")).writeTextMessage(json.toString());
		}else {
			webSocket.writeTextMessage(
					createErrorMsg("Node with id "+ json.getInt("id") +" is not connected to the server"));
		}	
	}

	
	private void setConfiguration(ServerWebSocket webSocket, JSONObject json) {
		int cols = json.has("cols") ? json.getInt("cols") : COLUMNS;
		int stepX = json.has("stepX") ? json.getInt("stepX") : STEP_X;
		int stepY = json.has("stepY") ? json.getInt("stepY") : STEP_Y;
		
		
		var configuration =  json.has("configuration")? json.getJSONObject("configuration") : null;
		
		int radius = configuration != null && configuration.has("radius") ? configuration.getInt("radius") : RADIUS;
		
		nodesDataGenerator =  new NodeDataGenerator(cols, stepX, stepY, radius, configuration);
		webRTCConnector = new WebRTCConnectorImpl(clientsWebSocketMap, radius);
		
		if(json.has("node_quantity")) {
			if(generatorList.isEmpty()) {
				webSocket.writeTextMessage(createErrorMsg("Configurations are saved, but there are not active node generators"));
				return;
			}
			
			JSONObject obj = new JSONObject();
			obj.put("type", "close_all_nodes");
			for (ServerWebSocket serverWebSocket : generatorList) {
				serverWebSocket.writeTextMessage(obj.toString());
			}
			
			int node_quantity = json.getInt("node_quantity");
			JSONObject createNodeMsg = new JSONObject();
			createNodeMsg.put("type", "generator_setup");
			createNodeMsg.put("node_quantity", node_quantity / generatorList.size());
			
			if(node_quantity % generatorList.size() ==  0) {
				generatorList.forEach(generator -> {
					generator.writeTextMessage(createNodeMsg.toString());
				});
			}else {
				JSONObject createNodeMsgWithRest = new JSONObject();
				createNodeMsgWithRest.put("type", "generator_setup");
				createNodeMsgWithRest.put("node_quantity", (node_quantity / generatorList.size()) +
												   		   (node_quantity % generatorList.size()));
				
				generatorList.get(0).writeTextMessage(createNodeMsgWithRest.toString());
				for(int i = 1; i < generatorList.size(); i++) {
					generatorList.get(i).writeTextMessage(createNodeMsg.toString());
				}
			}
		}
	}
	
	private void nodeSetup(ServerWebSocket webSocket) {
		var nodeData = nodesDataGenerator.getNewNodeData();
		JSONObject obj = new JSONObject();
		obj.put("type", "node_setup");
		obj.put("node_configuration", nodeData.getConfiguration());
		obj.put("x", nodeData.x);
		obj.put("y", nodeData.y);
		obj.put("id", nodeData.id);
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
	
	/**
	 * Save in "cache" the configuration of node
	 * if a new manager will be connected all nodes configurations will be send to it from "cache"
	 * @param json with node configuration
	 */
	private void saveNodeConfiguration(JSONObject json) {
		if(json.getString("type").equals("node_configuration") && json.has("id")) {
			json.remove("type");
			clientsConfigurationMap.put(json.getInt("id"), json);
			
			var list = new LinkedList<JSONObject>();
			list.add(json);
			System.out.println("Send Configuration");
			
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
				.put("type", "nodes_configurations")
				.put("configurations", new JSONArray(confList)).toString();
	}

}

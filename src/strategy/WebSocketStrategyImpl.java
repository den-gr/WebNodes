package strategy;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.ServerWebSocket;
import utils.ThermometerNodeDataGenerator;

public class WebSocketStrategyImpl implements WebSocketStrategy {
	private final static String CHANEL_M = "generator";
	
	private final List<ServerWebSocket> generatorList;
	private final List<ServerWebSocket> managerList;
	private final Map<Integer, ServerWebSocket> clientsMap;
	private final ThermometerNodeDataGenerator nodesDataGenerator;
	
	private final EventBus eBus;
	
		
	public WebSocketStrategyImpl(EventBus ebus) {
		this.eBus = ebus;
		generatorList = new LinkedList<>();
		managerList = new LinkedList<>();
		clientsMap = new HashMap<>();
		nodesDataGenerator = new ThermometerNodeDataGenerator(3, 20, 20);
	}

	@Override
	public void TextMessageHandler(String message, ServerWebSocket webSocket) {
		try {
			JSONObject json = new JSONObject(message);
			if(json.has("type")) {
				switch(json.getString("type")) {
					case "node_state":
						eBus.publish(CHANEL_M, json.toString());
						System.out.println("State msg: " +  json.toString());
						break;
					case "node_setup_demand":
						nodeSetup(webSocket);
						System.out.println("Node setup msg: " +  json.toString());
						break;
					case "generator_setup_demand":
						generatorList.add(webSocket);
						nodeGeneratorSetup();
						System.out.println("generator_setup_demand msg: " +  json.toString());
						break;
					case "manager_setup_demand":
						managerList.add(webSocket);
						eBus.consumer(CHANEL_M, m ->
							webSocket.writeTextMessage(m.body().toString()));
						getAllStates();
						System.out.println("Node manager msg: " +  json.toString());
						break;
					case "move_node":
						routeMessageToClient(webSocket, json);
						break;
					case "disconnect_node":
						routeMessageToClient(webSocket, json);
						break;
					case "set_temperature":
						routeMessageToClient(webSocket, json);
						break;
					default:
						System.out.println("Type of message is not recognized: " + json.toString());
				}
				
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
        }else if(clientsMap.values().contains(webSocket)) {
        	int id = clientsMap.entrySet()
        				.stream()
        				.filter(e -> e.getValue().equals(webSocket))
        				.findFirst()
        				.map(m -> m.getKey())
        				.get();
        	clientsMap.remove(id);
        	eBus.publish(CHANEL_M, createNodeDisconnectionMsg(id));
        }else {
        	System.out.println("Type of disconnected client is not recognized");
        }
	}
	
	private void routeMessageToClient(ServerWebSocket webSocket, JSONObject json) {
		if(managerList.contains(webSocket)) {
			if(clientsMap.containsKey(json.getInt("id"))) {
				clientsMap.get(json.getInt("id")).writeTextMessage(json.toString());
			}else {
				webSocket.writeTextMessage(
						createError("Node with id "+ json.getInt("id") +" does not exists"));
			}
			
		}
	}

	private void nodeGeneratorSetup() {
		//to do 
		JSONObject obj = new JSONObject();
		obj.put("type", "generator_setup");
		obj.put("node_quantity", 2);
		for (ServerWebSocket sock : generatorList) {
			sock.writeTextMessage(obj.toString());
		}
	}
	
	private void nodeSetup(ServerWebSocket webSocket) {
		var nodeData = nodesDataGenerator.getNewNodeData();
		JSONObject obj = new JSONObject();
		obj.put("type", "node_setup");
		obj.put("id", nodeData.id);
		obj.put("x", nodeData.x);
		obj.put("y", nodeData.y);
		clientsMap.put(nodeData.id, webSocket);
		webSocket.writeTextMessage(obj.toString());
	}
	
	private String createError(String errorMsg) {
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
	
	private void getAllStates() {
		JSONObject obj = new JSONObject();
		obj.put("type", "notify_state");
		for (ServerWebSocket ws : clientsMap.values()) {
			ws.writeTextMessage(obj.toString());
		}
	}
}

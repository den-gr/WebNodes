package test;

import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

public class WebSocketClient implements WebSocket.Listener {
        private final CountDownLatch latch;
        private NodeObserver nodeObserver;

        public WebSocketClient(CountDownLatch latch) { 
        	this.latch = latch;
    	}
        
        public void setNodeObserver(NodeObserver nodeObserver) {
        	this.nodeObserver = nodeObserver;
        }
        @Override
        public void onOpen(WebSocket webSocket) {
        	webSocket.sendText("{\"type\": \"manager_setup_demand\"}", true);
            print("onOpen using subprotocol " + webSocket.getSubprotocol());
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        	print("onText received " + data);
        	JSONObject json = new JSONObject(data.toString());
        	switch(json.getString("type")) {
        		case "error":
        			print("Error receive: " + json.getString("error_msg"));
        			break;
        		case "node_state":
        			elaborateState(json);
        			break;
        		case "nodes_configurations":
        			elaborateConfiguration(json);
        			break;
        		case "node_disconnection":
        			nodeDisconnection(json);
        			break;
    			default:
    				nodeObserver.errorOccured();
    				print("Unrecognized type of message");
    				
        	}
        	if(json.getString("type").equals("error")) {
        		
        	}
        	
            
            latch.countDown();
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        private void nodeDisconnection(JSONObject json) {
        	if(json.has("id")) {
        		nodeObserver.removeNode(json.getInt("id"));
        	}else {
        		nodeObserver.errorOccured();
        	}
		}

		@Override
        public void onError(WebSocket webSocket, Throwable error) {
           print("Bad day! " + webSocket.toString() + " \n  Error: " + error.getMessage());
            
            WebSocket.Listener.super.onError(webSocket, error);
        }
        
        
        private void elaborateState(JSONObject json) {
        	if(json.has("id")) {
        		var nodeState = nodeObserver.getNodeState(json.getInt("id"));
        		if(nodeState != null) {
        			if(json.has("x")) {
            			nodeState.setX(json.getInt("x"));
            		}
            		if(json.has("y")) {
            			nodeState.setY(json.getInt("y"));
            		}
            		if(json.has("connected_nodes_id")) {
            			var list = json.getJSONArray("connected_nodes_id").toList();
            			nodeState.setConnectedNodes(list.stream().map(m -> (Integer)m).collect(Collectors.toList()));
            		}
            		if(json.has("devices_state")) {
            			var values = json.getJSONObject("devices_state");
            			var keys = values.keySet();
            			keys.forEach(key -> nodeState.setValue(key, values.get(key)));
            		}
            		
        		}else {
        			nodeObserver.errorOccured();
        			print("Error: state of not existing node");
        		}
        		
        		
        	}else {
        		nodeObserver.errorOccured();
        		print("State has not id field");
        	}
        }
        
        private void elaborateConfiguration(JSONObject jsonn) {
        	if(jsonn.has("configurations")) {
        		JSONArray array = jsonn.getJSONArray("configurations");
        		List<JSONObject> confList = new ArrayList<>();;
        		for(int i = 0; i < array.length(); i++) {
        			confList.add(array.getJSONObject(i));
        		}
				confList.forEach(json -> {
        			if(json.has("id") && json.has("node_name") && json.has("sensors") && json.has("actuators") && json.has("radius")) {
        		
                		Map<String, Object> valuesMap = new HashMap<>();
                		valuesMap = updateDevicesMap(valuesMap, json.getJSONArray("sensors"));
                		valuesMap = updateDevicesMap(valuesMap, json.getJSONArray("actuators"));
                		
                		Map<String, String> namesMap = new HashMap<>();
                		namesMap = updateDeviceNamesMap(namesMap, json.getJSONArray("sensors"));
                		namesMap = updateDeviceNamesMap(namesMap, json.getJSONArray("actuators"));
                		
                		nodeObserver.addNode(json.getInt("id"), json.getInt("radius"), valuesMap, namesMap);
                		
                		
                	}else {
                		 nodeObserver.errorOccured();
                		 print("Congiguration field missing");
                	}
        		});
        	}else {
        		 print("The ongigurations field missing");
        		nodeObserver.errorOccured();
        	}
        	
        }
        
        private Map<String, Object> updateDevicesMap(Map<String, Object> map, JSONArray arr){
        	List<JSONObject> list = new ArrayList<>();
    		for(int i = 0; i < arr.length(); i++) {
    			list.add(arr.getJSONObject(i));
    		}
        	list.stream().forEach(obj -> map.put(obj.getString("value_name"), null));
        	return map;
        }
        
        private Map<String, String> updateDeviceNamesMap(Map<String, String> map, JSONArray arr){
        	List<JSONObject> list = new ArrayList<>();
    		for(int i = 0; i < arr.length(); i++) {
    			list.add(arr.getJSONObject(i));
    		}
    		list.stream().forEach(obj -> map.put(obj.has("sensor_name") ? obj.getString("sensor_name") : obj.getString("actuator_name"), obj.getString("value_name")));
        	return map;
        }
        
        
        private void print(String s ) {
        	System.out.println("TEST > " + s);
        }
        
        
        
}

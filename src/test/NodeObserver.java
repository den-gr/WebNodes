package test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NodeObserver {
	private boolean isErrorOccured = false;
	private final Map<Integer, NodeState> nodesStates = new HashMap<>();
	
	public void addNode(int id, int radius,  Map<String, Object> values,  Map<String, String> devicesNames) {
		var nodeState = new NodeState(id, radius, values, devicesNames);
		nodesStates.put(id, nodeState);
	}
	
	public NodeState getNodeState(int id) {
		return this.nodesStates.get(id);
	}
	
	public void removeNode(int id) {
		nodesStates.remove(id);
	}
	
	public int getNumberOfNodes() {
		return nodesStates.size();
	}
	
	public void errorOccured() {
		isErrorOccured = true;
	}
	
	public boolean isErrorOccured() {
		return this.isErrorOccured;
	}
	
	public List<NodeState> getNodesStates(){
		return this.nodesStates.values().stream().collect(Collectors.toList());
	}
	
	public class NodeState{
		private final Map<String, Object> values; // <value_name, value>
		private final Map<String, String> devices_misurations; // <device_name, value_name>
		private final int id;
		private int x = -1;
		private int y = -1;
		private int radius;
		private List<Integer> connected_nodes_id; 

		public NodeState(int id, int radius,  Map<String, Object> devices, Map<String, String> devices_misurations) {
			
			this.id = id;
			this.radius = radius;
			this.values = devices;
			this.devices_misurations = devices_misurations;
		}
		
		public Object getValue(String deviceName) {
			return values.get(devices_misurations.get(deviceName));
		}
		
		public void setX(int x) {
			this.x = x;
		}
		
		public void setY(int y) {
			this.y = y;
		}
		
		public void setConnectedNodes(List<Integer> connectedNodesId) {
			this.connected_nodes_id = connectedNodesId;
		}
		
		public void setValue(String valueName, Object value) {
			if(values.containsKey(valueName)) {
				values.put(valueName, value);
			}else {
				System.out.println("TEST > Error: not existing valueName");
			}
		}
		
		public int getId() { return this.id;}
		public int getX() { return this.x;}
		public int getY() { return this.y;}
		public int getRadius() { return this.radius;}
		public List<Integer> getConnectedNodes(){ return this.connected_nodes_id;}
		
	}
}

package utils;

import java.util.Iterator;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Helps in creating of nodes data.
 */
public class NodeDataGenerator {
	Iterator<Integer> idGenerator = Stream.iterate(0, i -> i + 1).iterator();
	private final int max_cols;
	private final int stepX;
	private final int stepY;
	private final int radius;
	private final JSONObject configuration;
	
	private int lastX = -1;
	private int lastY = 0;
	
	public NodeDataGenerator(int cols, int stepX, int stepY, int radius, JSONObject congiguration) {
		this.max_cols = cols;
		this.stepX = stepX;
		this.stepY = stepY;
		this.radius = radius;
		this.configuration = congiguration;
	}
	
	public NodeData getNewNodeData() {
		return new NodeData();
	}
	
	/** 
	 * Allow easy access to created node data
	 */
	public class NodeData{
		public final int id;
		public final int x;
		public final int y;
		
		public NodeData() {
			int tmpx; 
			int tmpy;
			if(++lastX >= max_cols) {
				lastX = 0;
				lastY++;
			}
			tmpx = lastX;
			tmpy = lastY;
			
			this.id = idGenerator.next();
			this.x = tmpx * stepX;
			this.y = tmpy * stepY;
		}
		
		
		public JSONObject getConfiguration() {
			if(configuration != null) {
				return configuration;
			}
			return getDefaultConfiguration();
			
		}
		
		private JSONObject getDefaultConfiguration() {
			var obj = new JSONObject();
			var actuators = new JSONArray();
			var sensors = new JSONArray();
			var thermometer = new JSONObject();
			var led = new JSONObject();
			
			thermometer.put("sensor_name", "thermometer");
			thermometer.put("value_name", "temperature");
			thermometer.put("value_type", "real");
			sensors.put(thermometer);
			
			led.put("actuator_name", "led");
			led.put("value_name", "turn_on");
			led.put("value_type", "boolean");
			actuators.put(led);
			
			
			
			obj.put("node_name", "Thermometer");
			obj.put("id", this.id);
			obj.put("sensors", sensors);
			obj.put("actuators", actuators);
			obj.put("radius", radius);
			return obj;
			
		}
	}
}

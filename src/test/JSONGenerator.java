package test;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSONGenerator {
	public final static String REAL_SENSOR_NAME = "Thermometer";
	public final static String BOOLEAN_SENSOR_NAME = "Motion detector";
	public final static String NATURAL_SENSOR_NAME = "Car counter";
	public final static String INTEGER_SENSOR_NAME = "Elevator monitoring";
	public final static String BOOLEAN_ACTUATOR_NAME = "Led";
	
	public static String getCongigurationMsg(int node_quantity, int cols, int stepX, int stepY, int radius) {
		JSONObject json = new JSONObject();
		
		JSONObject configuration = new JSONObject();
		var actuators = new JSONArray();
		var sensors = new JSONArray();
		
		sensors.put(createSensor(REAL_SENSOR_NAME, "Temperature", "real"));
		sensors.put(createSensor(NATURAL_SENSOR_NAME, "Number of cars", "natural"));
		sensors.put(createSensor(BOOLEAN_SENSOR_NAME, "motion detect", "boolean"));
		sensors.put(createSensor(INTEGER_SENSOR_NAME, "Elevator floor", "integer"));
		actuators.put(createActuator(BOOLEAN_ACTUATOR_NAME, "Is on", "boolean"));
		
		configuration.put("sensors", sensors);
		configuration.put("actuators", actuators);
		configuration.put("radius", radius);
		configuration.put("node_name", "Test Name");
		configuration.put("type", "node_configuration");
		
		
		json.put("type", "set_configuration");
		json.put("configuration", configuration);
		json.put("node_quantity", node_quantity);
		json.put("cols", cols);
		json.put("stepX", stepX);
		json.put("stepY", stepY);
		return json.toString();
	}
	
	
	public static String getChangeNodeStateMsg(int id, String deviceType, String device_name, Object value) {
		JSONObject json = new JSONObject();
		json.put("type", "change_node_state");
		json.put("id", id);
		json.put("device_type", deviceType);
		json.put(deviceType+"_name", device_name);
		json.put("value", value);
		return json.toString();
	}
	
	public static String getDisconnectNodeMsg(int id) {
		JSONObject json = new JSONObject();
		json.put("type", "disconnect_node");
		json.put("id", id);
		return json.toString();
	}
	
	public static String getMoveNodeMsg(int id, int x, int  y) {
		JSONObject json = new JSONObject();
		json.put("type", "move_node");
		json.put("id", id);
		json.put("x", x);
		json.put("y", y);
		return json.toString();
	}
	
	private static JSONObject createSensor(String sensorName, String value_name,String valueType) {
		var obj = new JSONObject();
		obj.put("sensor_name", sensorName);
		obj.put("value_name", value_name);
		obj.put("value_type", valueType);
		return obj;
	}
	
	private static JSONObject createActuator(String  actuatorName, String value_name,String valueType) {
		var obj = new JSONObject();
		obj.put("actuator_name", actuatorName);
		obj.put("value_name", value_name);
		obj.put("value_type", valueType);
		return obj;
	}
	
	public static String createStopRilevationMsg(int id) {
		var json = new JSONObject();
		json.put("type", "stop_sersors_rilevation");
		json.put("id", id);
		return json.toString();
	}

}

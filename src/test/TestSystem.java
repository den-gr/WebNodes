package test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import main.Server;
import test.NodeObserver.NodeState;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.CountDownLatch;

class TestSystem {
	final static int NODE_ID_0 = 0;
	final static int NODE_ID_1 = 1;
	final static int NODE_ID_2 = 2;
	final static int NODE_ID_999 = 999;
	
	final static int NUM_COLUMNS = 3;
	final static int STEP_X = 20;
	final static int STEP_Y = 25;
	final static int RADIUS = 50;
	
	final static int DELAY = 100;
	final static int LONG_DELAY = 1000;
	
	final static String SENSOR = "sensor";
	final static String ACTUATOR = "actuator";
	private static WebSocket ws;
	private NodeObserver nodeObserver;
	private static WebSocketClient wsclient;
	

	
	@BeforeAll
	 static void setUpBeforeClass() throws Exception {
		Vertx vertx = Vertx.vertx();
		Server server = new Server(8081);
		vertx.deployVerticle(server);
		
		try {	
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		CountDownLatch latch = new CountDownLatch(1);
		
		TestSystem.wsclient = new WebSocketClient(latch);
		
		TestSystem.ws = HttpClient
                .newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:8081"), wsclient)
                .join();
	}
	

	@BeforeEach
	void setUp() throws Exception {
		nodeObserver = new NodeObserver();
		wsclient.setNodeObserver(nodeObserver);
	
	}
	
	@Test
	void testConfiguration() {
		final int node_quantity = 1;
		
		//initialize system
		ws.sendText(JSONGenerator.getCongigurationMsg(node_quantity, NUM_COLUMNS, STEP_X, STEP_Y, RADIUS), true);
		wait(getSetUpDelay(node_quantity));
		assertFalse(nodeObserver.isErrorOccured());
		
		List<NodeState> list = nodeObserver.getNodesStates();
		//check radius
		list.forEach(e -> assertEquals(RADIUS, e.getRadius()));
		//check id 
		assertEquals(node_quantity, list.stream()
										.map(m -> m.getId())
										.distinct()
										.count());
		//check coordinates
		list.forEach(e -> {
			assertEquals(e.getX(), STEP_X*(e.getId()%NUM_COLUMNS));
			assertEquals(e.getY(), STEP_Y*(e.getId()/NUM_COLUMNS));
		});
	}


	
	@Test
	void testNodeStateChange() {
		final int node_quantity = 2; 
		
		assertEquals(nodeObserver.getNumberOfNodes(), 0);
		
		//initialize system
		ws.sendText(JSONGenerator.getCongigurationMsg(node_quantity, NUM_COLUMNS, STEP_X, STEP_Y, RADIUS), true);
		wait(getSetUpDelay(node_quantity));
		assertFalse(nodeObserver.isErrorOccured());
		
		ws.sendText(JSONGenerator.createStopRilevationMsg(NODE_ID_0), true);
		wait(DELAY);
		
		final float newRealValue = 99.99f;
		
		Object currentValue = nodeObserver.getNodeState(NODE_ID_0).getValue(JSONGenerator.REAL_SENSOR_NAME);
		assertNotNull(currentValue);
		Float currentValueAfterCast = currentValue instanceof Integer ? ((Integer)currentValue).floatValue() 
														:  ((BigDecimal)currentValue).floatValue();
		assertNotEquals(newRealValue, currentValueAfterCast, 0.001);
		
		
		ws.sendText(JSONGenerator.getChangeNodeStateMsg(NODE_ID_0, SENSOR, JSONGenerator.REAL_SENSOR_NAME, newRealValue), true);
		wait(DELAY);
		currentValue = nodeObserver.getNodeState(NODE_ID_0).getValue(JSONGenerator.REAL_SENSOR_NAME);
		assertTrue(currentValue instanceof BigDecimal);
		assertEquals(newRealValue, ((BigDecimal)currentValue).doubleValue(), 0.001);
		
		currentValue = nodeObserver.getNodeState(NODE_ID_1).getValue(JSONGenerator.BOOLEAN_ACTUATOR_NAME);
		assertNotNull(currentValue);
		assertTrue(currentValue instanceof Boolean);
		final Boolean newBooleanValue = !(boolean)currentValue;
		
		ws.sendText(JSONGenerator.getChangeNodeStateMsg(NODE_ID_1, ACTUATOR, JSONGenerator.BOOLEAN_ACTUATOR_NAME, newBooleanValue), true);
		wait(DELAY);
		
		currentValue = nodeObserver.getNodeState(NODE_ID_1).getValue(JSONGenerator.BOOLEAN_ACTUATOR_NAME);
		assertTrue(currentValue instanceof Boolean);
		assertEquals(currentValue, (boolean)newBooleanValue);
	}
	
	
	@Test
	void testNodeConnections() {
		final int node_quantity = 3; 
		
		//initialize system
		ws.sendText(JSONGenerator.getCongigurationMsg(node_quantity, NUM_COLUMNS, STEP_X, STEP_Y, RADIUS), true);
		wait(getSetUpDelay(node_quantity));
		assertFalse(nodeObserver.isErrorOccured());
		
		NodeState nodeState_0 = nodeObserver.getNodeState(NODE_ID_0);
		NodeState nodeState_1 = nodeObserver.getNodeState(NODE_ID_1);
		NodeState nodeState_2 = nodeObserver.getNodeState(NODE_ID_2);
		
		assertTrue(nodeState_0.getConnectedNodes().contains(NODE_ID_1));
		assertTrue(nodeState_0.getConnectedNodes().contains(NODE_ID_2));
		
		assertTrue(nodeState_1.getConnectedNodes().contains(NODE_ID_0));
		assertTrue(nodeState_1.getConnectedNodes().contains(NODE_ID_2));
		
		assertTrue(nodeState_2.getConnectedNodes().contains(NODE_ID_0));
		assertTrue(nodeState_2.getConnectedNodes().contains(NODE_ID_1));
		
		
		int newCoordinate = -100;
		ws.sendText(JSONGenerator.getMoveNodeMsg(NODE_ID_0, newCoordinate, newCoordinate), true);
		wait(LONG_DELAY);
		
		assertEquals(nodeState_0.getX(), newCoordinate);
		assertEquals(nodeState_0.getY(), newCoordinate);
		assertEquals(nodeState_0.getConnectedNodes().size(), 0);
		
		assertFalse(nodeState_1.getConnectedNodes().contains(NODE_ID_0));
		assertTrue(nodeState_1.getConnectedNodes().contains(NODE_ID_2));
		
		assertFalse(nodeState_2.getConnectedNodes().contains(NODE_ID_0));
		assertTrue(nodeState_2.getConnectedNodes().contains(NODE_ID_1));
		
		newCoordinate = 1;
		ws.sendText(JSONGenerator.getMoveNodeMsg(NODE_ID_0, newCoordinate, newCoordinate), true);
		wait(LONG_DELAY);
		
		assertEquals(nodeState_0.getX(), newCoordinate);
		assertEquals(nodeState_0.getY(), newCoordinate);
		
		assertTrue(nodeState_0.getConnectedNodes().contains(NODE_ID_1));
		assertTrue(nodeState_0.getConnectedNodes().contains(NODE_ID_2));
		
		assertTrue(nodeState_1.getConnectedNodes().contains(NODE_ID_0));
		assertTrue(nodeState_1.getConnectedNodes().contains(NODE_ID_2));
		
		assertTrue(nodeState_2.getConnectedNodes().contains(NODE_ID_0));
		assertTrue(nodeState_2.getConnectedNodes().contains(NODE_ID_1));
		
		newCoordinate = 2;
		ws.sendText(JSONGenerator.getMoveNodeMsg(NODE_ID_0, newCoordinate, newCoordinate), true);
		wait(DELAY);
		
		assertEquals(nodeState_0.getX(), newCoordinate);
		assertEquals(nodeState_0.getY(), newCoordinate);
		
		assertEquals(nodeState_0.getConnectedNodes().size(), 2);
		assertEquals(nodeState_1.getConnectedNodes().size(), 2);
		assertEquals(nodeState_2.getConnectedNodes().size(), 2);
	}
	
	@Test
	void testDisconnection() {
		int node_quantity = 1;
		
		//first controls
		assertEquals(0, nodeObserver.getNumberOfNodes());
		ws.sendText(JSONGenerator.getDisconnectNodeMsg(NODE_ID_999), true);
		assertEquals(0, nodeObserver.getNumberOfNodes());
		
		//initialize system
		ws.sendText(JSONGenerator.getCongigurationMsg(node_quantity, NUM_COLUMNS, STEP_X, STEP_Y, RADIUS), true);
		wait(getSetUpDelay(node_quantity));
		assertFalse(nodeObserver.isErrorOccured());
		
		//check that all nodes are created
		assertEquals(node_quantity, nodeObserver.getNumberOfNodes());
		
		//Disconnect all
		node_quantity--;
		for(int nodeId = 0;  node_quantity >= 0; node_quantity--, nodeId++) {
			ws.sendText(JSONGenerator.getDisconnectNodeMsg(nodeId), true);
			wait(DELAY);
			assertEquals(node_quantity, nodeObserver.getNumberOfNodes());
		}
	}
	
	
	@Test
	void testSensorRilevation() {
		int node_quantity = 1;
		
		//initialize system
		ws.sendText(JSONGenerator.getCongigurationMsg(node_quantity, NUM_COLUMNS, STEP_X, STEP_Y, RADIUS), true);
		wait(getSetUpDelay(node_quantity));
		assertFalse(nodeObserver.isErrorOccured());
		
		float realValue = ((Integer) nodeObserver.getNodeState(NODE_ID_0).getValue(JSONGenerator.REAL_SENSOR_NAME)).floatValue();
		int naturalValue = ((Integer) nodeObserver.getNodeState(NODE_ID_0).getValue(JSONGenerator.NATURAL_SENSOR_NAME));
		int integerValue = ((Integer) nodeObserver.getNodeState(NODE_ID_0).getValue(JSONGenerator.INTEGER_SENSOR_NAME));
		boolean booleanValue = (Boolean) nodeObserver.getNodeState(NODE_ID_0).getValue(JSONGenerator.BOOLEAN_ACTUATOR_NAME);
		
		wait(25000);
		assertFalse(realValue ==  ((Integer) nodeObserver.getNodeState(NODE_ID_0).getValue(JSONGenerator.REAL_SENSOR_NAME)).floatValue()
				&& naturalValue == (Integer) nodeObserver.getNodeState(NODE_ID_0).getValue(JSONGenerator.NATURAL_SENSOR_NAME)
				&& integerValue == (Integer) nodeObserver.getNodeState(NODE_ID_0).getValue(JSONGenerator.INTEGER_SENSOR_NAME)
				&& booleanValue == (Boolean) nodeObserver.getNodeState(NODE_ID_0).getValue(JSONGenerator.BOOLEAN_ACTUATOR_NAME));
		
		
		
	}
	
	private void wait(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private int  getSetUpDelay(int nodeQuantity) {
		int delay  = nodeQuantity > 2 ? nodeQuantity * 800 : nodeQuantity * 200;
		return 1500 + delay;
	}
}

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

class TestSystemWithTwoManger {
	final static int NODE_ID_0 = 0;
	final static int NODE_ID_1 = 1;
	final static int NODE_ID_2 = 2;
	final static int NODE_ID_999 = 999;
	
	final static int NUM_COLUMNS = 3;
	final static int STEP_X = 20;
	final static int STEP_Y = 25;
	final static int RADIUS = 50;
	
	final static int DELAY = 100;
	final static int LONG_DELAY = 1500;
	
	final static String SENSOR = "sensor";
	final static String ACTUATOR = "actuator";
	private static WebSocket ws;
	private static WebSocket ws2;
	private NodeObserver nodeObserver;
	private NodeObserver nodeObserver2;
	private static WebSocketClient wsclient;
	private static WebSocketClient wsclient2;
	

	
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
		
		TestSystemWithTwoManger.wsclient = new WebSocketClient(latch);
		TestSystemWithTwoManger.wsclient2 = new WebSocketClient(latch);
		
		TestSystemWithTwoManger.ws = HttpClient
                .newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:8081"), wsclient)
                .join();
		
		TestSystemWithTwoManger.ws2 = HttpClient
                .newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:8081"), wsclient2)
                .join();
	}
	

	@BeforeEach
	void setUp() throws Exception {
		nodeObserver = new NodeObserver();
		nodeObserver2 = new NodeObserver();
		wsclient.setNodeObserver(nodeObserver);
		wsclient2.setNodeObserver(nodeObserver2);
	
	}
	
	@Test
	void testConfiguration() {
		final int node_quantity = 5;
		
		//initialize system
		ws2.sendText(JSONGenerator.getCongigurationMsg(node_quantity, NUM_COLUMNS, STEP_X, STEP_Y, RADIUS), true);
		wait(getSetUpDelay(node_quantity));
		assertFalse(nodeObserver.isErrorOccured());
		
		testConfiguration(node_quantity, nodeObserver.getNodesStates());
		testConfiguration(node_quantity, nodeObserver2.getNodesStates());
	}
	
	private void testConfiguration(int node_quantity, List<NodeState> list) {
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
		nodeStateChangeRealBefore(currentValue, newRealValue);
		currentValue = nodeObserver2.getNodeState(NODE_ID_0).getValue(JSONGenerator.REAL_SENSOR_NAME);
		nodeStateChangeRealBefore(currentValue, newRealValue);
		
		ws2.sendText(JSONGenerator.getChangeNodeStateMsg(NODE_ID_0, SENSOR, JSONGenerator.REAL_SENSOR_NAME, newRealValue), true);
		wait(DELAY);
		currentValue = nodeObserver.getNodeState(NODE_ID_0).getValue(JSONGenerator.REAL_SENSOR_NAME);
		nodeStateChangeRealAfter(currentValue, newRealValue);
		currentValue = nodeObserver2.getNodeState(NODE_ID_0).getValue(JSONGenerator.REAL_SENSOR_NAME);
		nodeStateChangeRealAfter(currentValue, newRealValue);
		
		currentValue = nodeObserver.getNodeState(NODE_ID_1).getValue(JSONGenerator.BOOLEAN_ACTUATOR_NAME);
		assertNotNull(currentValue);
		assertTrue(currentValue instanceof Boolean);
		final Boolean newBooleanValue = !(boolean)currentValue;
		
		ws.sendText(JSONGenerator.getChangeNodeStateMsg(NODE_ID_1, ACTUATOR, JSONGenerator.BOOLEAN_ACTUATOR_NAME, newBooleanValue), true);
		wait(DELAY);
		
		nodeStateChangeBooleanAfter(nodeObserver.getNodeState(NODE_ID_1).getValue(JSONGenerator.BOOLEAN_ACTUATOR_NAME), newBooleanValue);
		nodeStateChangeBooleanAfter(nodeObserver2.getNodeState(NODE_ID_1).getValue(JSONGenerator.BOOLEAN_ACTUATOR_NAME), newBooleanValue);
	}
	
	private void nodeStateChangeRealBefore(Object currentValue, float newRealValue) {
		assertNotNull(currentValue);
		Float currentValueAfterCast = currentValue instanceof Integer ? ((Integer)currentValue).floatValue() 
														:  ((BigDecimal)currentValue).floatValue();
		assertNotEquals(newRealValue, currentValueAfterCast, 0.001);
	}
	
	private void nodeStateChangeRealAfter(Object currentValue, float newRealValue) {
		assertTrue(currentValue instanceof BigDecimal);
		assertEquals(newRealValue, ((BigDecimal)currentValue).doubleValue(), 0.001);
	}
	
	private void nodeStateChangeBooleanAfter(Object currentValue, boolean newBooleanValue) {
		assertTrue(currentValue instanceof Boolean);
		assertEquals((boolean)currentValue, newBooleanValue);
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

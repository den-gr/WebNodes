package strategy.nodes.p2p;

import org.json.JSONObject;

import io.vertx.core.http.ServerWebSocket;

/**
 * 
 * Helps interconnect nodes with WebRTC
 *
 */
public interface WebRTCConnector {
	
	/**
	 * @param json with state of a node
	 */
	void elaborateNewNodeState(JSONObject json);
	
	/**
	 * If a node is disconnected from the system, another nodes must not try to connect to it.
	 * @param id of disconnected node
	 */
	void notifyNodeDisconnection(int id);
}

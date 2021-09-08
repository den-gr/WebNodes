package strategy.nodes.p2p;

import org.json.JSONObject;

import io.vertx.core.http.ServerWebSocket;

public interface WebRTCConnector {
	
	/**
	 * @param SenderWebSocket a WebSocket of sender
	 * @param json a WebRTC signaling message
	 */
	void elaborateSignalingMsg(ServerWebSocket SenderWebSocket, JSONObject json);
	
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
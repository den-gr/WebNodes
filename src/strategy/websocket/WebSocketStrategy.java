package strategy.websocket;

import io.vertx.core.http.ServerWebSocket;

/**
 * 
 * Encapsulate a strategy of handling websocket channels event
 * 
 */
public interface WebSocketStrategy {
	
	/**
	 * Handle a new message 
	 * @param message arrived from websocket
	 * @param webSocket 
	 */
	void textMessageHandler(String message, ServerWebSocket webSocket);
	
	/**
	 * Handle closing of a websocket
	 * @param webSocket 
	 */
	void closeHandler(ServerWebSocket webSocket);

}

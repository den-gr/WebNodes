package strategy.websocket;

import io.vertx.core.http.ServerWebSocket;

public interface WebSocketStrategy {
	
	void textMessageHandler(String message, ServerWebSocket webSocket);
	
	void closeHandler(ServerWebSocket webSocket);

}

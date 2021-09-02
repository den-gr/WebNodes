package strategy;

import io.vertx.core.http.ServerWebSocket;

public interface WebSocketStrategy {
	
	void TextMessageHandler(String message, ServerWebSocket webSocket);
	
	void CloseHandler(ServerWebSocket webSocket);

}

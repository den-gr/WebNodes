package main;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import strategy.websocket.WebSocketStrategy;
import strategy.websocket.WebSocketStrategyImpl;

public class Server extends AbstractVerticle {
	private final static String PATH = "res/webroot/";
	private final int port;
		
	public Server(int port) {
		this.port = port;
	}
	
	@Override 
	public void start() {
		startServer(vertx);
	}
	
	private void startServer(Vertx vertx) {
		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());
		router.route("/static/*").handler(StaticHandler.create(PATH));
		HttpServer server = vertx.createHttpServer();
		
		WebSocketStrategy strategy = new WebSocketStrategyImpl(vertx.eventBus());
		
		server.webSocketHandler(webSocket -> {
			System.out.println("A client is connected with handlerID: " + webSocket.textHandlerID());
			
			webSocket.textMessageHandler(message -> strategy.textMessageHandler(message, webSocket));
			webSocket.closeHandler(message -> strategy.closeHandler(webSocket));
		});
			
		server.requestHandler(router).listen(port);
	}
}

package be.ugent.idlab.divide.monitor.local.monitoring.rsp.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@ClientEndpoint
public class WebSocketClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketClient.class.getName());

    public static WebSocketClient initialize(URI serverURI) {
        return new WebSocketClient(serverURI);
    }


    private Session session;

    private final URI serverURI;

    private final List<MessageHandler> messageHandlers;

    private WebSocketClient(URI serverURI) {
        this.serverURI = serverURI;
        this.messageHandlers = new ArrayList<>();

        // connect to WebSocket server
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        connect(container);
    }

    @SuppressWarnings("resource")
    private void connect(WebSocketContainer container) {
        boolean connected = false;
        while (!connected) {
            try {
                container.connectToServer(this, serverURI);
                LOGGER.info("Successfully connected to WebSocket server {}", serverURI.toString());

                connected = true;

            } catch (Exception e) {
                LOGGER.warn("Cannot connect to WebSocket server {} - retrying in 1 second",
                        serverURI.toString());
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void addMessageHandler(MessageHandler messageHandler) {
        this.messageHandlers.add(messageHandler);
    }

    @SuppressWarnings("unused")
    public void removeMessageHandler(MessageHandler messageHandler) {
        this.messageHandlers.remove(messageHandler);
    }

    @OnOpen
    public void onOpen(Session session) {
        LOGGER.info("Session opened");
        this.session = session;
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        LOGGER.warn("Connection to WebSocket server {} closed unexpectedly - " +
                "trying to reconnect in 1 second", serverURI.toString());

        // session is no longer known
        this.session = null;

        // wait 1 second
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }

        // try to reconnect
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        connect(container);
    }

    @OnMessage
    public void onMessage(String message) {
        LOGGER.trace("Message received: {}", message);

        // forward incoming messages from server to message handlers
        for (MessageHandler handler : messageHandlers) {
            handler.handleMessage(message);
        }
    }

    public Session getSession() {
        return session;
    }

}

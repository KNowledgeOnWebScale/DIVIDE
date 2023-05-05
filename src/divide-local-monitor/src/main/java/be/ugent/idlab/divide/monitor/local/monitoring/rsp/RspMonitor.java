package be.ugent.idlab.divide.monitor.local.monitoring.rsp;

import be.ugent.idlab.divide.monitor.local.LocalMonitorException;
import be.ugent.idlab.divide.monitor.local.monitoring.MonitoringService;
import be.ugent.idlab.divide.monitor.local.monitoring.rsp.ws.MessageHandler;
import be.ugent.idlab.divide.monitor.local.monitoring.rsp.ws.WebSocketClient;
import be.ugent.idlab.rspservice.common.interfaces.RDFStream;
import be.ugent.idlab.rspservice.common.interfaces.RDFStreamProcessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RSP monitor class which is a WebSocket client to the WebSocket server exposed by
 * the RSP engine wrapper. Any monitoring events from the RSP engine are sent by that
 * WebSocket server to all clients like this RSP monitor.
 */
class RspMonitor extends MonitoringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RspMonitor.class.getName());

    private WebSocketClient webSocketClient;

    private final int rspEngineMonitorWebSocketPort;

    RspMonitor(RDFStreamProcessor rdfStreamProcessor,
               RDFStream localMonitorStream,
               String componentId,
               String deviceId,
               int rspEngineMonitorWebSocketPort) {
        super(rdfStreamProcessor, localMonitorStream, componentId, deviceId);
        this.rspEngineMonitorWebSocketPort = rspEngineMonitorWebSocketPort;
    }

    @Override
    public void start() throws LocalMonitorException {
        // construct WebSocket url
        String webSocketUrl = String.format("ws://localhost:%d", this.rspEngineMonitorWebSocketPort);

        try {
            LOGGER.info("Starting RSP monitor: creating client to WebSocket server at url {}",
                    webSocketUrl);

            // initialize WebSocket client
            this.webSocketClient = WebSocketClient.initialize(new URI(webSocketUrl));

            // forward monitoring events received as WebSocket messages to output handler
            this.webSocketClient.addMessageHandler(new RspMonitorOutputHandler());

        } catch (URISyntaxException e) {
            String message = "RSP monitor could not be started due to problem with initializing" +
                    " WebSocket client connection to " + webSocketUrl;
            // not logged since LocalMonitor exception handler will report exception
            throw new LocalMonitorException(message, e);
        }
    }

    @Override
    public void reset() {
        // try to close active WebSocket session (but ignore any errors that may be caused by
        // fact that session was already closed by the system)
        try {
            this.webSocketClient.getSession().close();
        } catch (Exception ignored) {
        }
    }

    private class RspMonitorOutputHandler implements MessageHandler {

        private final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

        @Override
        public void handleMessage(String message) {
            try {
                // parse JSON message as a generic JSON object
                JsonObject jsonMessage = new JsonParser().parse(message).getAsJsonObject();

                // create empty array of events
                List<RspMonitorEvent> events = new ArrayList<>();

                // retrieve type of event
                String type = jsonMessage.get("type").getAsString();
                if ("query_execution".equals(type)) {
                    long timestamp = jsonMessage.get("timestamp").getAsLong();
                    String queryId = jsonMessage.get("queryId").getAsString();

                    float memoryUsageMb = jsonMessage.get("memoryUsageMb").getAsFloat();
                    float memoryUsageBytes = memoryUsageMb * 1048576;
                    events.add(new RspMonitorEvent(
                            timestamp, "rsp_query_execution_memory_usage", memoryUsageBytes, "byte", queryId));

                    float executionTimeMs = jsonMessage.get("executionTimeMs").getAsFloat();
                    float executionTimeSeconds = executionTimeMs / 1000.0f;
                    events.add(new RspMonitorEvent(
                            timestamp, "rsp_query_execution_time", executionTimeSeconds, "second", queryId));

                    float processingTimeMs = jsonMessage.get("processingTimeMs").getAsFloat();
                    float processingTimeSeconds = processingTimeMs / 1000.0f;
                    events.add(new RspMonitorEvent(
                            timestamp, "rsp_query_processing_time", processingTimeSeconds, "second", queryId));

                    int numberOfHits = jsonMessage.get("numberOfHits").getAsInt();
                    events.add(new RspMonitorEvent(
                            timestamp, "rsp_query_execution_hits", numberOfHits, "number", queryId));

                } else if ("stream_event".equals(type)) {
                    long timestamp = jsonMessage.get("timestamp").getAsLong();
                    String streamId = jsonMessage.get("streamId").getAsString();

                    int numberOfTriples = jsonMessage.get("numberOfTriples").getAsInt();
                    events.add(new RspMonitorEvent(
                            timestamp, "rsp_stream_event_triples", numberOfTriples, "number", streamId));
                }

                // create string of JSON array with all events
                String processedJson = String.format("[ %s ]",
                        events.stream().map(GSON::toJson).collect(Collectors.joining(",")));

                // forward processed JSON events to general monitor output handler
                handleMonitoringOutput(processedJson);

            } catch (Exception e) {
                LOGGER.warn("Could not correctly parse message received from RSP engine monitor", e);
            }
        }

    }

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private static class RspMonitorEvent {

        private final long time;
        private final String metric;
        private final float value;
        private final String unit;
        private final String featureOfInterestId;

        public RspMonitorEvent(long time, String metric, float value, String unit) {
            this.time = time;
            this.metric = metric;
            this.value = value;
            this.unit = unit;
            this.featureOfInterestId = null;
        }

        public RspMonitorEvent(long time, String metric, float value, String unit, String featureOfInterestId) {
            this.time = time;
            this.metric = metric;
            this.value = value;
            this.unit = unit;
            this.featureOfInterestId = featureOfInterestId;
        }

    }

}

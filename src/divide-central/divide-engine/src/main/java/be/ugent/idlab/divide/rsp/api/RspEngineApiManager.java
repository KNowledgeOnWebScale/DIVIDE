package be.ugent.idlab.divide.rsp.api;

import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;
import be.ugent.idlab.divide.rsp.engine.IRspEngine;
import be.ugent.idlab.util.http.HttpResponse;
import be.ugent.idlab.util.http.HttpUtilities;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.HttpHostConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

class RspEngineApiManager implements IRspEngineApiManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RspEngineApiManager.class.getName());

    private final String queriesUrl;
    private final String streamsUrl;

    RspEngineApiManager(IRspEngine rspEngine) throws DivideInvalidInputException {
        // first try to convert the base URL string to a URL and URI object
        // -> is required to perform the registration of queries and streams status
        //    changes to (a subpath of) this URL later on
        // -> if this fails, this means that this URL string is invalid
        try {
            (new URL(rspEngine.getBaseUrl())).toURI();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new DivideInvalidInputException(
                    "RSP engine URL is invalid");
        }

        // correctly configure all URLs used by this API manager
        String baseUrl = rspEngine.getBaseUrl();
        this.queriesUrl = String.format("%s/queries", baseUrl);
        this.streamsUrl = String.format("%s/streams", baseUrl);
    }

    @Override
    public void unregisterQuery(String queryName) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException {
        String url = String.format("%s/%s", this.queriesUrl, queryName);

        LOGGER.info("Unregistering query with name '" + queryName + "' at " + url);

        try {
            HttpResponse httpResponse = HttpUtilities.delete(url);

            int statusCode = httpResponse.getStatusCode();
            if (statusCode >= 300) {
                throw new RspEngineApiResponseException(String.format("RSP engine server " +
                                "responded with status code %d and error message: %s",
                        statusCode, httpResponse.getBody()));
            }

        } catch (HttpHostConnectException | ClientProtocolException e) {
            String description = String.format("Could not unregister query at %s because " +
                    "of connection issue", url);
            LOGGER.error(description, e);
            throw new RspEngineApiNetworkException(description, e);

        } catch (URISyntaxException | MalformedURLException e) {
            // this can normally not happen, since the URI is input-validated upon the
            // creation of this object
            String description = String.format("Could not unregister query at %s because " +
                    "this URL is invalid", url);
            LOGGER.error(description, e);
            throw new DivideInvalidInputException(description, e);

        } catch (IOException e) {
            String description = String.format("Could not unregister query at %s", url);
            LOGGER.error(description, e);
            throw new RspEngineApiNetworkException(description, e);
        }
    }

    @Override
    public JsonObject registerQuery(String queryName, String queryBody) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            RspEngineApiInputException,
            DivideInvalidInputException {
        String url = String.format("%s/%s", this.queriesUrl, queryName);

        LOGGER.info("Registering query with name '" + queryName + "' at " + url);

        try {
            HttpResponse httpResponse = HttpUtilities.put(url, queryBody);

            int statusCode = httpResponse.getStatusCode();
            if (statusCode >= 300) {
                throw new RspEngineApiResponseException(String.format("RSP engine server " +
                                "responded with status code %d and error message: %s",
                        statusCode, httpResponse.getBody()));
            }
            try {
                return new JsonParser().parse(httpResponse.getBody()).getAsJsonObject();
            } catch (Exception e) {
                return null;
            }

        } catch (HttpHostConnectException | ClientProtocolException e) {
            String description = String.format("Could not register query to %s because " +
                    "of connection issue", url);
            LOGGER.error(description, e);
            throw new RspEngineApiNetworkException(description, e);

        } catch (UnsupportedEncodingException e) {
            String description = String.format("Could not register query to %s because " +
                    "HTTP request body (= query body) is invalid", url);
            LOGGER.error(description, e);
            throw new RspEngineApiInputException(description, e);

        } catch (URISyntaxException | MalformedURLException e) {
            // this can normally not happen, since the URI is input-validated upon the
            // creation of this object
            String description = String.format("Could not register query to %s because " +
                    "this URL is invalid", url);
            LOGGER.error(description, e);
            throw new DivideInvalidInputException(description, e);

        } catch (IOException e) {
            String description = String.format("Could not register query to %s", url);
            LOGGER.error(description, e);
            throw new RspEngineApiNetworkException(description, e);
        }
    }

    @Override
    public List<JsonObject> getQueryObservers(String queryName) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException {
        String url = String.format("%s/%s/observers", this.queriesUrl, queryName);

        LOGGER.info("Retrieving observers of query with name '{}' at {}", queryName, url);

        try {
            HttpResponse httpResponse = HttpUtilities.get(url);

            int statusCode = httpResponse.getStatusCode();
            if (statusCode >= 300) {
                throw new RspEngineApiResponseException(String.format("RSP engine server " +
                                "responded with status code %d and error message: %s",
                        statusCode, httpResponse.getBody()));
            }

            // parse response body
            List<JsonObject> observers = new ArrayList<>();
            JsonArray jsonArray = new JsonParser().parse(
                    httpResponse.getBody()).getAsJsonArray();
            for (JsonElement jsonElement : jsonArray) {
                observers.add(jsonElement.getAsJsonObject());
            }
            return observers;

        } catch (HttpHostConnectException | ClientProtocolException e) {
            String description = String.format("Could not retrieve observers of query %s at %s because " +
                    "of connection issue", queryName, url);
            LOGGER.error(description, e);
            throw new RspEngineApiNetworkException(description, e);

        } catch (URISyntaxException | MalformedURLException e) {
            // this can normally not happen, since the URI is input-validated upon the
            // creation of this object
            String description = String.format("Could not retrieve observers of query %s at %s because " +
                    "this URL is invalid", queryName, url);
            LOGGER.error(description, e);
            throw new DivideInvalidInputException(description, e);

        } catch (IOException e) {
            String description = String.format("Could not retrieve observers of query %s at %s",
                    queryName, url);
            LOGGER.error(description, e);
            throw new RspEngineApiNetworkException(description, e);
        }
    }

    @Override
    public void registerQueryObserver(String queryName, String observerUrl) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException {
        String url = String.format("%s/%s/observers", this.queriesUrl, queryName);

        LOGGER.info("Registering observer '{}' of query with name '{}' at {}",
                observerUrl, queryName, url);

        try {
            HttpResponse httpResponse = HttpUtilities.put(url, observerUrl);

            int statusCode = httpResponse.getStatusCode();
            if (statusCode >= 300) {
                throw new RspEngineApiResponseException(String.format("RSP engine server " +
                                "responded with status code %d and error message: %s",
                        statusCode, httpResponse.getBody()));
            }

        } catch (HttpHostConnectException | ClientProtocolException e) {
            String description = String.format("Could not register observer %s of query %s at %s because " +
                    "of connection issue", observerUrl, queryName, url);
            LOGGER.error(description, e);
            throw new RspEngineApiNetworkException(description, e);

        } catch (URISyntaxException | MalformedURLException e) {
            // this can normally not happen, since the URI is input-validated upon the
            // creation of this object
            String description = String.format("Could not register observer %s of query %s at %s because " +
                    "this URL is invalid", observerUrl, queryName, url);
            LOGGER.error(description, e);
            throw new DivideInvalidInputException(description, e);

        } catch (IOException e) {
            String description = String.format("Could not register observer %s of query %s at %s",
                    observerUrl, queryName, url);
            LOGGER.error(description, e);
            throw new RspEngineApiNetworkException(description, e);
        }
    }

    @Override
    public void pauseRspEngineStreams() throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException {
        LOGGER.info("Pausing streams at " + streamsUrl);
        updateRspEngineStreamsStatus("pause");
    }

    @Override
    public void restartRspEngineStreams() throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException {
        LOGGER.info("Restarting streams at " + streamsUrl);
        updateRspEngineStreamsStatus("restart");
    }

    private void updateRspEngineStreamsStatus(String action) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException {
        try {
            HttpResponse httpResponse = HttpUtilities.post(streamsUrl, "action=" + action);

            int statusCode = httpResponse.getStatusCode();
            if (statusCode >= 300) {
                throw new RspEngineApiResponseException(String.format("RSP engine server " +
                                "responded with status code %d and error message: %s",
                        statusCode, httpResponse.getBody()));
            }

        } catch (HttpHostConnectException | ClientProtocolException e) {
            String description = String.format("Could not %s streams at %s because " +
                    "of connection issue", action, streamsUrl);
            LOGGER.error(description, e);
            throw new RspEngineApiNetworkException(description, e);

        } catch (URISyntaxException | MalformedURLException e) {
            // this can normally not happen, since the URI is input-validated upon the
            // creation of this object
            String description = String.format("Could not %s streams at %s because " +
                    "this URL is invalid", action, streamsUrl);
            LOGGER.error(description, e);
            throw new DivideInvalidInputException(description, e);

        } catch (IOException e) {
            String description = String.format("Could not %s streams at %s", action, streamsUrl);
            LOGGER.error(description, e);
            throw new RspEngineApiNetworkException(description, e);
        }
    }

    @Override
    public void registerStream(String streamName,
                               boolean ignoreAlreadyExists) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException {
        String url;
        try {
            url = String.format("%s/%s", this.streamsUrl,
                    URLEncoder.encode(streamName, StandardCharsets.UTF_8.toString()));
        } catch (UnsupportedEncodingException e) {
            throw new DivideInvalidInputException(
                    "Stream name " + streamName + " cannot be URL encoded", e);
        }

        LOGGER.info("Registering stream with name '{}' at {}", streamName, url);

        try {
            HttpResponse httpResponse = HttpUtilities.put(url);

            int statusCode = httpResponse.getStatusCode();
            if (statusCode >= 300) {
                // only throw exception on a 400 response code if the 'ignoreAlreadyExists'
                // is set to false
                if (statusCode != 400 || !ignoreAlreadyExists) {
                    throw new RspEngineApiResponseException(String.format("RSP engine server " +
                                    "responded with status code %d and error message: %s",
                            statusCode, httpResponse.getBody()));
                }
            }

        } catch (HttpHostConnectException | ClientProtocolException e) {
            String description = String.format("Could not register stream %s at %s because " +
                    "of connection issue", streamName, url);
            LOGGER.error(description, e);
            throw new RspEngineApiNetworkException(description, e);

        } catch (URISyntaxException | MalformedURLException e) {
            // this can normally not happen, since the URI is input-validated upon the
            // creation of this object
            String description = String.format("Could not register stream %s at %s because " +
                    "this URL is invalid", streamName, url);
            LOGGER.error(description, e);
            throw new DivideInvalidInputException(description, e);

        } catch (IOException e) {
            String description = String.format("Could not register stream %s at %s",
                    streamName, url);
            LOGGER.error(description, e);
            throw new RspEngineApiNetworkException(description, e);
        }
    }

    @Override
    public List<JsonObject> retrieveQueriesWithInputStream(String streamName) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException {
        String url;
        try {
            url = String.format("%s/%s/queries", this.streamsUrl,
                    URLEncoder.encode(streamName, StandardCharsets.UTF_8.toString()));
        } catch (UnsupportedEncodingException e) {
            throw new DivideInvalidInputException(
                    "Stream name " + streamName + " cannot be URL encoded", e);
        }

        LOGGER.info("Retrieving queries with input stream '{}' at {}", streamName, url);

        try {
            HttpResponse httpResponse = HttpUtilities.get(url);

            int statusCode = httpResponse.getStatusCode();
            if (statusCode >= 300) {
                throw new RspEngineApiResponseException(String.format("RSP engine server " +
                                "responded with status code %d and error message: %s",
                        statusCode, httpResponse.getBody()));
            }

            // parse response body
            List<JsonObject> queryBodies = new ArrayList<>();
            JsonArray jsonArray = new JsonParser().parse(
                    httpResponse.getBody()).getAsJsonArray();
            for (JsonElement jsonElement : jsonArray) {
                queryBodies.add(jsonElement.getAsJsonObject());
            }
            return queryBodies;

        } catch (HttpHostConnectException | ClientProtocolException e) {
            String description = String.format("Could not retrieve queries with input stream %s" +
                    " at %s because of connection issue", streamName, url);
            LOGGER.error(description, e);
            throw new RspEngineApiNetworkException(description, e);

        } catch (URISyntaxException | MalformedURLException e) {
            // this can normally not happen, since the URI is input-validated upon the
            // creation of this object
            String description = String.format("Could not retrieve queries with input stream %s " +
                    "at %s because this URL is invalid", streamName, url);
            LOGGER.error(description, e);
            throw new DivideInvalidInputException(description, e);

        } catch (IOException e) {
            String description = String.format("Could not retrieve queries with input stream %s at %s",
                    streamName, url);
            LOGGER.error(description, e);
            throw new RspEngineApiNetworkException(description, e);
        }
    }

    @Override
    public void enableStreamForwardingToWebSocket(String streamName,
                                                  String webSocketUrl) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException {
        updateStreamForwardingToWebSocket(streamName, webSocketUrl, true);
    }

    @Override
    public void disableStreamForwardingToWebSocket(String streamName,
                                                   String webSocketUrl) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException {
        updateStreamForwardingToWebSocket(streamName, webSocketUrl, false);
    }

    private void updateStreamForwardingToWebSocket(String streamName,
                                                   String webSocketUrl,
                                                   boolean enable) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException {
        String url;
        try {
            url = String.format("%s/%s/forwarders/%s", this.streamsUrl,
                    URLEncoder.encode(streamName, StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(webSocketUrl, StandardCharsets.UTF_8.toString()));
        } catch (UnsupportedEncodingException e) {
            throw new DivideInvalidInputException(
                    "Stream name " + streamName + " and/or WebSocket URL " +
                            webSocketUrl + " cannot be URL encoded", e);
        }

        LOGGER.info("{} stream forwarding for stream with name '{}' " +
                        "to WebSocket URL {} at {}",
                enable ? "Enabling" : "Disabling", streamName, webSocketUrl, url);

        try {
            HttpResponse httpResponse;
            if (enable) {
                httpResponse = HttpUtilities.post(url);
            } else {
                httpResponse = HttpUtilities.delete(url);
            }

            int statusCode = httpResponse.getStatusCode();
            if (statusCode >= 300) {
                throw new RspEngineApiResponseException(String.format("RSP engine server " +
                                "responded with status code %d and error message: %s",
                        statusCode, httpResponse.getBody()));
            }

        } catch (HttpHostConnectException | ClientProtocolException e) {
            String description = String.format("Could not %s stream forwarding for stream " +
                    "with name %s to WebSocket URL %s at %s because " +
                    "of connection issue", enable ? "enable" : "disable",
                    streamName, webSocketUrl, url);
            LOGGER.error(description, e);
            throw new RspEngineApiNetworkException(description, e);

        } catch (URISyntaxException | MalformedURLException e) {
            // this can normally not happen, since the URI is input-validated upon the
            // creation of this object
            String description = String.format("Could not %s stream forwarding for stream " +
                    "with name %s to WebSocket URL %s at %s because " +
                    "this URL is invalid", enable ? "enable" : "disable",
                    streamName, webSocketUrl, url);
            LOGGER.error(description, e);
            throw new DivideInvalidInputException(description, e);

        } catch (IOException e) {
            String description = String.format("Could not %s stream forwarding for stream " +
                    "with name %s to WebSocket URL %s at %s", enable ? "enable" : "disable",
                    streamName, webSocketUrl, url);
            LOGGER.error(description, e);
            throw new RspEngineApiNetworkException(description, e);
        }
    }

}

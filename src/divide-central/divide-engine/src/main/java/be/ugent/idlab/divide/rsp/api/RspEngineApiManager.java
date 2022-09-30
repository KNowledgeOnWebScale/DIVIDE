package be.ugent.idlab.divide.rsp.api;

import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;
import be.ugent.idlab.divide.rsp.engine.IRspEngine;
import be.ugent.idlab.divide.rsp.query.IRspQuery;
import be.ugent.idlab.util.http.HttpResponse;
import be.ugent.idlab.util.http.HttpUtilities;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.HttpHostConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

class RspEngineApiManager implements IRspEngineApiManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RspEngineApiManager.class.getName());

    private final String registrationUrl;
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

        this.registrationUrl = rspEngine.getRegistrationUrl();
        this.streamsUrl = rspEngine.getStreamsUrl();
    }

    @Override
    public void unregisterQuery(IRspQuery query) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException {
        String url = String.format("%s/%s", this.registrationUrl, query.getQueryName());

        LOGGER.info("Unregistering query with name '" + query.getQueryName() + "' at " + url);

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
    public void registerQuery(IRspQuery query) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            RspEngineApiInputException,
            DivideInvalidInputException {
        String url = String.format("%s/%s", this.registrationUrl, query.getQueryName());

        LOGGER.info("Registering query with name '" + query.getQueryName() + "' at " + url);

        try {
            HttpResponse httpResponse = HttpUtilities.put(url, query.getQueryBody());

            int statusCode = httpResponse.getStatusCode();
            if (statusCode >= 300) {
                throw new RspEngineApiResponseException(String.format("RSP engine server " +
                                "responded with status code %d and error message: %s",
                        statusCode, httpResponse.getBody()));
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

}

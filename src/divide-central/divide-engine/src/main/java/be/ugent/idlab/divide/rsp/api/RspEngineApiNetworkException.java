package be.ugent.idlab.divide.rsp.api;

@SuppressWarnings("unused")
public class RspEngineApiNetworkException extends RspEngineApiException {

    public RspEngineApiNetworkException(String description, Exception base) {
        super(description, base);
    }

    public RspEngineApiNetworkException(String description) {
        super(description);
    }

    public RspEngineApiNetworkException(Exception base) {
        super(base);
    }

}

package be.ugent.idlab.divide.rsp.api;

@SuppressWarnings("unused")
public abstract class RspEngineApiException extends Exception {

    public RspEngineApiException(String description, Exception base) {
        super(description, base);
    }

    public RspEngineApiException(String description) {
        super(description);
    }

    public RspEngineApiException(Exception base) {
        super(base);
    }

}

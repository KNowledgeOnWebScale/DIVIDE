package be.ugent.idlab.divide.rsp.api;

@SuppressWarnings("unused")
public class RspEngineApiResponseException extends RspEngineApiException {

    public RspEngineApiResponseException(String description, Exception base) {
        super(description, base);
    }

    public RspEngineApiResponseException(String description) {
        super(description);
    }

    public RspEngineApiResponseException(Exception base) {
        super(base);
    }

}

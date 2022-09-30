package be.ugent.idlab.divide.rsp.api;

@SuppressWarnings("unused")
public class RspEngineApiInputException extends RspEngineApiException {

    public RspEngineApiInputException(String description, Exception base) {
        super(description, base);
    }

    public RspEngineApiInputException(String description) {
        super(description);
    }

    public RspEngineApiInputException(Exception base) {
        super(base);
    }

}

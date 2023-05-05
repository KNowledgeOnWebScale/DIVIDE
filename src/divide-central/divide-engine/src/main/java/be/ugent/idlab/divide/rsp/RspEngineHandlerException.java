package be.ugent.idlab.divide.rsp;

@SuppressWarnings("unused")
public class RspEngineHandlerException extends Exception {

    public RspEngineHandlerException(String description, Exception base) {
        super(description, base);
    }

    public RspEngineHandlerException(String description) {
        super(description);
    }

    public RspEngineHandlerException(Exception base) {
        super(base);
    }

}

package be.ugent.idlab.divide.rsp.query.window;

@SuppressWarnings("unused")
class StreamWindowParsingException extends Exception {

    StreamWindowParsingException(String description, Exception base) {
        super(description, base);
    }

    StreamWindowParsingException(String description) {
        super(description);
    }

    StreamWindowParsingException(Exception base) {
        super(base);
    }

}

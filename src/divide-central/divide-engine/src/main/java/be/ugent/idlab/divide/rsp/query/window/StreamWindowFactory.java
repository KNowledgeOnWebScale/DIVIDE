package be.ugent.idlab.divide.rsp.query.window;

public class StreamWindowFactory {

    public static IStreamWindow createInstance(String streamIri,
                                           String windowDefinition) {
        return new StreamWindow(streamIri, windowDefinition);
    }

}

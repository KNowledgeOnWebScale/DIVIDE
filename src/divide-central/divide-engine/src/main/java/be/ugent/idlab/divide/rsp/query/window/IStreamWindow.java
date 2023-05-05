package be.ugent.idlab.divide.rsp.query.window;

public interface IStreamWindow {

    String getStreamIri();

    String getWindowDefinition();

    int getQuerySlidingStepInSeconds();

    int getWindowSizeInSeconds();

    int getWindowStartInSecondsAgo();

    int getWindowEndInSecondsAgo();

}

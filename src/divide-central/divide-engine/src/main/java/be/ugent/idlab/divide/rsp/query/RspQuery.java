package be.ugent.idlab.divide.rsp.query;

import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import be.ugent.idlab.divide.rsp.engine.IRspEngine;
import be.ugent.idlab.divide.rsp.query.window.IStreamWindow;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

public class RspQuery implements IRspQuery {

    private final String queryName;
    private final String queryBody;
    private final String rspQLQueryBody;
    private final IDivideQuery divideQuery;

    private String id;
    private List<IStreamWindow> streamWindows;
    private IRspEngine rspEngine;
    private int querySlidingStepInSeconds;
    private int streamWindowSizeInSeconds;
    private IComponent associatedComponent;

    public RspQuery(String queryName,
                    String queryBody,
                    String rspQLQueryBody,
                    IDivideQuery divideQuery) {
        this.queryName = queryName;
        this.queryBody = queryBody;
        this.rspQLQueryBody = rspQLQueryBody;
        this.divideQuery = divideQuery;
    }

    @Override
    public String getQueryName() {
        return queryName;
    }

    @Override
    public String getQueryBody() {
        return queryBody;
    }

    @Override
    public String getRspQLQueryBody() {
        return rspQLQueryBody;
    }

    @Override
    public IDivideQuery getOriginalDivideQuery() {
        return divideQuery;
    }

    // IMPORTANT: equality of RSP queries is defined by their RSP-QL body,
    //            and NOT by their name or translated body (which might
    //            contain the name)!
    //            (since body comparison is done to determine whether
    //             a query is already registered on an RSP engine or not)
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RspQuery rspQuery = (RspQuery) o;
        return rspQLQueryBody.equals(rspQuery.rspQLQueryBody);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queryBody);
    }

    @Override
    public void updateAfterRegistration(String id,
                                        List<IStreamWindow> streamWindows,
                                        IRspEngine rspEngine,
                                        IComponent associatedComponent) {
        this.id = id;
        this.streamWindows = streamWindows;
        this.rspEngine = rspEngine;
        this.associatedComponent = associatedComponent;

        // extract query sliding step from stream windows
        // -> currently checks for the stream window with the lowest query sliding step
        //    (no filter because this value should always be defined and be positive
        OptionalInt minStreamWindowSlidingStep = streamWindows.stream()
                .mapToInt(IStreamWindow::getQuerySlidingStepInSeconds).min();
        if (minStreamWindowSlidingStep.isPresent()) {
            this.querySlidingStepInSeconds = minStreamWindowSlidingStep.getAsInt();
        } else {
            this.querySlidingStepInSeconds = -1;
        }

        // extract stream window size from stream windows
        // -> currently only fills in the stream window size if the query has
        //    exactly 1 stream window (otherwise it is -1)
        // -> will be -1 as well if this stream window has no window size
        //    (happens if it is defined with a FROM-TO)
        if (streamWindows.size() == 1) {
            this.streamWindowSizeInSeconds = streamWindows.get(0).getWindowSizeInSeconds();
        } else {
            this.streamWindowSizeInSeconds = -1;
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<IStreamWindow> getStreamWindows() {
        return streamWindows;
    }

    @Override
    public IRspEngine getRspEngine() {
        return rspEngine;
    }

    @Override
    public int getQuerySlidingStepInSeconds() {
        return querySlidingStepInSeconds;
    }

    @Override
    public int getStreamWindowSizeInSeconds() {
        return streamWindowSizeInSeconds;
    }

    @Override
    public IComponent getAssociatedComponent() {
        return associatedComponent;
    }

    @Override
    public String toString() {
        return "RspQuery{" +
                "queryName='" + queryName + '\'' +
                ", queryBody='" + queryBody + '\'' +
                ", rspQLQueryBody='" + rspQLQueryBody + '\'' +
                ", divideQuery=" + divideQuery +
                ", id='" + id + '\'' +
                ", streamWindows=" + streamWindows +
                ", rspEngine=" + rspEngine +
                ", querySlidingStepInSeconds=" + querySlidingStepInSeconds +
                ", streamWindowSizeInSeconds=" + streamWindowSizeInSeconds +
                ", associatedComponent=" + associatedComponent +
                '}';
    }

}

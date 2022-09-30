package be.ugent.idlab.divide.core.engine;

public interface IDivideQueryUpdateTask {

    /**
     * Execute this query update task.
     *
     * @return true if the update thread on which this task is running was
     *              interrupted during the execution of this task, false otherwise
     */
    boolean execute();

}

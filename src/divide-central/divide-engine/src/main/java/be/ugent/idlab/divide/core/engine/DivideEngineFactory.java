package be.ugent.idlab.divide.core.engine;

public class DivideEngineFactory {

    /**
     * Create and return a new DIVIDE engine.
     *
     * @return newly created DIVIDE engine
     */
    public static synchronized IDivideEngine createInstance() {
        return new DivideEngine();
    }

}

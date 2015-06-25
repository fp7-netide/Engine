package eu.netide.core.api;

/**
 * Created by timvi on 25.06.2015.
 */
public final class NetworkApplicationInfo {
    private String _name;

    public String GetName() {
        return _name;
    }

    public NetworkApplicationInfo(String name) {
        _name = name;
    }
}

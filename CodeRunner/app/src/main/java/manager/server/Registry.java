package manager.server;

import java.util.ArrayList;
import java.util.List;

public class Registry {

    private final List<Endpoint> servers = new ArrayList<>();
    private int next = 0;

    public synchronized void add(Endpoint endpoint) {
        if (!servers.contains(endpoint)) {
            servers.add(endpoint);
        }
    }

    public synchronized Endpoint pick() {
        if (servers.isEmpty()) {
            return null;
        }

        Endpoint endpoint = servers.get(next % servers.size());
        next = (next + 1) % servers.size();

        return endpoint;
    }
}
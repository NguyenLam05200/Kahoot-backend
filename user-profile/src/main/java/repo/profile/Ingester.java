package repo.profile;

import io.vertx.core.Future;

public interface Ingester {
    Future<Integer> In();
    void Out();
}

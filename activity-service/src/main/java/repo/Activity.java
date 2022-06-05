package repo;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;

public interface Activity {
    Observable<RowSet<Row>> write(String deviceId, String deviceSync, Integer counter);
    Single<Integer> countStepsInHours(String deviceId, int hours);
}

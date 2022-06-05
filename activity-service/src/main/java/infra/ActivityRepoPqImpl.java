package infra;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;
import io.vertx.reactivex.sqlclient.Tuple;
import repo.Activity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ActivityRepoPqImpl implements Activity {
  private final PgPool pool;
  private final String createActivitySQL =
      "INSERT INTO activities(device_id, device_sync, created_on, steps) VALUES($1, $2, current_timestamp, $3);";
  private final String countStepsInHoursSQL =
      "SELECT sum(steps) as steps from activities where device_id=$1 and created_on >= $2;";
  private static final Logger logger = LoggerFactory.getLogger(ActivityRepoPqImpl.class);

  public ActivityRepoPqImpl(PgPool pool) {
    this.pool = pool;
  }

  @Override
  public Observable<RowSet<Row>> write(String deviceId, String deviceSync, Integer counter) {

    return pool.rxGetConnection()
        .flatMapObservable(
            conn ->
                conn.rxPrepare(createActivitySQL)
                    .flatMapObservable(
                        stmt -> {
                          return stmt.query()
                              .rxExecute(Tuple.of(deviceId, deviceSync, counter))
                              .toObservable();
                        }))
        ;
  }

  @Override
  public Single<Integer> countStepsInHours(String deviceId, int hours) {

    return pool.rxGetConnection()
        .flatMap(
            conn ->
                conn.rxPrepare(countStepsInHoursSQL)
                    .flatMap(
                        stmt ->
                            stmt.query()
                                .rxExecute(
                                    Tuple.of(
                                        deviceId, LocalDateTime.now().minus(hours, ChronoUnit.HOURS)))
                                .map(res -> res.iterator().next().getInteger("steps"))));
  }
}

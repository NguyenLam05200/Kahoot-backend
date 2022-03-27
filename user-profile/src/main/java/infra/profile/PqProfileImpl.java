package infra.profile;

import entity.User;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.mongo.MongoClient;
import repo.profile.UserProfile;

public class PqProfileImpl implements UserProfile {
    private final MongoClient client;

    public PqProfileImpl(MongoClient client) {
        this.client = client;
    }

    @Override
    public Future<Boolean> Existed(String username) {
        JsonObject query = new JsonObject()
                .put("username", username);
        client
            .rxFindOne("users", query, new JsonObject())
                .map(json -> json.getString("user_name"))
                .subscribe( _username -> {
                    System.out.println("user" + _username);
                })
        ;
        return Future.succeededFuture();
    }

    @Override
    public Future<User> Create(String username, String password) {
        return null;
    }

    @Override
    public Future<User> VerifyProfile(String username, String password) {
        return null;
    }
}

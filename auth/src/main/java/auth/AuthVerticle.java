package auth;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.mongo.MongoAuthenticationOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.auth.mongo.MongoAuthentication;
import io.vertx.reactivex.ext.auth.mongo.MongoUserUtil;
import io.vertx.reactivex.ext.mongo.MongoClient;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;

public class AuthVerticle extends AbstractVerticle {
    private MongoUserUtil userUtil;
    private MongoAuthentication mongoAuthProvider;
    private MongoClient mongoClient;
    private static final Logger logger = LoggerFactory.getLogger(AuthVerticle.class);

    private void handleSuccessAuth(RoutingContext rc) {
        logger.info("[] user auth OK");
        rc.response().setStatusCode(200).end();
    }

    private void handleAuthError(RoutingContext rc, Throwable e) {
        logger.error("[] auth error: " + e);
    rc.fail(HttpResponseStatus.UNAUTHORIZED.code());
    }

    private boolean validateBody(JsonObject body) {
        return body.containsKey("username") && body.containsKey("password");
    }

    private void register(RoutingContext rc) {
        JsonObject body = rc.getBodyAsJson();
        JsonObject extraInfo = new JsonObject()
                .put("$set", new JsonObject()
                        .put("email","email")
                        .put("city", "city")
                        .put("deviceId", "device-id")
                        .put("makePublic", true));

        if (!validateBody(body) ) {
            logger.error("invalid body");
      rc.fail(HttpResponseStatus.UNAUTHORIZED.code());
        }

        userUtil
                .rxCreateUser(body.getString("username"), body.getString("password"))
                .flatMapMaybe(id -> insertExtraInfo(extraInfo, id))
                .ignoreElement()
                .subscribe( () -> handleSuccessAuth(rc), err -> handleAuthError(rc, err.getCause()));
    }

    private void authenticate(RoutingContext rc) {
        JsonObject body = rc.getBodyAsJson();
        if (!validateBody(body) ) {
            rc.fail(401);
        }

        mongoAuthProvider
                .rxAuthenticate(body)
                .subscribe(
                        ok -> handleSuccessAuth(rc),
                        err -> handleAuthError(rc, err)
                );
    }

    private MaybeSource<? extends JsonObject> insertExtraInfo(JsonObject extraInfo, String userId) {
        JsonObject query = new JsonObject().put("_id", userId);

        return mongoClient
                .rxFindOneAndUpdate("user", query, extraInfo)
                .onErrorResumeNext(err -> {
                    logger.error("error" + err);
                    return deleteIncompleteUser(query, err);
                    }
                );
    }

    private MaybeSource<? extends JsonObject> deleteIncompleteUser(JsonObject query, Throwable err) {
        if (err.getMessage().contains("E11000")) {
            logger.error("");
            return Maybe.error(new Throwable("E11000"));
        }
        return Maybe.error(err);
    }

    private MaybeSource<? extends JsonObject> getUserInfo(String username) {
        return Maybe.just(new JsonObject());
    }

    private void getUserInfo(RoutingContext rc) {
        String username = rc.pathParam("username");
        if (username.isEmpty()) {
            rc.fail(401);
      return;
        }

        JsonObject fields = new JsonObject()
                .put("_id", 0)
                .put("email",1)
                .put("city", 1)
                .put("deviceId", 1)
                .put("makePublic", 1)
                .put("username", 1);

        JsonObject query = new JsonObject()
                .put("username", username);

        mongoClient.rxFindOne("user", query, fields)
                .toSingle()
                .subscribe(user -> {
                    rc
                            .response()
                            .putHeader("Content-Type", "application/json")
                            .end(user.encode());
                }, err -> {
                    logger.info("looked for not found user: " + username);
                    rc.fail(500);
                });
    }

  private boolean validate(JsonObject body, String... fields) {
        return true;
    }

    private void updateUser(RoutingContext rc) {
        JsonObject body = rc.getBodyAsJson();

        String username = body.getString("username");
        if (username.isEmpty()) {
            rc.fail(401);
        }

        JsonObject query = new JsonObject().put("username", username);

        JsonObject updateUser = new JsonObject();
        if (body.containsKey("deviceId")) {
            updateUser.put("deviceId", body.getString("deviceId"));
        }
        if (body.containsKey("email")) {
            updateUser.put("email", body.getString("email"));
        }
        if (body.containsKey("city")) {
            updateUser.put("city", body.getString("city"));
        }

        mongoClient.rxFindOneAndUpdate("user", query, new JsonObject().put("$set", updateUser))
                .ignoreElement()
                .subscribe(
                        () -> {
                            rc.response().end("update OK");
                        },
                        err -> {
                            logger.error("update failed: " + err);
                            rc.fail(500);
                        }
                );
    }

    private void getUserByDeviceId(RoutingContext rc) {
        String deviceId = rc.pathParam("deviceId");
        JsonObject query = new JsonObject()
                .put("deviceId", deviceId);

        JsonObject fields = new JsonObject()
                .put("_id", 0)
                .put("username", 1)
                .put("email", 1);

        mongoClient
                .rxFindOne("user", query, fields)
                .toSingle()
                .subscribe(user -> {
                    rc
                            .response()
                            .putHeader("Content-Type", "application/json")
                            .end(user.encode());
                }, err -> {
                    logger.info("err: ", err);
                    rc.fail(500);
                });
    }

    @Override
    public Completable rxStart() {
        JsonObject mongoConfig = new JsonObject()
                .put("host", "localhost")
                .put("port", 27017)
                .put("db_name", "profiles");

        mongoClient = MongoClient.createShared(vertx, mongoConfig);
        mongoAuthProvider = MongoAuthentication.create(mongoClient, new MongoAuthenticationOptions());
        userUtil = MongoUserUtil.create(mongoClient);

        BodyHandler bodyHandler = BodyHandler.create();
        HttpServer svr = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.post().handler(bodyHandler);
        router.put().handler(bodyHandler);

        router.post("/register").handler(this::register);
        router.post("/authenticate").handler(this::authenticate);
        router.post("/users").handler(this::authenticate);
        router.get("/users/:username").handler(this::getUserInfo);
    router
        .put("/users")
        //                .handler(authHandler)
        .handler(this::updateUser);
        router.get("/users/own/:deviceId").handler(this::getUserByDeviceId);

        svr.requestHandler(router).listen(3000);
        return super.rxStart();
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.rxDeployVerticle(new AuthVerticle())
                .subscribe(ok -> {
                    logger.info("Deploy Auth Verticle OK");
                }, err -> {
                    logger.error("Deploy Auth Verticle failed: "+  err);
                });
    }
}


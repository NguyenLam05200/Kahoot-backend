package auth;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.mongo.MongoAuthenticationOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.Cookie;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.auth.authentication.AuthenticationProvider;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.auth.mongo.MongoAuthentication;
import io.vertx.reactivex.ext.auth.mongo.MongoUserUtil;
import io.vertx.reactivex.ext.mongo.MongoClient;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class AuthVerticle extends AbstractVerticle {
  public static final int PORT = 8080;
  private static final Logger logger = LoggerFactory.getLogger(AuthVerticle.class);
  private final int jwtExpSecond = 3600; // 1 hour
  private MongoUserUtil userUtil;
  private AuthenticationProvider mongoAuthProvider;
    private MongoClient mongoClient;
  private JWTAuth jwtAuthProvider;

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    JsonObject mongoConfig =
        new JsonObject().put("host", "localhost").put("port", 27017).put("db_name", "profiles");
    MongoClient mongoClient = MongoClient.createShared(vertx, mongoConfig);
    MongoAuthentication mongoAuthentication =
        MongoAuthentication.create(mongoClient, new MongoAuthenticationOptions());
    MongoUserUtil userUtil = MongoUserUtil.create(mongoClient);
    JWTAuth jwtAuthProvider =
        JWTAuth.create(
            vertx,
            new JWTAuthOptions()
                .addPubSecKey(
                    new PubSecKeyOptions().setAlgorithm("HS256").setBuffer("keyboard cat")));

    AuthVerticle authVerticle =
        new AuthVerticle(userUtil, mongoAuthentication, mongoClient, jwtAuthProvider);

    vertx
        .rxDeployVerticle(authVerticle)
        .subscribe(
            ok -> {
              logger.info("Deploy Auth Verticle OK");
            },
            err -> {
              logger.error("Deploy Auth Verticle failed: " + err);
            });
  }

  String toResponseFormat(String e) {
    return new JsonObject().put("error", e).encodePrettily();
  }

    private void handleSuccessAuth(RoutingContext rc) {
        rc.response().setStatusCode(200).end();
    }

    private void handleAuthError(RoutingContext rc, Throwable e) {

    rc.response().setStatusCode(401);
    rc.end(toResponseFormat("Invalid email or password"));
    }

    private boolean validateBody(JsonObject body) {
    return !body.isEmpty() && body.containsKey("email") && body.containsKey("password");
    }

    private void register(RoutingContext rc) {
        JsonObject body = rc.getBodyAsJson();


        if (!validateBody(body) ) {
            logger.error("missing username or password");
            rc.response().end(new JsonObject().put("error", "missing email or password").toString());
            rc.fail(HttpResponseStatus.BAD_REQUEST.code());
            return;
        }
        if (!body.containsKey("name")) {
            logger.error("missing name");
            rc.response().end(new JsonObject().put("error", "missing name").toString());
            rc.fail(HttpResponseStatus.BAD_REQUEST.code());
            return;
        }
        JsonObject extraInfo = new JsonObject()
                .put("$set", new JsonObject()
                        .put("name", body.getString("name"))
                );
    mongoClient.find(
        "user",
        new JsonObject().put("username", body.getString("email")),
        ar -> {
          if (!ar.result().isEmpty()) {
            rc.response().setStatusCode(401);
            rc.end(toResponseFormat("Your email is already exist, please try another email!"));
          } else {
            userUtil
                .rxCreateUser(body.getString("email"), body.getString("password"))
                .flatMapMaybe(id -> insertExtraInfo(extraInfo, id))
                .ignoreElement()
                .subscribe(() -> handleSuccessAuth(rc), err -> handleAuthError(rc, err.getCause()));
          }
        });
    }

    private void authenticate(RoutingContext rc) {
        JsonObject body = rc.getBodyAsJson();
        if (!validateBody(body) ) {
      rc.fail(404);
        }
    body.put("username", body.getString("email"));

    mongoAuthProvider
        .rxAuthenticate(body)
        .map(
            user -> {
              List<String> scopes = new ArrayList<>();
              scopes.add("scope.admin");

              JWTOptions jwtOptions =
                  new JWTOptions()
                      .setExpiresInSeconds(jwtExpSecond)
                      .setSubject(user.get("username"));

              JsonObject claims =
                  new JsonObject()
                      .put("email", body.getString("email"))
                      .put("name", user.principal().getString("name"))
                      .put("extra", new JsonObject())
                      .put("scope", scopes);

              String token = jwtAuthProvider.generateToken(claims, jwtOptions);
              rc.response().addCookie(Cookie.cookie("KAHOOT_JWT", token).setMaxAge(jwtExpSecond));
              user.attributes().put("token", token);
              return user;
            })
        .subscribe(
            user -> {
              rc.end(
                  new JsonObject()
                      .put("token", user.attributes().getString("token"))
                      .encodePrettily());
            },
            err -> handleAuthError(rc, err));
    }


    private MaybeSource<? extends JsonObject> insertExtraInfo(JsonObject extraInfo, String userId) {
        JsonObject query = new JsonObject().put("_id", userId);

        return mongoClient
                .rxFindOneAndUpdate("user", query, extraInfo)
                .onErrorResumeNext(
                        err -> {
                            logger.error("error" + err);
                            return deleteIncompleteUser(query, err);
                        });
    }

  /**
   * deleteIncompleteUser acts like a transaction clean up
   *
   * @param query .
   * @param err .
   * @return .
   */
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
    //        String username = rc.pathParam("username");
    //        if (username.isEmpty()) {
    //            rc.fail(401);
    //      return;
    //        }
    //
    //        JsonObject fields = new JsonObject()
    //                .put("_id", 0)
    //                .put("email",1)
    //                .put("city", 1)
    //                .put("deviceId", 1)
    //                .put("makePublic", 1)
    //                .put("username", 1);
    //
    //        JsonObject query = new JsonObject()
    //                .put("username", username);
    //
    //        mongoClient.rxFindOne("user", query, fields)
    //                .toSingle()
    //                .subscribe(user -> {
    //                    rc
    //                            .response()
    //                            .putHeader("Content-Type", "application/json")
    //                            .end(user.encode());
    //                }, err -> {
    //                    logger.info("looked for not found user: " + username);
    //                    rc.fail(500);
    //                });
  }

  private boolean validate(JsonObject body, String... fields) {
        return true;
    }

  private void updateUser(RoutingContext rc) {
    //        JsonObject body = rc.getBodyAsJson();
    //
    //        String username = body.getString("username");
    //        if (username.isEmpty()) {
    //            rc.fail(401);
    //        }
    //
    //        JsonObject query = new JsonObject().put("username", username);
    //
    //        JsonObject updateUser = new JsonObject();
    //        if (body.containsKey("deviceId")) {
    //            updateUser.put("deviceId", body.getString("deviceId"));
    //        }
    //        if (body.containsKey("email")) {
    //            updateUser.put("email", body.getString("email"));
    //        }
    //        if (body.containsKey("city")) {
    //            updateUser.put("city", body.getString("city"));
    //        }
    //
    //        mongoClient.rxFindOneAndUpdate("user", query, new JsonObject().put("$set",
    // updateUser))
    //                .ignoreElement()
    //                .subscribe(
    //                        () -> {
    //                            rc.response().end("update OK");
    //                        },
    //                        err -> {
    //                            logger.error("update failed: " + err);
    //                            rc.fail(500);
    //                        }
    //                );
  }

  private void getUserByDeviceId(RoutingContext rc) {
    //        String deviceId = rc.pathParam("deviceId");
    //        JsonObject query = new JsonObject()
    //                .put("deviceId", deviceId);
    //
    //        JsonObject fields = new JsonObject()
    //                .put("_id", 0)
    //                .put("username", 1)
    //                .put("email", 1);
    //
    //        mongoClient
    //                .rxFindOne("user", query, fields)
    //                .toSingle()
    //                .subscribe(user -> {
    //                    rc
    //                            .response()
    //                            .putHeader("Content-Type", "application/json")
    //                            .end(user.encode());
    //                }, err -> {
    //                    logger.info("err: ", err);
    //                    rc.fail(500);
    //                });
  }

    @Override
    public Completable rxStart() {
        BodyHandler bodyHandler = BodyHandler.create();
        HttpServer svr = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.post().handler(bodyHandler);
        router.put().handler(bodyHandler);

    router.post("/v1/user/register").handler(this::register);
    router.post("/v1/user/authenticate").handler(this::authenticate);
    svr.requestHandler(router).listen(PORT);
        return super.rxStart();
    }
}


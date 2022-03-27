package api;

import entity.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import repo.profile.UserProfile;

public class ProfileVerticle extends AbstractVerticle {
    private final UserProfile userProfile;
    private final JWTAuth jwtAuthProvider;
    static final Logger logger = LoggerFactory.getLogger(ProfileVerticle.class);

    private static final String CONFIG_HTTP_PORT = "CONFIG_HTTP_PORT";
    private static final String CONFIG_HTTP_API_PREFIX = "CONFIG_HTTP_API_PREFIX";
    private int port;
    private String apiPrefix; // e.g /test



    public ProfileVerticle(UserProfile userProfile, JWTAuth provider) {
        this.userProfile = userProfile;
        this.jwtAuthProvider = provider;
    }

    private void handleLogin(RoutingContext rc) {
        String username = rc.getBodyAsJson().getString("username");
        String password = rc.getBodyAsJson().getString("password");
        if (username == null || password == null) {
            rc.end("Invalid username or password");
        }

        Future<User> user = userProfile.VerifyProfile(username, password);
        user.onComplete(ar -> {
            if (ar.succeeded()) {
                JsonObject claims = new JsonObject()
                        .put("username", username);
                String token = makeJwtToken(username, claims);
                rc.response().putHeader("Content-Type", "application/jwt").end(token);
            } else {
                handleAuthError(rc, ar.cause());
            }
        });
    }

    private void handlerCreateUser(RoutingContext rc) {
        String username = rc.getBodyAsJson().getString("username");
        String password = rc.getBodyAsJson().getString("password");
        if (username == null || password == null) {
            rc.end("Invalid username or password");
        }

        userProfile.Existed(username).onComplete(
                ar-> {
                    if (ar.succeeded()) {
                        if (ar.result()) {
                            rc.end("User existed");
                            return;
                        }
                    }
                }
        );
        Future<User> user = userProfile.Create(username, password);
        user.onComplete(ar -> {
            if (ar.succeeded()) {
                rc.end("OK");
            } else {
                rc.end("Invalid username or password");
            }
        });
    }

    private void handleAuthError(RoutingContext rc, Throwable e) {
        logger.error("auth error: " + e);
        rc.fail(401);
    }


    private String makeJwtToken(String username, JsonObject claims) {

        JWTOptions opts = new JWTOptions()
                .setAlgorithm("RS256")
                .setExpiresInMinutes(10_080)
                .setSubject(username);

        return jwtAuthProvider.generateToken(claims, opts);
    }


    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        super.start(startPromise);
        port = Integer.parseInt(System.getenv(CONFIG_HTTP_PORT));
        apiPrefix = System.getenv(CONFIG_HTTP_API_PREFIX);

        BodyHandler bodyHandler = BodyHandler.create();
        HttpServer svr = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.post().handler(bodyHandler);
        router.put().handler(bodyHandler);

        JWTAuthHandler authHandler = JWTAuthHandler.create(jwtAuthProvider);
        router.get(apiPrefix + "/test").handler(routingContext -> {
            routingContext.end("test page");
        });
        router.get(apiPrefix +"/admin").handler(routingContext -> {
            routingContext.end("test page");
        });

        router.post(apiPrefix +"/api/users/login").handler(this::handleLogin);
        router.post(apiPrefix +"/api/users").handler(this::handlerCreateUser);

        router.get(apiPrefix+"/users/:username").handler(rc -> {
            String username = rc.pathParam("username");
            JsonObject userProfile = new JsonObject()
                    .put("username", username)
                    .put("email", "ngngnhatdinh1110@gmail.com")
                    ;
            rc.end(userProfile.toString());
        });

        router.get(apiPrefix+"/users/owns/:deviceId").handler(rc -> {
            String deviceId = rc.pathParam("deviceId");
            JsonObject userProfile = new JsonObject()
                    .put("username", "dinhnn")
                    .put("deviceId", deviceId)
                    ;
            rc.end(userProfile.toString());
        });

        svr.requestHandler(router).listen(port);
    }
}

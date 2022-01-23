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
    private JWTAuth jwtAuth;
    static final Logger logger = LoggerFactory.getLogger(ProfileVerticle.class);

    private static final String CONFIG_HTTP_PORT = "CONFIG_HTTP_PORT";
    private int port;

    public ProfileVerticle(UserProfile userProfile) {
        this.userProfile = userProfile;
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
                String token = makeJwtToken(username);
                rc.response().putHeader("Content-Type", "application/jwt").end(token);
            } else {
                handleAuthError(rc, ar.cause());
            }
        });
    }

    private void handlerCreateUser(RoutingContext rc ) {
        String username = rc.getBodyAsJson().getString("username");
        String password = rc.getBodyAsJson().getString("password");
        if (username == null || password == null) {
            rc.end("Invalid username or password");
        }

        userProfile.Existed(username).onComplete(
                ar-> {
                    if (ar.succeeded()) {
                    rc.end("User existed");
                }}
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
        logger.error("user login failed");
        rc.fail(403);
    }

    private String makeJwtToken(String username) {
        JsonObject claims = new JsonObject()
                .put("username", username);
        JWTOptions opts = new JWTOptions()
                .setAlgorithm("RS256")
                .setExpiresInMinutes(10_080)
                .setSubject(username);

        return jwtAuth.generateToken(claims, opts);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        super.start(startPromise);
        port = 81;

        BodyHandler bodyHandler = BodyHandler.create();
        HttpServer svr = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.post().handler(bodyHandler);
        router.put().handler(bodyHandler);

        JWTAuthHandler authHandler = JWTAuthHandler.create(jwtAuth);
        router.get("/test").handler(routingContext -> {
            routingContext.end("test page");
        });
        router.post("/api/users/login").handler(this::handleLogin);
        router.post("/api/users").handler(authHandler).handler(this::handlerCreateUser);


        svr.requestHandler(router).listen(port);
    }
}

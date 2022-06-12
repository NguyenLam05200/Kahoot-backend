package api;

import entity.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
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

  public ProfileVerticle(UserProfile userProfileRepo, JWTAuth jwtAuthProvider) {
    this.userProfile = userProfileRepo;
    this.jwtAuthProvider = jwtAuthProvider;
    }



    private void handlerCreateUser(RoutingContext rc) {
        String username = rc.getBodyAsJson().getString("username");
        String password = rc.getBodyAsJson().getString("password");
        if (username == null || password == null) {
            rc.end("Invalid username or password");
      return;
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

  private void handlerUpdateUser(RoutingContext rc) {}

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
        JWTAuthHandler adminAuthHandler = JWTAuthHandler.create(jwtAuthProvider).withScope("scope.admin");

    router
        .get(apiPrefix + "/test")
        .handler(
            rc -> {
              rc.end("test page");
            });

    router
        .get(apiPrefix + "/admin")
        .handler(adminAuthHandler)
        .handler(
            routingContext -> {
              routingContext.end("test page");
            });

        router.post(apiPrefix +"/api/users").handler(this::handlerCreateUser);

    router
        .get(apiPrefix + "/users/:username")
        .handler(authHandler)
        .handler(
            rc -> {
              String username = rc.pathParam("username");

              rc.end("To Be Defined");
            });

        svr.requestHandler(router).listen(port);
    }
}

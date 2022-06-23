package api;

import entity.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.mongo.MongoClient;
import repo.profile.UserProfile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ProfileVerticle extends AbstractVerticle {
  private MongoClient mongoClient;
    private final UserProfile userProfile;
    private final JWTAuth jwtAuthProvider;
    static final Logger logger = LoggerFactory.getLogger(ProfileVerticle.class);

    private static final String CONFIG_HTTP_PORT = "CONFIG_HTTP_PORT";
    private static final String CONFIG_HTTP_API_PREFIX = "CONFIG_HTTP_API_PREFIX";
    private int port;
  private final String apiPrefix = "/v1/api"; // e.g /test
  private final String adminApiPrefix = "/v1/api/admin";

  public ProfileVerticle(
      UserProfile userProfileRepo, JWTAuth jwtAuthProvider, MongoClient mongoClient) {
    this.userProfile = userProfileRepo;
    this.jwtAuthProvider = jwtAuthProvider;
    this.mongoClient = mongoClient;
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

  private boolean validateQuiz(JsonObject body) throws Exception {
    if (body.containsKey("quizImage")
        && body.containsKey("questions")
        && !body.getJsonObject("questions").isEmpty()) {
      return true;
    } else {
      throw new Exception("invalid quiz");
    }
  }

  private boolean validateQuestions(JsonObject questions) throws Exception {
    Set<String> fields = questions.fieldNames();
    List<JsonObject> invalidQuestions =
        fields.stream()
            .map(field -> questions.getJsonObject(field))
            .filter(
                question ->
                    !(question.containsKey("text")
                        && question.containsKey("img")
                        && question.containsKey("time")
                        && question.containsKey("ans")))
            .collect(Collectors.toList());

    if (!invalidQuestions.isEmpty()) {
      throw new Exception("invalid questions: " + invalidQuestions);
    }
    return true;
  }

  private void handleCreateQuiz(RoutingContext rc) {
    JsonObject body = rc.getBodyAsJson();
    try {
      validateQuiz(body);
      validateQuestions(body.getJsonObject("questions"));
    } catch (Exception e) {
      rc.response().setStatusCode(500);
      rc.end(e.toString());
      return;
    }

    mongoClient.save(
        "quiz",
        body,
        ar -> {
          if (ar.succeeded()) {
            rc.response().setStatusCode(200);
            JsonObject resp = new JsonObject();
            resp.put("id", ar.result());
            rc.end(resp.encodePrettily());
          } else {
            rc.response().setStatusCode(500);
            rc.end(ar.cause().toString());
            logger.error(ar.cause());
            return;
          }
        });
  }

  private void handleGetQuiz(RoutingContext rc) {
    JsonObject query = new JsonObject();
    query.put("_id", rc.request().getParam("quizID"));
    mongoClient
        .find("quiz", query)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                List<JsonObject> quizzes = ar.result();
                if (quizzes.size() == 0) {
                  rc.response().setStatusCode(400);
                  rc.end("Not found");
                }
                rc.end(quizzes.get(0).toString());
              } else {
                rc.response().setStatusCode(500);
              }
            });
  }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        super.start(startPromise);
        port = Integer.parseInt(System.getenv(CONFIG_HTTP_PORT));

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

    router
        .get(apiPrefix + "/admin")
        .handler(adminAuthHandler)
        .handler(
            routingContext -> {
              routingContext.end("test page");
            });

    router
        .get(adminApiPrefix + "/quiz/:quizID")
        .handler(adminAuthHandler)
        .handler(this::handleGetQuiz);

    router.post(adminApiPrefix + "/quiz").handler(adminAuthHandler).handler(this::handleCreateQuiz);

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

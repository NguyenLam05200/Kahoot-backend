package api;

import entity.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import org.apache.commons.lang3.StringUtils;
import repo.profile.UserProfile;

import java.util.List;

public class ProfileVerticle extends AbstractVerticle {
  static final Logger logger = LoggerFactory.getLogger(ProfileVerticle.class);
  private static final String CONFIG_HTTP_PORT = "CONFIG_HTTP_PORT";
  private static final String CONFIG_HTTP_API_PREFIX = "CONFIG_HTTP_API_PREFIX";
  private final UserProfile userProfile;
  private final JWTAuth jwtAuthProvider;
  private final int port = 9090;
  private final String apiPrefix = "/v1/api"; // e.g /test
  private final String adminApiPrefix = "/v1/api/admin";
  private MongoClient mongoClient;

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

    userProfile
        .Existed(username)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                if (ar.result()) {
                  rc.end("User existed");
                  return;
                }
              }
            });

    Future<User> user = userProfile.Create(username, password);
    user.onComplete(
        ar -> {
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
        && !body.getJsonArray("questions").isEmpty()) {
      return true;
    } else {
      throw new Exception("invalid quiz");
    }
  }

  private boolean validateQuestions(JsonObject body) throws Exception {
    JsonArray questions = body.getJsonArray("questions");
    for (int i = 0; i < questions.size(); i++) {
      JsonObject question = questions.getJsonObject(i);
      boolean valid =
          question.containsKey("id")
              && question.containsKey("text")
              && question.containsKey("img")
              && question.containsKey("time")
              && question.containsKey("ans")
              && question.containsKey("points");

      if (!valid) {
        throw new Exception("invalid questions");
      }
    }
    return true;
  }

  private void handleCreateQuiz(RoutingContext rc) {
    JsonObject body = rc.getBodyAsJson();
    try {
      validateQuiz(body);
      validateQuestions(body);
    } catch (Exception e) {
      rc.response().setStatusCode(400);
      JsonObject resp = new JsonObject();
      resp.put("error", e.toString());
      rc.end(resp.encodePrettily());
      return;
    }
    String headerValue = rc.request().getHeader("Authorization");
    String jwt = StringUtils.remove(headerValue, "Bearer ");
    JsonObject credentials = new JsonObject().put("token", jwt);
    jwtAuthProvider
        .authenticate(credentials)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                body.put("email", ar.result().principal().getString("email"));
                mongoClient.save(
                    "quiz",
                    body,
                    arr -> {
                      if (arr.succeeded()) {
                        if (StringUtils.isEmpty(arr.result())) {
                          // case create quiz
                          mongoClient
                              .findOneAndUpdate(
                                  "quiz",
                                  new JsonObject().put("_id", body.getString("_id")),
                                  new JsonObject().put("$set", new JsonObject().put("plays", 0)))
                              .onComplete(
                                  updateAr -> {
                                    logger.info("add Plays result: " + updateAr.result());
                                  });
                        }
                        rc.response().setStatusCode(200);
                        JsonObject resp = new JsonObject();
                        resp.put("id", arr.result());
                        rc.end(resp.encodePrettily());
                      } else {
                        rc.response().setStatusCode(500);
                        JsonObject resp = new JsonObject();
                        resp.put("error", ar.cause().toString());
                        rc.end(resp.encodePrettily());
                        logger.error(arr.cause());
                        return;
                      }
                    });

              } else {
                rc.response().setStatusCode(500);
                rc.end(ar.cause().toString());
                logger.error("Invalid JWT: " + ar.cause());
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

  private void handleGetAllQuizzesByUser(RoutingContext rc) {
    parseEmailFromRoutingContext(rc)
        .compose(
            email -> {
              return mongoClient.find("quiz", new JsonObject().put("email", email));
            })
        .onComplete(
            quizzes -> {
              JsonObject response = new JsonObject();
              response.put("quizzes", quizzes.result());
              rc.response().setStatusCode(200);
              rc.end(response.encodePrettily());
            });
  }

  private void handleDeleteQuiz(RoutingContext rc) {

    parseEmailFromRoutingContext(rc)
        .compose(
            email -> {
              JsonObject query = new JsonObject();
              query.put("_id", rc.request().getParam("quizID"));
              query.put("email", email);
              return mongoClient.findOneAndDelete("quiz", query);
            })
        .onSuccess(
            ok -> {
              rc.response().setStatusCode(200);
              rc.end();
            })
        .onFailure(
            ar -> {
              JsonObject response = new JsonObject();
              response.put("error", ar.getCause());
              rc.response().setStatusCode(500);
              rc.end(response.encodePrettily());
            });
  }

  private Future<String> parseEmailFromRoutingContext(RoutingContext rc) {
    String headerValue = rc.request().getHeader("Authorization");
    String jwt = StringUtils.remove(headerValue, "Bearer ");
    JsonObject credentials = new JsonObject().put("token", jwt);
    Promise promise = Promise.promise();
    jwtAuthProvider
        .authenticate(credentials)
        .onComplete(
            ar -> {
              String email = ar.result().principal().getString("email");
              promise.tryComplete(email);
            });
    return promise.future();
  }

  private void handleIncreasePlay(RoutingContext rc) {
    String quizId = rc.pathParam("quizId");
    JsonObject updateJson = new JsonObject().put("$inc", new JsonObject().put("plays", 1));
    parseEmailFromRoutingContext(rc)
        .compose(
            email -> {
              JsonObject query = new JsonObject().put("email", email).put("_id", quizId);
              return mongoClient
                  .findOneAndUpdate("quiz", query, updateJson)
                  .onSuccess(
                      ok -> {
                        rc.response().setStatusCode(200);
                        JsonObject response = new JsonObject();
                        response.put("plays", ok.getString("plays"));
                        rc.end(response.encodePrettily());
                      })
                  .onFailure(
                      ar -> {
                        JsonObject response = new JsonObject();
                        response.put("error", ar.getCause());
                        rc.response().setStatusCode(500);
                        rc.end(response.encodePrettily());
                      });
            });
  }

  private void handleGetReport(RoutingContext rc) {
    String id = rc.pathParam("reportId");
    parseEmailFromRoutingContext(rc)
        .compose(
            email -> {
              return mongoClient.find(
                  "reports", new JsonObject().put("email", email).put("_id", id));
            })
        .onSuccess(
            result -> {
              if (result.isEmpty()) {
                rc.response().setStatusCode(400);
                JsonObject response = new JsonObject();
                response.put("error", "not found");
                rc.end(response.encodePrettily());
                return;
              }
              rc.end(result.get(0).encodePrettily());
            })
        .onFailure(
            ar -> {
              JsonObject response = new JsonObject();
              response.put("error", ar.getCause());
              rc.response().setStatusCode(500);
              rc.end(response.encodePrettily());
            });
  }

  private void handleDeleteReport(RoutingContext rc) {
    parseEmailFromRoutingContext(rc)
        .compose(
            email -> {
              JsonObject query = new JsonObject();
              query.put("_id", rc.request().getParam("reportId"));
              query.put("email", email);
              return mongoClient.findOneAndDelete("reports", query);
            })
        .onSuccess(
            ok -> {
              rc.response().setStatusCode(200);
              rc.end();
            })
        .onFailure(
            ar -> {
              JsonObject response = new JsonObject();
              response.put("error", ar.getCause());
              rc.response().setStatusCode(500);
              rc.end(response.encodePrettily());
            });
  }

  private void handleCreateReport(RoutingContext rc) {
    JsonObject body = rc.getBodyAsJson();
    if (!body.containsKey("quizID")
        || !body.containsKey("percentRightTotal")
        || !body.containsKey("players")
        || !body.containsKey("timeStart")
        || !body.containsKey("timeEnd")
        || !body.containsKey("analysisResults")
        || !body.containsKey("listCountChooseAns")) {

      rc.response().setStatusCode(401);
      rc.end("bad request: missing fields in body");
    }

    String headerValue = rc.request().getHeader("Authorization");
    String jwt = StringUtils.remove(headerValue, "Bearer ");
    JsonObject credentials = new JsonObject().put("token", jwt);
    jwtAuthProvider
        .authenticate(credentials)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                body.put("email", ar.result().principal().getString("email"));
                body.put("author", ar.result().principal().getString("name"));
                mongoClient.save(
                    "reports",
                    body,
                    arr -> {
                      if (arr.succeeded()) {
                        rc.response().setStatusCode(200);
                        JsonObject resp = new JsonObject();
                        resp.put("id", arr.result());
                        rc.end(resp.encodePrettily());
                      } else {
                        rc.response().setStatusCode(500);
                        JsonObject resp = new JsonObject();
                        resp.put("error", ar.cause().toString());
                        rc.end(resp.encodePrettily());
                        logger.error(arr.cause());
                        return;
                      }
                    });

              } else {
                rc.response().setStatusCode(500);
                rc.end(ar.cause().toString());
                logger.error("Invalid JWT: " + ar.cause());
                return;
              }
            });
  }

  private void handleGetAllReportsByUser(RoutingContext rc) {
    parseEmailFromRoutingContext(rc)
        .compose(
            email -> {
              return mongoClient.find("reports", new JsonObject().put("email", email));
            })
        .onComplete(
            quizzes -> {
              JsonObject response = new JsonObject();
              response.put("reports", quizzes.result());
              rc.response().setStatusCode(200);
              rc.end(response.encodePrettily());
            });
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    super.start(startPromise);

    BodyHandler bodyHandler = BodyHandler.create();
    HttpServer svr = vertx.createHttpServer();
    Router router = Router.router(vertx);
    router.post().handler(bodyHandler);
    router.put().handler(bodyHandler);

    JWTAuthHandler authHandler = JWTAuthHandler.create(jwtAuthProvider);
    JWTAuthHandler adminAuthHandler =
        JWTAuthHandler.create(jwtAuthProvider).withScope("scope.admin");

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

    router
        .delete(adminApiPrefix + "/quiz/:quizID")
        .handler(adminAuthHandler)
        .handler(this::handleDeleteQuiz);

    router
        .get(adminApiPrefix + "/quiz-all")
        .handler(adminAuthHandler)
        .handler(this::handleGetAllQuizzesByUser);

    router.post(adminApiPrefix + "/quiz").handler(adminAuthHandler).handler(this::handleCreateQuiz);

    router
        .post(adminApiPrefix + "/plays/:quizId")
        .handler(adminAuthHandler)
        .handler(this::handleIncreasePlay);

    router
        .get(adminApiPrefix + "/report/:reportId")
        .handler(adminAuthHandler)
        .handler(this::handleGetReport);

    router
        .post(adminApiPrefix + "/report")
        .handler(adminAuthHandler)
        .handler(this::handleCreateReport);

    router
        .get(adminApiPrefix + "/report-all")
        .handler(adminAuthHandler)
        .handler(this::handleGetAllReportsByUser);

    router
        .delete(adminApiPrefix + "/report/:reportId")
        .handler(adminAuthHandler)
        .handler(this::handleDeleteReport);

    svr.requestHandler(router).listen(port);
  }
}


import api.ProfileVerticle;
import infra.profile.MemProfileImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.mongo.MongoClient;
import repo.profile.UserProfile;
import utils.JwtAuthHelper;
import utils.PassObfuscatorImpl;
import utils.PasswordObfuscator;

public class MainVerticle extends AbstractVerticle {
    static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);
  private static final String JWT_SECRET_KEY = "keyboard cat";

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        super.start(startPromise);


    }

    public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    PasswordObfuscator po = new PassObfuscatorImpl();
    JWTAuth jwtAuthProvider = JwtAuthHelper.createSHAJWTAuth(vertx, JWT_SECRET_KEY);
    UserProfile profileRepo = new MemProfileImpl(po);
    JsonObject mongoConfig =
        new JsonObject().put("host", "localhost").put("port", 27017).put("db_name", "profiles");
    MongoClient mongoClient = MongoClient.createShared(vertx, mongoConfig);
    ProfileVerticle userProfileVerticle = new ProfileVerticle(profileRepo, jwtAuthProvider, mongoClient);
        vertx.deployVerticle(userProfileVerticle, ar -> {
            if (ar.succeeded()) {
                logger.info("done");
            } else {
                logger.error("failed" + ar.cause());
            }
        });

    //        vertx.deployVerticle(new IngestVerticle(), ar -> {
    //            if (ar.failed()) {
    //                logger.error("failed" + ar.cause());
    //            } else {
    //                logger.info("deployed Ingest Verticle");
    //            }
    //        });

  }
}

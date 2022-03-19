import api.IngestVerticle;
import api.ProfileVerticle;
import infra.profile.MemProfileImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.auth.jwt.JWTAuth;
import repo.profile.UserProfile;
import utils.JwtAuthHelper;
import utils.PassObfuscatorImpl;
import utils.PasswordObfuscator;

public class MainVerticle extends AbstractVerticle {
    static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        super.start(startPromise);


    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        PasswordObfuscator po = new PassObfuscatorImpl();
        JWTAuth provider = JwtAuthHelper.createRSAJWTAuth(vertx);
        UserProfile profile = new MemProfileImpl(po);

        ProfileVerticle userProfileVerticle = new ProfileVerticle(profile, provider);

        vertx.deployVerticle(userProfileVerticle, ar -> {
            if (ar.succeeded()) {
                logger.info("done");
            } else {
                logger.error("failed" + ar.cause());
            }
        });


        vertx.deployVerticle(new IngestVerticle(), ar -> {
            if (ar.failed()) {
                logger.error("failed" + ar.cause());
            } else {
                logger.info("deployed Ingest Verticle");
            }
        });

        vertx.deployVerticle(userProfileVerticle, ar -> {
            if (ar.failed()) {
                logger.error("failed" + ar.cause());
            } else {
                logger.info("deployed Auth Verticle");
            }
        });
    }
}

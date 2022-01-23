import api.IngestVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import utils.PassObfuscatorImpl;
import utils.PasswordObfuscator;

public class MainVerticle extends AbstractVerticle {
    static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        super.start(startPromise);

        PasswordObfuscator po = new PassObfuscatorImpl();
//        UserProfile profile = new MemProfileImpl(po);
//        ProfileVerticle userProfileVerticle = new ProfileVerticle(profile);

//        vertx.deployVerticle(userProfileVerticle, ar -> {
//            if (ar.succeeded()) {
//                logger.info("done");
//            } else {
//                logger.error("failed" + ar.cause());
//            }
//        });
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        MainVerticle v = new MainVerticle();

        vertx.deployVerticle(v, ar -> {
            if (ar.succeeded()) {
                logger.info("Deploy main verticle succeeded");
            } else {
                logger.error("Deploy main verticle failed: " + ar.cause());
            }
        });
        vertx.deployVerticle(new IngestVerticle(), ar -> {
            if (ar.failed()) {
                logger.error("failed" + ar.cause());
            }
        });
    }
}

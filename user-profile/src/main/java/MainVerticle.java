import common.JsonSerializer;
import entity.User;
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

        User u = new User("dinhnn", "123");
        JsonSerializer serializer = new JsonSerializer();

        try {
            logger.info("LOL"+serializer.serialize(u));
        } catch (Exception e) {

        }

        vertx.deployVerticle(v, ar -> {
            if (ar.succeeded()) {
                logger.info("Deploy main verticle succeeded");
            } else {
                logger.error("Deploy main verticle failed: " +ar.cause());
            }
        });
    }
}

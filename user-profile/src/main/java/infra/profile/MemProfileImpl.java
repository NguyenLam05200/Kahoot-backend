package infra.profile;

import entity.User;
import io.vertx.core.Future;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import repo.profile.UserProfile;
import utils.PasswordObfuscator;

import java.util.HashMap;

public class MemProfileImpl implements UserProfile {
    private final static Logger logger = LoggerFactory.getLogger(MemProfileImpl.class);
    final PasswordObfuscator passwordObfuscator;
    private final HashMap<String, User> data;

    public MemProfileImpl(PasswordObfuscator passwordObfuscator) {
        this.passwordObfuscator = passwordObfuscator;
        this.data = new HashMap<>();
        this.data.put("admin", new User(
                "admin",
                passwordObfuscator.Obfuscate("admin")
        ));
    }

    @Override
    public Future<Boolean> Existed(String username) {
        boolean exists = data.containsKey(username);
        return Future.succeededFuture(exists);
    }

    @Override
    public Future<User> Create(String username, String password) {
        String hashedPass = passwordObfuscator.Obfuscate(password);
        User newUser = new User(username, hashedPass);
        data.put(username, newUser);
        return Future.succeededFuture(newUser);
    }

    @Override
    public Future<User> VerifyProfile(String username, String password) {
        String hashedPass = passwordObfuscator.Obfuscate(password);
        User user = data.get(username);
        if (user != null && hashedPass.equals(user.hashedPass)) {
            return Future.succeededFuture(user);
        }
        logger.info("get user failed username: " + username);
        return Future.failedFuture("user not found");
    }
}

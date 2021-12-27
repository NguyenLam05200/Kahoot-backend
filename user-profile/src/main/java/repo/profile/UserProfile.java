package repo.profile;

import io.vertx.core.Future;
import entity.User;


public interface UserProfile {
    Future<Boolean> Existed(String username);
    Future<User> Create(String username, String password);
    Future<User> VerifyProfile(String username, String password);
}

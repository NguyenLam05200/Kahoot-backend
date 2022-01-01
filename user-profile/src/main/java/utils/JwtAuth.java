package utils;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

public class JwtAuth {
    public JWTAuth GetJWTAuth(Vertx vertx) {
        try {
            String publicKey = CryptoHelper.publicKey();
            String privateKey = CryptoHelper.privateKey();
            return JWTAuth.create(vertx, new JWTAuthOptions()
                    .addPubSecKey(new PubSecKeyOptions()
                            .setAlgorithm("SHA256")
                            .setBuffer(publicKey)
                    )
                    .addPubSecKey(new PubSecKeyOptions()
                            .setAlgorithm("SHA256")
                            .setBuffer(privateKey)
                    )
            );
        } catch (Exception e) {
            return null;
        }
    }
}

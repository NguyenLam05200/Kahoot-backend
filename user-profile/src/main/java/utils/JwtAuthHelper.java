package utils;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

public class JwtAuthHelper {
    public static JWTAuth createRSAJWTAuth(Vertx vertx) {
        try {
            String publicKey = CryptoHelper.publicKey();
            String privateKey = CryptoHelper.privateKey();
            return JWTAuth.create(vertx, new JWTAuthOptions()
                    .addPubSecKey(new PubSecKeyOptions()
                            .setAlgorithm("RS256")
                            .setBuffer(publicKey)
                    )
                    .addPubSecKey(new PubSecKeyOptions()
                            .setAlgorithm("RS256")
                            .setBuffer(privateKey)
                    )
            );
        } catch (Exception e) {
            System.out.println("Error"+e);
            return null;
        }
    }

    public static JWTAuth createSHAJWTAuth(Vertx vertx) {
        try {
            String privateKey = CryptoHelper.privateKey();
            return JWTAuth.create(vertx, new JWTAuthOptions()
                    .addPubSecKey(new PubSecKeyOptions()
                            .setAlgorithm("HS256")
                            .setBuffer(privateKey)
                    )
            );
        } catch (Exception e) {
            System.out.println("Error"+e);
            return null;
        }
    }
    public static JWTAuth createSHAJWTAuth(Vertx vertx, String secret) {
        try {
            return JWTAuth.create(vertx, new JWTAuthOptions()
                    .addPubSecKey(new PubSecKeyOptions()
                            .setAlgorithm("HS256")
                            .setBuffer(secret)
                    )
            );
        } catch (Exception e) {
            System.out.println("Error"+e);
            return null;
        }
    }
}

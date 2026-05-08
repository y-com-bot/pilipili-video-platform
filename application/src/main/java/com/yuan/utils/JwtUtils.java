package com.yuan.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.util.Date;
import java.util.UUID;

public class JwtUtils {
    private static final String SECRET = "dsjafk_jdfkgk_91kdhfo";
    private static final long EXPIRE_TIME = 1000 * 60 * 60 * 2;

    public static String createToken(Long userId, String username, String role){
        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        Date now = new Date();
        return JWT.create().withClaim("userId", userId)
                .withClaim("username",username)
                .withClaim("role",role)
                .withIssuedAt(now)
                .withJWTId(UUID.randomUUID().toString())
                .withExpiresAt(new Date(now.getTime() + EXPIRE_TIME))
                .sign(algorithm);
    }

    public static DecodedJWT verifyToken(String token){
        try{
            Algorithm algorithm = Algorithm.HMAC256(SECRET);
            JWTVerifier verifier = JWT.require(algorithm).build();
            return verifier.verify(token);
        }catch(JWTVerificationException e){
            return null;
        }
    }
}

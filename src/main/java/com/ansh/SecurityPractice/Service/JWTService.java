package com.ansh.SecurityPractice.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JWTService {

    private String secretKey ="";

    public JWTService()  {
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("HmacSHA256");
            SecretKey sk = keyGen.generateKey();
            secretKey = Base64.getEncoder().encodeToString(sk.getEncoded());
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String generateToken(String username) {

        Map<String, Object> claims = new HashMap<>();

        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis()+60 * 60 * 30))
                .and()
                .signWith(getKey())
                .compact();

    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Extracts the username (subject) from the token
    public String extractUserName(String token) {
        // Uses a reusable method to extract claims and get the 'subject' (username)
        return extractClaim(token, Claims::getSubject);
    }

    // Generic method to extract any claim from the token using a function (e.g., getSubject, getExpiration)
    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        // Parses all claims from the token
        final Claims claims = extractAllClaims(token);
        // Applies the provided function to extract a specific claim
        return claimResolver.apply(claims);
    }

    // Parses and extracts all claims (payload) from the token after verifying its signature
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey()) // Uses the secret/signing key to verify the token signature
                .build()
                .parseSignedClaims(token) // Parses the token and extracts the claims (payload)
                .getPayload();
    }

    // Validates the token by ensuring:
// 1. The username inside the token matches the expected user details
// 2. The token has not expired
    public boolean validateToken(String token, UserDetails userDetails) {
        final String userName = extractUserName(token);
        // Check username matches and token is not expired
        return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Checks if the token's expiration date is before the current date (i.e., expired)
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Extracts the expiration date from the token
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}

package com.placemate.auth;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.placemate.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
@Service
public class JwtService {
 private final SecretKey signingKey; private final long expirationMs;
 public JwtService(@Value("${app.jwt.secret}") String secret,@Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
  this.signingKey=Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(secret)); this.expirationMs=expirationMs;
 }
 public String generateToken(User user) {
  Date now=new Date(); return Jwts.builder().subject(user.getEmail()).claim("role",user.getRole().name())
   .issuedAt(now).expiration(new Date(now.getTime()+expirationMs)).signWith(signingKey).compact();
 }
 public String extractEmail(String token){return parseClaims(token).getSubject();}
 public boolean isValid(String token){try{return parseClaims(token).getExpiration().after(new Date());}catch(RuntimeException ex){return false;}}
 public long getExpirationMs(){return expirationMs;}
 private Claims parseClaims(String token){return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();}
}

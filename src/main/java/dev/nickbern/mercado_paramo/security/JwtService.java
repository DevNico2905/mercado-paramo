package dev.nickbern.mercado_paramo.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    public String generarToken(UserDetails usuario){

        String rol = usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElseThrow();

        Date now = new Date();
        Date expire = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("rol", rol)
                .issuedAt(now)
                .expiration(expire)
                .signWith(secretKey)
                .compact();
    }

    public String extraerEmail(String token){
        return parsearClaims(token).getSubject();
    }

    private Claims parsearClaims(String token){
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean esValido(String token, UserDetails usuario){
        try {
            String email = extraerEmail(token);
            return email.equals(usuario.getUsername()) && usuario.isEnabled();
        } catch (JwtException | IllegalArgumentException e){
            return false;
        }
    }

    public long getExpiracionSegundos() {
        return expirationMs / 1000;
    }

}

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Map;

public class JwtMake {
    public static void main(String[] args) {
        String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        long now = System.currentTimeMillis();
        long exp = now + 86400000L;
        String token = Jwts.builder()
                .setClaims(Map.of("role", "ADMIN", "email", "admin@eventsphere.com", "fullName", "Admin User"))
                .setSubject("admin")
                .setIssuedAt(new java.util.Date(now))
                .setExpiration(new java.util.Date(exp))
                .signWith(key)
                .compact();
        System.out.println(token);
    }
}

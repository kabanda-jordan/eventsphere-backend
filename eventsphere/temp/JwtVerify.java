import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

public class JwtVerify {
    public static void main(String[] args) {
        try {
            String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
            String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiQURNSU4iLCJlbWFpbCI6ImFkbWluQGV2ZW50c3BoZXJlLmNvbSIsImZ1bGxOYW1lIjoiQWRtaW4gVXNlciIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzc4Njg0OTM1LCJleHAiOjE3Nzc3NzEzMzV9.jdKFcMNbMdHZnzArWBZVpQSy8strnHIS5uFv6wN54VM";
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            System.out.println("VALID");
            System.out.println(claims);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

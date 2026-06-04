import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

public class JwtVerify2 {
  public static void main(String[] args) {
    try {
      String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
      SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
      Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims("" + args[0]).getPayload();
      System.out.println("VALID");
      System.out.println(claims);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

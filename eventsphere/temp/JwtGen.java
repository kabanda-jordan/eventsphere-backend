import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.time.Instant;
import java.util.Map;

public class JwtGen {
    public static void main(String[] args) throws Exception {
        String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        String username = "admin";
        String email = "admin@eventsphere.com";
        String fullName = "Admin User";
        String role = "ADMIN";
        long expirationMillis = 86400000L;
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        long now = Instant.now().getEpochSecond();
        long exp = now + (expirationMillis / 1000);
        String payloadJson = String.format("{\"role\":\"%s\",\"email\":\"%s\",\"fullName\":\"%s\",\"sub\":\"%s\",\"iat\":%d,\"exp\":%d}", role, email, fullName, username, now, exp);
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        System.out.println(header + "." + payload + "." + signature);
    }
}

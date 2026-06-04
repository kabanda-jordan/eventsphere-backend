import io.jsonwebtoken.io.Decoders;
public class SecretInfo {
  public static void main(String[] args) {
    String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    byte[] bytes = Decoders.BASE64.decode(secret);
    System.out.println(bytes.length);
    for (int i = 0; i < Math.min(32, bytes.length); i++) {
      System.out.printf("%02x", bytes[i]);
    }
    System.out.println();
  }
}

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    // Essa estrutura de dados é apenas um exemplo, em um ambiente real você precisaria consultar um banco de dados ou similar
    private Map<String, String> users = Map.of("user", "password");

    @GetMapping("/login")
    public ResponseEntity<?> login(@RequestHeader("Authorization") String authHeader) {
        // Basic base64encoded(username:password)
        String base64Credentials = authHeader.substring("Basic".length()).trim();
        String credentials = new String(Base64.getDecoder().decode(base64Credentials),
                Charset.forName("UTF-8"));
        // credentials = username:password
        final String[] values = credentials.split(":",2);

        String username = values[0];
        String password = values[1];

        // Aqui verificamos se as credenciais fornecidas correspondem a um usuário existente
        if (users.containsKey(username) && users.get(username).equals(password)) {
            return ResponseEntity.ok("Logged in!");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }
}

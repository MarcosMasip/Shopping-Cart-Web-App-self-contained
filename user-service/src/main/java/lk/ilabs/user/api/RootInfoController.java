package lk.ilabs.user.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class RootInfoController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "service", "user-service",
                "version", "1.0.0",
                "timestamp", Instant.now().toString(),
                "endpoints", new String[]{"/api/v1/users", "/api/v1/users/{id}", "/api/v1/users/username/{username}"}
        );
    }
}

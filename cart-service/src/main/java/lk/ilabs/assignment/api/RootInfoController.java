package lk.ilabs.assignment.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class RootInfoController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "service", "cart-service",
                "version", "1.0.0",
                "timestamp", Instant.now().toString(),
                "endpoints", new String[]{"/api/v1/cart-items", "/api/v1/cart-items/summary"}
        );
    }
}

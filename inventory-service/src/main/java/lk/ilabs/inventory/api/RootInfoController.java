package lk.ilabs.inventory.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class RootInfoController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "service", "inventory-service",
                "version", "1.0.0",
                "timestamp", Instant.now().toString(),
                "endpoints", new String[]{"/api/v1/items", "/api/v1/items/{code}"}
        );
    }
}

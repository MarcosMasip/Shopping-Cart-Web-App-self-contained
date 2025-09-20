package lk.ilabs.assignment.apigateway.service;

import lk.ilabs.assignment.apigateway.dto.UserDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class AuthService implements ReactiveUserDetailsService {

    private final WebClient.Builder webClientBuilder;
    private final String userServiceBaseUrl;

    public AuthService(WebClient.Builder webClientBuilder,
                       @Value("${service.user.base-url:http://localhost:8082}") String userServiceBaseUrl) {
        this.webClientBuilder = webClientBuilder;
        this.userServiceBaseUrl = userServiceBaseUrl;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return Mono.create(sink -> {
        // Fetch by username (String) to avoid Long path variable mismatch
        Mono<UserDTO> dtoMono = webClientBuilder.build()
            .get()
            .uri(userServiceBaseUrl + "/api/v1/users/username/{username}", username)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> Mono.empty())
                    .bodyToMono(UserDTO.class);

            dtoMono.subscribe(userDTO -> {
                if (userDTO == null || userDTO.getUsername() == null) {
                    sink.success(null);
                } else {
                    sink.success(new User(
                            userDTO.getUsername(),
                            userDTO.getPassword(),
                            List.of(new SimpleGrantedAuthority("ROLE_" + userDTO.getRole()))
                    ));
                }
            }, sink::error);
        });

    }
}

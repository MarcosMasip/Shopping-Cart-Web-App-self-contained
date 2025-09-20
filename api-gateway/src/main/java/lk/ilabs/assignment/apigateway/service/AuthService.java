package lk.ilabs.assignment.apigateway.service;

import lk.ilabs.assignment.apigateway.dto.UserDTO;
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

    public AuthService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return Mono.create(sink -> {
            // Direct static routing (service discovery removed): user-service runs on port 8082 by default.
            Mono<UserDTO> dtoMono = webClientBuilder.build()
                    .get()
                    .uri("http://localhost:8082/api/v1/users/{username}", username)
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

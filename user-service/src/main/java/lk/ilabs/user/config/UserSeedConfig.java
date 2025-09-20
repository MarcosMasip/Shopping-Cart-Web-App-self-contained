package lk.ilabs.user.config;

import lk.ilabs.user.entity.User;
import lk.ilabs.user.repository.UserRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Configuration
public class UserSeedConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeedConfig.class);

    private final UserRepository repo;

    public UserSeedConfig(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void run(String... args){
        log.info("[UserSeed] Starting user seeding check");
        seed();
        log.info("[UserSeed] Completed user seeding check");
    }

    @Transactional
    protected void seed(){
        String passwordPlain = "demo";
        String hash = DigestUtils.sha256Hex(passwordPlain);
        record U(String username, String role) {}
        List<U> required = List.of(
                new U("system", "ROLE_SYSTEM"),
                new U("admin", "ROLE_ADMIN"),
                new U("demo", "ROLE_USER")
        );
        for (U u : required){
            repo.findByUsername(u.username()).ifPresentOrElse(
                    existing -> {
                        log.info("[UserSeed] EXISTS username={} role={} id={}", u.username(), u.role(), existing.getId());
                    },
                    () -> {
                        User saved = repo.save(new User(null, u.username(), hash, u.role(), Instant.now()));
                        log.info("[UserSeed] CREATED username={} role={} id={}", u.username(), u.role(), saved.getId());
                    }
            );
        }
    }
}

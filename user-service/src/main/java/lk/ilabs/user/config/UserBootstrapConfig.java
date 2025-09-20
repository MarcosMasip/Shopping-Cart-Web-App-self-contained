package lk.ilabs.user.config;

import lk.ilabs.user.entity.User;
import lk.ilabs.user.repository.UserRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Configuration
public class UserBootstrapConfig {

    private static final Logger log = LoggerFactory.getLogger(UserBootstrapConfig.class);

    private record SeedUser(String username, String role){}

    private static final SeedUser[] DEFAULT_USERS = new SeedUser[]{
            new SeedUser("system", "ROLE_SYSTEM"),
            new SeedUser("admin",  "ROLE_ADMIN"),
            new SeedUser("demo",   "ROLE_USER")
    };

    @Bean
    ApplicationRunner userSeedRunner(UserRepository repo){
        return args -> seed(repo);
    }

    @Transactional
    protected void seed(UserRepository repo){
        String hash = DigestUtils.sha256Hex("demo"); // shared password
        int created = 0;
        for(SeedUser su : DEFAULT_USERS){
            repo.findByUsername(su.username()).ifPresentOrElse(u -> {}, () -> {
                repo.save(new User(null, su.username(), hash, su.role(), Instant.now()));
                log.info("Seeded user {} with role {}", su.username(), su.role());
            });
        }
    }
}

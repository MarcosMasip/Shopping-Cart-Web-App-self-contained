package lk.ilabs.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private String role; // ROLE_SYSTEM, ROLE_ADMIN, ROLE_USER
    private Instant createdAt;

    @PrePersist
    public void prePersist(){
        if (createdAt == null) createdAt = Instant.now();
    }
}

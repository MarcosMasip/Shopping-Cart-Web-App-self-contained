package lk.ilabs.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
public class UserDTO implements Serializable {
    private Long id;
    private String username;
    // Expose hashed password for demo authentication via gateway Basic Auth (NOT for production use)
    private String password; // contains password hash
    private String role;     // Stored as ROLE_USER, ROLE_ADMIN, etc.
    private Instant createdAt;

    public UserDTO(Long id, String username, String password, String role, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.createdAt = createdAt;
    }
}

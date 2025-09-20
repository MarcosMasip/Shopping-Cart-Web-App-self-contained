package lk.ilabs.user.service.impl;

import lk.ilabs.user.dto.UserDTO;
import lk.ilabs.user.entity.User;
import lk.ilabs.user.repository.UserRepository;
import lk.ilabs.user.service.UserService;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDTO create(String username, String rawPassword, String role) {
        userRepository.findByUsername(username).ifPresent(u -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists"); });
        User saved = userRepository.save(new User(null, username, DigestUtils.sha256Hex(rawPassword), role, null));
        return toDTO(saved);
    }

    @Override
    public UserDTO get(Long id) {
        return userRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Override
    public UserDTO getByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::toDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Override
    public List<UserDTO> list() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .map(this::toDTO)
                .toList();
    }

    private UserDTO toDTO(User u){
        // Provide hashed password to gateway for Basic auth password comparison (demo only)
        return new UserDTO(u.getId(), u.getUsername(), u.getPasswordHash(), u.getRole(), u.getCreatedAt());
    }
}

package lk.ilabs.user.service;

import lk.ilabs.user.dto.UserDTO;

import java.util.List;

public interface UserService {
    UserDTO create(String username, String rawPassword, String role);
    UserDTO get(Long id);
    UserDTO getByUsername(String username);
    List<UserDTO> list();
}

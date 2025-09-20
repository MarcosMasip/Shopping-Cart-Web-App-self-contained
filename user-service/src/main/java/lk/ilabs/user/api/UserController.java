package lk.ilabs.user.api;

import lk.ilabs.user.dto.UserDTO;
import lk.ilabs.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public UserDTO create(@RequestParam String username, @RequestParam String password, @RequestParam(defaultValue = "ROLE_USER") String role){
        return userService.create(username, password, role);
    }

    @GetMapping("/{id}")
    public UserDTO get(@PathVariable Long id){
        return userService.get(id);
    }

    @GetMapping
    public List<UserDTO> list(){
        return userService.list();
    }

    @GetMapping("/username/{username}")
    public UserDTO getByUsername(@PathVariable String username) {
        return userService.getByUsername(username);
    }
}

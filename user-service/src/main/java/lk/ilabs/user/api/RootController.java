package lk.ilabs.user.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {
    @GetMapping(value = "/", produces = "text/plain")
    public String root(){
        return "OK";
    }
}

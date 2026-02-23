package org.com.story.controller;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.response.UserResponse;
import org.com.story.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse getMyProfile() {
        return userService.getUserProfile();
    }
}

package ru.practicum.user;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminUserController {

    private final UserService userService;

    // POST /admin/users — добавить нового пользователя
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody NewUserRequest req) {
        log.info("ADMIN:create user {}", req.getEmail());
        return userService.create(req);
    }

    // GET /admin/users — получить список по ids или пагинацией (from/size)
    @GetMapping
    public List<UserDto> getUsers(@Valid @ModelAttribute AdminUserParam params) {
        log.info("ADMIN:get users params={}", params);
        return userService.getUsers(params);
    }

    // DELETE /admin/users/{userId}
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Integer userId) {
        log.info("ADMIN:delete user id={}", userId);
        userService.delete(userId);
    }
}
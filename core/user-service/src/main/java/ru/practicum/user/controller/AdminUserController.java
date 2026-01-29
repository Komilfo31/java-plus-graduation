package ru.practicum.user.controller;


import interaction.model.user.AdminUserParam;
import interaction.model.user.UserShortDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import interaction.model.user.NewUserRequest;
import interaction.model.user.UserDto;
import ru.practicum.user.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody NewUserRequest req) {
        log.info("ADMIN:create user {}", req.getEmail());
        return userService.create(req);
    }

    @GetMapping
    public List<UserDto> getUsers(@Valid @ModelAttribute AdminUserParam params) {
        log.info("ADMIN:get users params={}", params);
        return userService.getUsers(params);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Integer userId) {
        log.info("ADMIN:delete user id={}", userId);
        userService.delete(userId);
    }

    @GetMapping("/{id}")
    public UserShortDto getById(@PathVariable("id") Integer id) {
        log.info("Запрос пользователя id={} от микросервиса", id);
        return userService.getById(id);
    }

    @GetMapping("/by-ids")
    public List<UserShortDto> getByIds(@RequestParam List<Integer> userIds) {
        log.info("Запрос микросервисом пользователей с ID {}", userIds);
        return userService.getByIds(userIds);
    }
}
package ru.practicum.user;

import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto create(NewUserRequest request);

    List<UserDto> getUsers(AdminUserParam param);

    void delete(Integer userId);
}

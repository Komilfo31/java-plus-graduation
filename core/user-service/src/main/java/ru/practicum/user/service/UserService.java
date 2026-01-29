package ru.practicum.user.service;

import interaction.model.user.AdminUserParam;
import interaction.model.user.NewUserRequest;
import interaction.model.user.UserDto;
import interaction.model.user.UserShortDto;

import java.util.List;

public interface UserService {
    UserDto create(NewUserRequest request);

    List<UserDto> getUsers(AdminUserParam param);

    void delete(Integer userId);

    UserShortDto getById(Integer id);

    List<UserShortDto> getByIds(List<Integer> ids);
}

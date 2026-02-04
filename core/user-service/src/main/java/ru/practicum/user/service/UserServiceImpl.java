package ru.practicum.user.service;

import interaction.exceptions.exception.ConflictException;
import interaction.exceptions.exception.NotFoundException;
import interaction.model.user.AdminUserParam;
import interaction.model.user.NewUserRequest;
import interaction.model.user.UserDto;
import interaction.model.user.UserShortDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;

    @Override
    @Transactional
    public UserDto create(NewUserRequest newUserRequest) {
        if (userRepository.existsByEmail(newUserRequest.getEmail())) {
            throw new ConflictException("User with email %s already exists."
                    .formatted(newUserRequest.getEmail()));
        }
        User user = UserMapper.toUser(newUserRequest);
        return UserMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsers(AdminUserParam param) {
        // если ids переданы — возвращаем конкретных пользователей без пагинации
        if (param.getIds() != null && !param.getIds().isEmpty()) {
            return userRepository.findAllById(param.getIds()).stream()
                    .map(UserMapper::toDto)
                    .toList();
        }
        // from/size -> page/size
        int from = param.getFrom() == null ? 0 : param.getFrom();
        int size = param.getSize() == null ? 10 : param.getSize();
        int page = from / size;

        return userRepository.findAll(PageRequest.of(page, size))
                .map(UserMapper::toDto)
                .getContent();
    }

    @Override
    @Transactional
    public void delete(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=%d was not found".formatted(userId));
        }
        userRepository.deleteById(userId);
    }

    @Override
    public UserShortDto getById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User with id " + id + " not found"));
        return mapper.toShortDto(user);
    }

    @Override
    public List<UserShortDto> getByIds(List<Integer> ids) {
        List<User> foundUsers = userRepository.findAllByIdIn(ids);

        Set<Integer> foundIds = foundUsers.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        List<Integer> notFoundIds = ids.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!notFoundIds.isEmpty()) {
            throw new NotFoundException("Users with IDs " + notFoundIds + " not found.");
        }

        return foundUsers.stream()
                .map(mapper::toShortDto)
                .toList();
    }
}

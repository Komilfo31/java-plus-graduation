package ru.practicum.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.handling.exception.ConflictException;
import ru.practicum.handling.exception.NotFoundException;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.model.User;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

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
}

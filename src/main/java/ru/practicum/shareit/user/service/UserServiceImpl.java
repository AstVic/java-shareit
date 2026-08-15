package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public UserDto create(UserDto userDto) {
        checkUser(userDto);

        User user = UserMapper.toUser(userDto);
        user = userRepository.save(user);
        return UserMapper.toUserDto(user);
    }

    @Override
    public UserDto getById(long userId) {
        checkById(userId);

        return UserMapper.toUserDto(userRepository.findById(userId));
    }

    @Override
    public Collection<UserDto> getAll() {
        return userRepository.findAll().stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    public UserDto update(long userId, UserDto userDto) {
        checkById(userId);

        User user = userRepository.findById(userId);
        if (userDto.getName() != null) {
            user.setName(userDto.getName());
        }
        if (userDto.getEmail() != null) {
            if (!userDto.getEmail().equals(user.getEmail())
                    && userRepository.existsByEmail(userDto.getEmail())) {
                throw new ConflictException("Пользователь с таким email уже существует");
            }
            user.setEmail(userDto.getEmail());
        }
        User updatedUser = userRepository.update(user);
        return UserMapper.toUserDto(updatedUser);
    }

    @Override
    public void delete(long userId) {
        checkById(userId);

        userRepository.delete(userId);
    }

    private void checkById(long userId) {
        if (userRepository.findById(userId) == null) {
            throw new NotFoundException("Пользователя с таким id нет");
        }
    }

    private void checkUser(UserDto userDto) {
        if (userDto == null) {
            throw new ValidationException("Пользователь не может быть null");
        }
        if (userDto.getName() == null || userDto.getEmail() == null) {
            throw new ValidationException("Поля имени и почты должны содержать значения");
        }
        if (!userDto.getEmail().contains("@")) {
            throw new ValidationException("Неверный формат почты");
        }
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }
    }
}

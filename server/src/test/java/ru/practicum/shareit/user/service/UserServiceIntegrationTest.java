package ru.practicum.shareit.user.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createPersistsUser() {
        UserDto created = userService.create(new UserDto(null, "Виктория", "vika@example.com"));

        assertThat(created.getId()).isNotNull();
        assertThat(userRepository.findById(created.getId())).isPresent();
    }

    @Test
    void createRejectsDuplicateEmail() {
        userService.create(new UserDto(null, "Виктория", "dup@example.com"));

        assertThrows(ConflictException.class,
                () -> userService.create(new UserDto(null, "Другая", "dup@example.com")));
    }

    @Test
    void updateChangesStoredUser() {
        UserDto created = userService.create(new UserDto(null, "Виктория", "update@example.com"));

        userService.update(created.getId(), new UserDto(null, "Вика", "vika.new@example.com"));

        UserDto stored = userService.getById(created.getId());
        assertThat(stored.getName()).isEqualTo("Вика");
        assertThat(stored.getEmail()).isEqualTo("vika.new@example.com");
    }

    @Test
    void getAllReturnsCreatedUsers() {
        userService.create(new UserDto(null, "Первый", "first@example.com"));
        userService.create(new UserDto(null, "Второй", "second@example.com"));

        assertThat(userService.getAll()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void deleteRemovesStoredUser() {
        UserDto created = userService.create(new UserDto(null, "Удаляемый", "delete@example.com"));

        userService.delete(created.getId());

        assertThrows(NotFoundException.class, () -> userService.getById(created.getId()));
    }
}

package ru.practicum.shareit.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(1L, "Виктория", "vika@example.com");
    }

    @Test
    void createSavesUserAndReturnsDto() {
        when(userRepository.existsByEmail("vika@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto result = userService.create(new UserDto(null, "Виктория", "vika@example.com"));

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Виктория");
        assertThat(result.getEmail()).isEqualTo("vika@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createThrowsWhenDtoIsNull() {
        assertThrows(ValidationException.class, () -> userService.create(null));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createThrowsWhenNameIsMissing() {
        assertThrows(ValidationException.class,
                () -> userService.create(new UserDto(null, null, "vika@example.com")));
    }

    @Test
    void createThrowsWhenEmailIsMissing() {
        assertThrows(ValidationException.class,
                () -> userService.create(new UserDto(null, "Виктория", null)));
    }

    @Test
    void createThrowsWhenEmailHasNoAtSign() {
        assertThrows(ValidationException.class,
                () -> userService.create(new UserDto(null, "Виктория", "vika.example.com")));
    }

    @Test
    void createThrowsWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("vika@example.com")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> userService.create(new UserDto(null, "Виктория", "vika@example.com")));
    }

    @Test
    void getByIdReturnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(userService.getById(1L).getEmail()).isEqualTo("vika@example.com");
    }

    @Test
    void getByIdThrowsWhenUserIsMissing() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getById(42L));
    }

    @Test
    void getAllReturnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        assertThat(userService.getAll()).hasSize(1);
    }

    @Test
    void updateChangesNameOnly() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDto result = userService.update(1L, new UserDto(null, "Вика", null));

        assertThat(result.getName()).isEqualTo("Вика");
        assertThat(result.getEmail()).isEqualTo("vika@example.com");
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void updateChangesEmail() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        UserDto result = userService.update(1L, new UserDto(null, null, "new@example.com"));

        assertThat(result.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void updateKeepsSameEmailWithoutConflictCheck() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDto result = userService.update(1L, new UserDto(null, null, "vika@example.com"));

        assertThat(result.getEmail()).isEqualTo("vika@example.com");
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void updateThrowsWhenEmailBelongsToAnotherUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> userService.update(1L, new UserDto(null, null, "taken@example.com")));
    }

    @Test
    void updateThrowsWhenUserIsMissing() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.update(42L, new UserDto()));
    }

    @Test
    void deleteRemovesUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenUserIsMissing() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.delete(42L));
        verify(userRepository, never()).deleteById(anyLong());
    }
}

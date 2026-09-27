package ru.practicum.shareit.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private final UserDto userDto = new UserDto(1L, "Виктория", "vika@example.com");

    @Test
    void createReturnsCreatedUser() throws Exception {
        when(userService.create(any(UserDto.class))).thenReturn(userDto);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Виктория"))
                .andExpect(jsonPath("$.email").value("vika@example.com"));
    }

    @Test
    void createReturnsBadRequestOnValidationError() throws Exception {
        when(userService.create(any(UserDto.class))).thenThrow(new ValidationException("Неверный формат почты"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserDto(null, "Виктория", "bad"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Неверный формат почты"));
    }

    @Test
    void createReturnsConflictOnDuplicateEmail() throws Exception {
        when(userService.create(any(UserDto.class)))
                .thenThrow(new ConflictException("Пользователь с таким email уже существует"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isConflict());
    }

    @Test
    void getByIdReturnsUser() throws Exception {
        when(userService.getById(1L)).thenReturn(userDto);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("vika@example.com"));
    }

    @Test
    void getByIdReturnsNotFoundForMissingUser() throws Exception {
        when(userService.getById(anyLong())).thenThrow(new NotFoundException("Пользователя с таким id нет"));

        mockMvc.perform(get("/users/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Пользователя с таким id нет"));
    }

    @Test
    void getAllReturnsUsers() throws Exception {
        when(userService.getAll()).thenReturn(List.of(userDto));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void updateReturnsUpdatedUser() throws Exception {
        UserDto updated = new UserDto(1L, "Вика", "vika@example.com");
        when(userService.update(anyLong(), any(UserDto.class))).thenReturn(updated);

        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Вика"));
    }

    @Test
    void deleteRemovesUser() throws Exception {
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isOk());

        verify(userService).delete(1L);
    }
}

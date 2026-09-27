package ru.practicum.shareit.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
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
    private UserClient userClient;

    @Test
    void createPassesValidUserToServer() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(Map.of("id", 1, "name", "Виктория"));
        when(userClient.createUser(any(UserDto.class))).thenReturn(response);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserDto(null, "Виктория", "vika@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createRejectsBlankName() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserDto(null, "  ", "vika@example.com"))))
                .andExpect(status().isBadRequest());

        verify(userClient, never()).createUser(any(UserDto.class));
    }

    @Test
    void createRejectsMalformedEmail() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserDto(null, "Виктория", "vika.example.com"))))
                .andExpect(status().isBadRequest());

        verify(userClient, never()).createUser(any(UserDto.class));
    }

    @Test
    void createRejectsMissingEmail() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserDto(null, "Виктория", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAcceptsNameOnly() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(Map.of("id", 1, "name", "Вика"));
        when(userClient.updateUser(anyLong(), any(UserUpdateDto.class))).thenReturn(response);

        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Вика\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Вика"));
    }

    @Test
    void updateAcceptsEmailOnly() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(Map.of("id", 1));
        when(userClient.updateUser(anyLong(), any(UserUpdateDto.class))).thenReturn(response);

        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateRejectsMalformedEmail() throws Exception {
        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"broken\"}"))
                .andExpect(status().isBadRequest());

        verify(userClient, never()).updateUser(anyLong(), any(UserUpdateDto.class));
    }

    @Test
    void getByIdDelegatesToClient() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(Map.of("id", 1));
        when(userClient.getUser(1L)).thenReturn(response);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getAllDelegatesToClient() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(List.of(Map.of("id", 1)));
        when(userClient.getUsers()).thenReturn(response);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deleteDelegatesToClient() throws Exception {
        when(userClient.deleteUser(1L)).thenReturn(ResponseEntity.ok().build());

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isOk());

        verify(userClient).deleteUser(1L);
    }
}

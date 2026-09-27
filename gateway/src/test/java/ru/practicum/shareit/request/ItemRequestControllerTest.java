package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemRequestClient itemRequestClient;

    @Test
    void createPassesValidRequestToServer() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(Map.of("id", 5, "description", "Нужна дрель"));
        when(itemRequestClient.createRequest(anyLong(), any(ItemRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/requests")
                        .header(USER_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemRequestDto("Нужна дрель"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void createRejectsBlankDescription() throws Exception {
        mockMvc.perform(post("/requests")
                        .header(USER_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemRequestDto("   "))))
                .andExpect(status().isBadRequest());

        verify(itemRequestClient, never()).createRequest(anyLong(), any(ItemRequestDto.class));
    }

    @Test
    void createRejectsMissingDescription() throws Exception {
        mockMvc.perform(post("/requests")
                        .header(USER_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsMissingUserHeader() throws Exception {
        mockMvc.perform(post("/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemRequestDto("Нужна дрель"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOwnDelegatesToClient() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(List.of(Map.of("id", 5)));
        when(itemRequestClient.getOwnRequests(1L)).thenReturn(response);

        mockMvc.perform(get("/requests").header(USER_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAllDelegatesToClient() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(List.of(Map.of("id", 5)));
        when(itemRequestClient.getAllRequests(2L)).thenReturn(response);

        mockMvc.perform(get("/requests/all").header(USER_HEADER, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void getByIdDelegatesToClient() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(Map.of("id", 5));
        when(itemRequestClient.getRequest(2L, 5L)).thenReturn(response);

        mockMvc.perform(get("/requests/5").header(USER_HEADER, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }
}

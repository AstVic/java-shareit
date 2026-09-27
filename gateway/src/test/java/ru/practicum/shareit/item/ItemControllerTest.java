package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemClient itemClient;

    @Test
    void createPassesValidItemToServer() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(Map.of("id", 10, "name", "Дрель"));
        when(itemClient.createItem(anyLong(), any(ItemDto.class))).thenReturn(response);

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemDto(null, "Дрель", "Аккумуляторная дрель", true, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void createAcceptsRequestId() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(Map.of("id", 10, "requestId", 5));
        when(itemClient.createItem(anyLong(), any(ItemDto.class))).thenReturn(response);

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemDto(null, "Дрель", "Аккумуляторная дрель", true, 5L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(5));
    }

    @Test
    void createRejectsBlankName() throws Exception {
        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemDto(null, " ", "Аккумуляторная дрель", true, null))))
                .andExpect(status().isBadRequest());

        verify(itemClient, never()).createItem(anyLong(), any(ItemDto.class));
    }

    @Test
    void createRejectsMissingDescription() throws Exception {
        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemDto(null, "Дрель", null, true, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsMissingAvailable() throws Exception {
        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemDto(null, "Дрель", "Аккумуляторная дрель", null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsMissingUserHeader() throws Exception {
        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemDto(null, "Дрель", "Аккумуляторная дрель", true, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAcceptsPartialPayload() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(Map.of("id", 10, "name", "Перфоратор"));
        when(itemClient.updateItem(anyLong(), anyLong(), any(ItemUpdateDto.class))).thenReturn(response);

        mockMvc.perform(patch("/items/10")
                        .header(USER_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Перфоратор\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Перфоратор"));
    }

    @Test
    void getByIdDelegatesToClient() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(Map.of("id", 10));
        when(itemClient.getItem(1L, 10L)).thenReturn(response);

        mockMvc.perform(get("/items/10").header(USER_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void getAllByOwnerDelegatesToClient() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(List.of(Map.of("id", 10)));
        when(itemClient.getItems(1L)).thenReturn(response);

        mockMvc.perform(get("/items").header(USER_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void searchDelegatesToClient() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(List.of(Map.of("id", 10)));
        when(itemClient.searchItems(anyLong(), anyString())).thenReturn(response);

        mockMvc.perform(get("/items/search").header(USER_HEADER, 1L).param("text", "дрель"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void addCommentPassesValidComment() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(Map.of("id", 1000, "text", "Отличная дрель"));
        when(itemClient.addComment(anyLong(), anyLong(), any(CommentDto.class))).thenReturn(response);

        mockMvc.perform(post("/items/10/comment")
                        .header(USER_HEADER, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentDto("Отличная дрель"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Отличная дрель"));
    }

    @Test
    void addCommentRejectsBlankText() throws Exception {
        mockMvc.perform(post("/items/10/comment")
                        .header(USER_HEADER, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentDto("   "))))
                .andExpect(status().isBadRequest());

        verify(itemClient, never()).addComment(anyLong(), anyLong(), any(CommentDto.class));
    }
}

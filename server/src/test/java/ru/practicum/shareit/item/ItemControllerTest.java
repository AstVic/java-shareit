package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.dto.ItemWithCommentsDto;
import ru.practicum.shareit.item.service.ItemService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
    private ItemService itemService;

    private final ItemDto itemDto = new ItemDto(10L, "Дрель", "Аккумуляторная дрель", true, null);

    @Test
    void createReturnsCreatedItem() throws Exception {
        when(itemService.create(anyLong(), any(ItemDto.class))).thenReturn(itemDto);

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Дрель"));
    }

    @Test
    void createReturnsItemWithRequestId() throws Exception {
        ItemDto answer = new ItemDto(10L, "Дрель", "Аккумуляторная дрель", true, 5L);
        when(itemService.create(anyLong(), any(ItemDto.class))).thenReturn(answer);

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(5));
    }

    @Test
    void createReturnsBadRequestOnValidationError() throws Exception {
        when(itemService.create(anyLong(), any(ItemDto.class)))
                .thenThrow(new ValidationException("Название вещи обязательно"));

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemDto())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReturnsUpdatedItem() throws Exception {
        ItemDto updated = new ItemDto(10L, "Перфоратор", "Мощный", false, null);
        when(itemService.update(anyLong(), anyLong(), any(ItemDto.class))).thenReturn(updated);

        mockMvc.perform(patch("/items/10")
                        .header(USER_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Перфоратор"))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void updateReturnsNotFoundForStranger() throws Exception {
        when(itemService.update(anyLong(), anyLong(), any(ItemDto.class)))
                .thenThrow(new NotFoundException("Редактировать вещь может только её владелец"));

        mockMvc.perform(patch("/items/10")
                        .header(USER_HEADER, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByIdReturnsItemWithComments() throws Exception {
        ItemWithCommentsDto dto = new ItemWithCommentsDto(10L, "Дрель", "Аккумуляторная дрель", true,
                null, null, null, List.of());
        when(itemService.getById(anyLong(), anyLong())).thenReturn(dto);

        mockMvc.perform(get("/items/10").header(USER_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.comments").isArray());
    }

    @Test
    void getAllByOwnerReturnsItems() throws Exception {
        ItemWithBookingsDto dto = new ItemWithBookingsDto(10L, "Дрель", "Аккумуляторная дрель", true,
                null, null, null, List.of());
        when(itemService.getAllByOwner(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/items").header(USER_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void searchReturnsItems() throws Exception {
        when(itemService.search(anyString())).thenReturn(List.of(itemDto));

        mockMvc.perform(get("/items/search").param("text", "дрель"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void addCommentReturnsComment() throws Exception {
        CommentDto commentDto = new CommentDto(1000L, "Отличная дрель", "Арендатор", LocalDateTime.now());
        when(itemService.addComment(anyLong(), anyLong(), any(CommentCreateDto.class))).thenReturn(commentDto);

        mockMvc.perform(post("/items/10/comment")
                        .header(USER_HEADER, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentCreateDto("Отличная дрель"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Отличная дрель"))
                .andExpect(jsonPath("$.authorName").value("Арендатор"));
    }

    @Test
    void addCommentReturnsBadRequestWithoutFinishedBooking() throws Exception {
        when(itemService.addComment(anyLong(), anyLong(), any(CommentCreateDto.class)))
                .thenThrow(new ValidationException("Оставить отзыв может только пользователь с завершённой арендой"));

        mockMvc.perform(post("/items/10/comment")
                        .header(USER_HEADER, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentCreateDto("Отличная дрель"))))
                .andExpect(status().isBadRequest());
    }
}

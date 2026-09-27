package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemForRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private ItemRequestService itemRequestService;

    private final ItemRequestDto requestDto = new ItemRequestDto(5L, "Нужна дрель",
            LocalDateTime.now().withNano(0), List.of(new ItemForRequestDto(10L, "Дрель", 2L)));

    @Test
    void createReturnsRequest() throws Exception {
        when(itemRequestService.create(anyLong(), any(ItemRequestCreateDto.class))).thenReturn(requestDto);

        mockMvc.perform(post("/requests")
                        .header(USER_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemRequestCreateDto("Нужна дрель"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.description").value("Нужна дрель"))
                .andExpect(jsonPath("$.created").exists());
    }

    @Test
    void createReturnsBadRequestForBlankDescription() throws Exception {
        when(itemRequestService.create(anyLong(), any(ItemRequestCreateDto.class)))
                .thenThrow(new ValidationException("Описание запроса обязательно"));

        mockMvc.perform(post("/requests")
                        .header(USER_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemRequestCreateDto(" "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Описание запроса обязательно"));
    }

    @Test
    void getOwnReturnsRequestsWithAnswers() throws Exception {
        when(itemRequestService.getOwn(1L)).thenReturn(List.of(requestDto));

        mockMvc.perform(get("/requests").header(USER_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].items.length()").value(1))
                .andExpect(jsonPath("$[0].items[0].id").value(10))
                .andExpect(jsonPath("$[0].items[0].name").value("Дрель"))
                .andExpect(jsonPath("$[0].items[0].ownerId").value(2));
    }

    @Test
    void getAllReturnsRequestsOfOtherUsers() throws Exception {
        when(itemRequestService.getAll(2L)).thenReturn(List.of(requestDto));

        mockMvc.perform(get("/requests/all").header(USER_HEADER, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void getByIdReturnsRequest() throws Exception {
        when(itemRequestService.getById(2L, 5L)).thenReturn(requestDto);

        mockMvc.perform(get("/requests/5").header(USER_HEADER, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Нужна дрель"));
    }

    @Test
    void getByIdReturnsNotFoundForMissingRequest() throws Exception {
        when(itemRequestService.getById(anyLong(), anyLong()))
                .thenThrow(new NotFoundException("Запроса с таким id нет"));

        mockMvc.perform(get("/requests/99").header(USER_HEADER, 1L))
                .andExpect(status().isNotFound());
    }
}

package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemShortDto;
import ru.practicum.shareit.user.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    private final LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
    private final LocalDateTime end = LocalDateTime.now().plusDays(2).withNano(0);

    private BookingDto bookingDto(BookingStatus status) {
        return new BookingDto(100L, start, end,
                new ItemShortDto(10L, "Дрель"),
                new UserShortDto(2L, "Арендатор"),
                status);
    }

    @Test
    void createReturnsBooking() throws Exception {
        when(bookingService.create(anyLong(), any(BookingCreateDto.class)))
                .thenReturn(bookingDto(BookingStatus.WAITING));

        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingCreateDto(10L, start, end))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.item.name").value("Дрель"))
                .andExpect(jsonPath("$.booker.id").value(2));
    }

    @Test
    void createReturnsBadRequestForUnavailableItem() throws Exception {
        when(bookingService.create(anyLong(), any(BookingCreateDto.class)))
                .thenThrow(new ValidationException("Вещь недоступна для бронирования"));

        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingCreateDto(10L, start, end))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveReturnsApprovedBooking() throws Exception {
        when(bookingService.approve(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(bookingDto(BookingStatus.APPROVED));

        mockMvc.perform(patch("/bookings/100").header(USER_HEADER, 1L).param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approveReturnsForbiddenForNonOwner() throws Exception {
        when(bookingService.approve(anyLong(), anyLong(), anyBoolean()))
                .thenThrow(new ForbiddenException("Подтверждать бронирование может только владелец вещи"));

        mockMvc.perform(patch("/bookings/100").header(USER_HEADER, 2L).param("approved", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByIdReturnsBooking() throws Exception {
        when(bookingService.getById(anyLong(), anyLong())).thenReturn(bookingDto(BookingStatus.WAITING));

        mockMvc.perform(get("/bookings/100").header(USER_HEADER, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    void getByIdReturnsNotFoundForStranger() throws Exception {
        when(bookingService.getById(anyLong(), anyLong()))
                .thenThrow(new NotFoundException("Просматривать бронирование может только его автор"));

        mockMvc.perform(get("/bookings/100").header(USER_HEADER, 3L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllByBookerReturnsBookings() throws Exception {
        when(bookingService.getAllByBooker(anyLong(), anyString()))
                .thenReturn(List.of(bookingDto(BookingStatus.WAITING)));

        mockMvc.perform(get("/bookings").header(USER_HEADER, 2L).param("state", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAllByBookerReturnsBadRequestForUnknownState() throws Exception {
        when(bookingService.getAllByBooker(anyLong(), anyString()))
                .thenThrow(new ValidationException("Unknown state: UNSUPPORTED"));

        mockMvc.perform(get("/bookings").header(USER_HEADER, 2L).param("state", "UNSUPPORTED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown state: UNSUPPORTED"));
    }

    @Test
    void getAllByOwnerReturnsBookings() throws Exception {
        when(bookingService.getAllByOwner(anyLong(), anyString()))
                .thenReturn(List.of(bookingDto(BookingStatus.APPROVED)));

        mockMvc.perform(get("/bookings/owner").header(USER_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }
}

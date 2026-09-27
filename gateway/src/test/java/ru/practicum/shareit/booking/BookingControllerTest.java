package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookItemRequestDto;
import ru.practicum.shareit.booking.dto.BookingState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    private BookingClient bookingClient;

    private final LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
    private final LocalDateTime end = LocalDateTime.now().plusDays(2).withNano(0);

    @Test
    void bookItemPassesValidBookingToServer() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(Map.of("id", 100, "status", "WAITING"));
        when(bookingClient.bookItem(anyLong(), any(BookItemRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookItemRequestDto(10L, start, end))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    void bookItemRejectsPastStart() throws Exception {
        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookItemRequestDto(
                                10L, LocalDateTime.now().minusDays(2), LocalDateTime.now().plusDays(1)))))
                .andExpect(status().isBadRequest());

        verify(bookingClient, never()).bookItem(anyLong(), any(BookItemRequestDto.class));
    }

    @Test
    void bookItemRejectsMissingDates() throws Exception {
        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":10}"))
                .andExpect(status().isBadRequest());

        verify(bookingClient, never()).bookItem(anyLong(), any(BookItemRequestDto.class));
    }

    @Test
    void bookItemRejectsMissingItemId() throws Exception {
        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookItemRequestDto(null, start, end))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveDelegatesToClient() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(Map.of("id", 100, "status", "APPROVED"));
        when(bookingClient.approveBooking(anyLong(), anyLong(), anyBoolean())).thenReturn(response);

        mockMvc.perform(patch("/bookings/100").header(USER_HEADER, 1L).param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void getBookingDelegatesToClient() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(Map.of("id", 100));
        when(bookingClient.getBooking(2L, 100L)).thenReturn(response);

        mockMvc.perform(get("/bookings/100").header(USER_HEADER, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    void getBookingsUsesDefaultState() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(List.of(Map.of("id", 100)));
        when(bookingClient.getBookings(anyLong(), any(BookingState.class), anyInt(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/bookings").header(USER_HEADER, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(bookingClient).getBookings(2L, BookingState.ALL, 0, 10);
    }

    @Test
    void getBookingsRejectsUnknownState() throws Exception {
        mockMvc.perform(get("/bookings").header(USER_HEADER, 2L).param("state", "UNSUPPORTED"))
                .andExpect(status().isBadRequest());

        verify(bookingClient, never()).getBookings(anyLong(), any(BookingState.class), anyInt(), anyInt());
    }

    @Test
    void getBookingsRejectsNegativeFrom() throws Exception {
        mockMvc.perform(get("/bookings").header(USER_HEADER, 2L).param("from", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBookingsRejectsZeroSize() throws Exception {
        mockMvc.perform(get("/bookings").header(USER_HEADER, 2L).param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOwnerBookingsDelegatesToClient() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(List.of(Map.of("id", 100)));
        when(bookingClient.getOwnerBookings(anyLong(), any(BookingState.class), anyInt(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/bookings/owner").header(USER_HEADER, 1L).param("state", "waiting"))
                .andExpect(status().isOk());

        verify(bookingClient).getOwnerBookings(1L, BookingState.WAITING, 0, 10);
    }

    @Test
    void bookingStateParsesKnownNamesIgnoringCase() {
        assertThat(BookingState.from("current")).contains(BookingState.CURRENT);
        assertThat(BookingState.from("PAST")).contains(BookingState.PAST);
        assertThat(BookingState.from("rejected")).contains(BookingState.REJECTED);
        assertThat(BookingState.from("future")).contains(BookingState.FUTURE);
        assertThat(BookingState.from("nonsense")).isEmpty();
    }
}

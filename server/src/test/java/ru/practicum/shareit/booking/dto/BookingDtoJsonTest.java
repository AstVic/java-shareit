package ru.practicum.shareit.booking.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.item.dto.ItemShortDto;
import ru.practicum.shareit.user.dto.UserShortDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class BookingDtoJsonTest {

    @Autowired
    private JacksonTester<BookingDto> bookingDtoJson;

    @Autowired
    private JacksonTester<BookingCreateDto> bookingCreateDtoJson;

    @Autowired
    private JacksonTester<BookingShortDto> bookingShortDtoJson;

    @Test
    void serializesDatesInIsoFormat() throws Exception {
        BookingDto dto = new BookingDto(
                100L,
                LocalDateTime.of(2026, 10, 1, 12, 0, 0),
                LocalDateTime.of(2026, 10, 2, 12, 0, 0),
                new ItemShortDto(10L, "Дрель"),
                new UserShortDto(2L, "Арендатор"),
                BookingStatus.WAITING);

        assertThat(bookingDtoJson.write(dto)).extractingJsonPathNumberValue("$.id").isEqualTo(100);
        assertThat(bookingDtoJson.write(dto)).extractingJsonPathStringValue("$.start")
                .isEqualTo("2026-10-01T12:00:00");
        assertThat(bookingDtoJson.write(dto)).extractingJsonPathStringValue("$.end")
                .isEqualTo("2026-10-02T12:00:00");
        assertThat(bookingDtoJson.write(dto)).extractingJsonPathStringValue("$.status").isEqualTo("WAITING");
        assertThat(bookingDtoJson.write(dto)).extractingJsonPathStringValue("$.item.name").isEqualTo("Дрель");
        assertThat(bookingDtoJson.write(dto)).extractingJsonPathNumberValue("$.booker.id").isEqualTo(2);
    }

    @Test
    void deserializesDatesFromIsoFormat() throws Exception {
        String json = "{\"itemId\":10,\"start\":\"2026-10-01T12:00:00\",\"end\":\"2026-10-02T12:00:00\"}";

        BookingCreateDto dto = bookingCreateDtoJson.parseObject(json);

        assertThat(dto.getItemId()).isEqualTo(10L);
        assertThat(dto.getStart()).isEqualTo(LocalDateTime.of(2026, 10, 1, 12, 0, 0));
        assertThat(dto.getEnd()).isEqualTo(LocalDateTime.of(2026, 10, 2, 12, 0, 0));
    }

    @Test
    void serializesShortBooking() throws Exception {
        BookingShortDto dto = new BookingShortDto(100L, 2L);

        assertThat(bookingShortDtoJson.write(dto)).extractingJsonPathNumberValue("$.id").isEqualTo(100);
        assertThat(bookingShortDtoJson.write(dto)).extractingJsonPathNumberValue("$.bookerId").isEqualTo(2);
    }
}

package ru.practicum.shareit.item.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.booking.dto.BookingShortDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemDtoJsonTest {

    @Autowired
    private JacksonTester<ItemDto> itemDtoJson;

    @Autowired
    private JacksonTester<ItemWithBookingsDto> itemWithBookingsDtoJson;

    @Autowired
    private JacksonTester<ItemWithCommentsDto> itemWithCommentsDtoJson;

    @Autowired
    private JacksonTester<CommentDto> commentDtoJson;

    @Autowired
    private JacksonTester<CommentCreateDto> commentCreateDtoJson;

    @Test
    void serializesItemWithRequestId() throws Exception {
        ItemDto dto = new ItemDto(10L, "Дрель", "Аккумуляторная дрель", true, 5L);

        assertThat(itemDtoJson.write(dto)).extractingJsonPathNumberValue("$.id").isEqualTo(10);
        assertThat(itemDtoJson.write(dto)).extractingJsonPathStringValue("$.name").isEqualTo("Дрель");
        assertThat(itemDtoJson.write(dto)).extractingJsonPathBooleanValue("$.available").isTrue();
        assertThat(itemDtoJson.write(dto)).extractingJsonPathNumberValue("$.requestId").isEqualTo(5);
    }

    @Test
    void deserializesItemWithRequestId() throws Exception {
        String json = "{\"name\":\"Дрель\",\"description\":\"Аккумуляторная дрель\","
                + "\"available\":true,\"requestId\":5}";

        ItemDto dto = itemDtoJson.parseObject(json);

        assertThat(dto.getName()).isEqualTo("Дрель");
        assertThat(dto.getAvailable()).isTrue();
        assertThat(dto.getRequestId()).isEqualTo(5L);
    }

    @Test
    void deserializesItemWithoutRequestId() throws Exception {
        ItemDto dto = itemDtoJson.parseObject(
                "{\"name\":\"Дрель\",\"description\":\"Аккумуляторная дрель\",\"available\":true}");

        assertThat(dto.getRequestId()).isNull();
    }

    @Test
    void serializesItemWithBookings() throws Exception {
        ItemWithBookingsDto dto = new ItemWithBookingsDto(10L, "Дрель", "Аккумуляторная дрель", true, 5L,
                new BookingShortDto(100L, 2L), new BookingShortDto(101L, 3L), List.of());

        assertThat(itemWithBookingsDtoJson.write(dto)).extractingJsonPathNumberValue("$.lastBooking.id")
                .isEqualTo(100);
        assertThat(itemWithBookingsDtoJson.write(dto)).extractingJsonPathNumberValue("$.nextBooking.bookerId")
                .isEqualTo(3);
        assertThat(itemWithBookingsDtoJson.write(dto)).extractingJsonPathNumberValue("$.requestId").isEqualTo(5);
    }

    @Test
    void serializesItemWithComments() throws Exception {
        CommentDto comment = new CommentDto(1000L, "Отличная дрель", "Арендатор",
                LocalDateTime.of(2026, 9, 27, 23, 45, 0));
        ItemWithCommentsDto dto = new ItemWithCommentsDto(10L, "Дрель", "Аккумуляторная дрель", true, null,
                null, null, List.of(comment));

        assertThat(itemWithCommentsDtoJson.write(dto)).extractingJsonPathStringValue("$.comments[0].authorName")
                .isEqualTo("Арендатор");
        assertThat(itemWithCommentsDtoJson.write(dto)).extractingJsonPathStringValue("$.comments[0].created")
                .isEqualTo("2026-09-27T23:45:00");
    }

    @Test
    void serializesComment() throws Exception {
        CommentDto dto = new CommentDto(1000L, "Отличная дрель", "Арендатор",
                LocalDateTime.of(2026, 9, 27, 23, 45, 0));

        assertThat(commentDtoJson.write(dto)).extractingJsonPathStringValue("$.text").isEqualTo("Отличная дрель");
        assertThat(commentDtoJson.write(dto)).extractingJsonPathStringValue("$.created")
                .isEqualTo("2026-09-27T23:45:00");
    }

    @Test
    void deserializesCommentCreateDto() throws Exception {
        CommentCreateDto dto = commentCreateDtoJson.parseObject("{\"text\":\"Отличная дрель\"}");

        assertThat(dto.getText()).isEqualTo("Отличная дрель");
    }

    @Test
    void serializesItemForRequest() throws Exception {
        ItemForRequestDto dto = new ItemForRequestDto(10L, "Дрель", 2L);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getName()).isEqualTo("Дрель");
        assertThat(dto.getOwnerId()).isEqualTo(2L);
    }
}

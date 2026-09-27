package ru.practicum.shareit.request.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.item.dto.ItemForRequestDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemRequestDtoJsonTest {

    @Autowired
    private JacksonTester<ItemRequestDto> itemRequestDtoJson;

    @Autowired
    private JacksonTester<ItemRequestCreateDto> itemRequestCreateDtoJson;

    @Test
    void serializesRequestWithAnswers() throws Exception {
        ItemRequestDto dto = new ItemRequestDto(
                5L,
                "Нужна дрель",
                LocalDateTime.of(2026, 9, 27, 23, 45, 0),
                List.of(new ItemForRequestDto(10L, "Дрель", 2L)));

        assertThat(itemRequestDtoJson.write(dto)).extractingJsonPathNumberValue("$.id").isEqualTo(5);
        assertThat(itemRequestDtoJson.write(dto)).extractingJsonPathStringValue("$.description")
                .isEqualTo("Нужна дрель");
        assertThat(itemRequestDtoJson.write(dto)).extractingJsonPathStringValue("$.created")
                .isEqualTo("2026-09-27T23:45:00");
        assertThat(itemRequestDtoJson.write(dto)).extractingJsonPathNumberValue("$.items[0].id").isEqualTo(10);
        assertThat(itemRequestDtoJson.write(dto)).extractingJsonPathStringValue("$.items[0].name")
                .isEqualTo("Дрель");
        assertThat(itemRequestDtoJson.write(dto)).extractingJsonPathNumberValue("$.items[0].ownerId").isEqualTo(2);
    }

    @Test
    void serializesEmptyAnswersAsArray() throws Exception {
        ItemRequestDto dto = new ItemRequestDto(5L, "Нужна дрель", LocalDateTime.now(), List.of());

        assertThat(itemRequestDtoJson.write(dto)).extractingJsonPathArrayValue("$.items").isEmpty();
    }

    @Test
    void deserializesCreateDto() throws Exception {
        ItemRequestCreateDto dto = itemRequestCreateDtoJson.parseObject("{\"description\":\"Нужна дрель\"}");

        assertThat(dto.getDescription()).isEqualTo("Нужна дрель");
    }
}

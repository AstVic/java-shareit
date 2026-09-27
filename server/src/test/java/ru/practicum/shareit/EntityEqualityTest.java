package ru.practicum.shareit;

import org.junit.jupiter.api.Test;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.item.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.user.User;

import static org.assertj.core.api.Assertions.assertThat;

class EntityEqualityTest {

    @Test
    void usersWithSameIdAreEqual() {
        User first = new User(1L, "Виктория", "vika@example.com");
        User second = new User(1L, "Другое имя", "other@example.com");
        User third = new User(2L, "Виктория", "vika@example.com");
        User withoutId = new User(null, "Виктория", "vika@example.com");

        assertThat(first).isEqualTo(first);
        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(third);
        assertThat(first).isNotEqualTo("не пользователь");
        assertThat(withoutId).isNotEqualTo(first);
        assertThat(first).hasSameHashCodeAs(second);
    }

    @Test
    void itemsWithSameIdAreEqual() {
        Item first = item(10L);
        Item second = item(10L);
        Item third = item(11L);
        Item withoutId = item(null);

        assertThat(first).isEqualTo(first);
        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(third);
        assertThat(first).isNotEqualTo("не вещь");
        assertThat(withoutId).isNotEqualTo(first);
        assertThat(first).hasSameHashCodeAs(second);
    }

    @Test
    void bookingsWithSameIdAreEqual() {
        Booking first = new Booking();
        first.setId(100L);
        Booking second = new Booking();
        second.setId(100L);
        Booking third = new Booking();
        third.setId(101L);
        Booking withoutId = new Booking();

        assertThat(first).isEqualTo(first);
        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(third);
        assertThat(first).isNotEqualTo("не бронирование");
        assertThat(withoutId).isNotEqualTo(first);
        assertThat(first).hasSameHashCodeAs(second);
    }

    @Test
    void commentsWithSameIdAreEqual() {
        Comment first = new Comment();
        first.setId(1000L);
        Comment second = new Comment();
        second.setId(1000L);
        Comment third = new Comment();
        third.setId(1001L);
        Comment withoutId = new Comment();

        assertThat(first).isEqualTo(first);
        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(third);
        assertThat(first).isNotEqualTo("не комментарий");
        assertThat(withoutId).isNotEqualTo(first);
        assertThat(first).hasSameHashCodeAs(second);
    }

    @Test
    void requestsWithSameIdAreEqual() {
        ItemRequest first = new ItemRequest();
        first.setId(5L);
        ItemRequest second = new ItemRequest();
        second.setId(5L);
        ItemRequest third = new ItemRequest();
        third.setId(6L);
        ItemRequest withoutId = new ItemRequest();

        assertThat(first).isEqualTo(first);
        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(third);
        assertThat(first).isNotEqualTo("не запрос");
        assertThat(withoutId).isNotEqualTo(first);
        assertThat(first).hasSameHashCodeAs(second);
    }

    private Item item(Long id) {
        Item item = new Item();
        item.setId(id);
        item.setName("Дрель");
        item.setDescription("Аккумуляторная дрель");
        item.setAvailable(true);
        return item;
    }
}

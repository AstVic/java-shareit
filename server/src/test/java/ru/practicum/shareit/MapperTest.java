package ru.practicum.shareit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.item.Comment;
import ru.practicum.shareit.item.CommentMapper;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.dto.ItemWithCommentsDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestMapper;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MapperTest {

    private User owner;
    private User booker;
    private Item item;
    private ItemRequest request;

    @BeforeEach
    void setUp() {
        owner = new User(1L, "Владелец", "owner@example.com");
        booker = new User(2L, "Арендатор", "booker@example.com");

        request = new ItemRequest();
        request.setId(5L);
        request.setDescription("Нужна дрель");
        request.setRequester(booker);
        request.setCreated(LocalDateTime.of(2026, 9, 27, 23, 45, 0));

        item = new Item();
        item.setId(10L);
        item.setName("Дрель");
        item.setDescription("Аккумуляторная дрель");
        item.setAvailable(true);
        item.setOwner(owner);
        item.setRequest(request);
    }

    @Test
    void userMapperConvertsBothWays() {
        UserDto dto = UserMapper.toUserDto(owner);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Владелец");
        assertThat(dto.getEmail()).isEqualTo("owner@example.com");

        User user = UserMapper.toUser(dto);

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getEmail()).isEqualTo("owner@example.com");
        assertThat(new UserMapper()).isNotNull();
    }

    @Test
    void itemMapperConvertsItemWithRequest() {
        ItemDto dto = ItemMapper.toItemDto(item);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getRequestId()).isEqualTo(5L);
    }

    @Test
    void itemMapperLeavesRequestIdNullWhenThereIsNoRequest() {
        item.setRequest(null);

        assertThat(ItemMapper.toItemDto(item).getRequestId()).isNull();
    }

    @Test
    void itemMapperConvertsDtoToItem() {
        Item result = ItemMapper.toItem(new ItemDto(null, "Молоток", "Тяжёлый", false, null));

        assertThat(result.getName()).isEqualTo("Молоток");
        assertThat(result.getDescription()).isEqualTo("Тяжёлый");
        assertThat(result.getAvailable()).isFalse();
        assertThat(new ItemMapper()).isNotNull();
    }

    @Test
    void itemMapperBuildsDtoWithBookings() {
        BookingShortDto last = new BookingShortDto(100L, 2L);
        BookingShortDto next = new BookingShortDto(101L, 3L);
        List<CommentDto> comments = List.of(new CommentDto(1000L, "Отзыв", "Арендатор", LocalDateTime.now()));

        ItemWithBookingsDto withBookings = ItemMapper.toItemWithBookingsDto(item, last, next, comments);
        ItemWithCommentsDto withComments = ItemMapper.toItemWithCommentsDto(item, last, next, comments);

        assertThat(withBookings.getLastBooking()).isEqualTo(last);
        assertThat(withBookings.getNextBooking()).isEqualTo(next);
        assertThat(withBookings.getRequestId()).isEqualTo(5L);
        assertThat(withComments.getComments()).hasSize(1);
        assertThat(withComments.getRequestId()).isEqualTo(5L);
    }

    @Test
    void itemMapperBuildsDtoWithoutRequest() {
        item.setRequest(null);

        assertThat(ItemMapper.toItemWithBookingsDto(item, null, null, List.of()).getRequestId()).isNull();
        assertThat(ItemMapper.toItemWithCommentsDto(item, null, null, List.of()).getRequestId()).isNull();
    }

    @Test
    void commentMapperConvertsBothWays() {
        Comment comment = new Comment();
        comment.setId(1000L);
        comment.setText("Отличная дрель");
        comment.setItem(item);
        comment.setAuthor(booker);
        comment.setCreated(LocalDateTime.of(2026, 9, 27, 23, 45, 0));

        CommentDto dto = CommentMapper.toCommentDto(comment);

        assertThat(dto.getId()).isEqualTo(1000L);
        assertThat(dto.getAuthorName()).isEqualTo("Арендатор");
        assertThat(dto.getCreated()).isEqualTo(LocalDateTime.of(2026, 9, 27, 23, 45, 0));

        Comment created = CommentMapper.toComment(new CommentCreateDto("Новый отзыв"));

        assertThat(created.getText()).isEqualTo("Новый отзыв");
        assertThat(new CommentMapper()).isNotNull();
    }

    @Test
    void bookingMapperConvertsBothWays() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 12, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 2, 12, 0, 0);

        Booking booking = new Booking();
        booking.setId(100L);
        booking.setStart(start);
        booking.setEnd(end);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.APPROVED);

        BookingDto dto = BookingMapper.toBookingDto(booking);

        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getItem().getName()).isEqualTo("Дрель");
        assertThat(dto.getBooker().getName()).isEqualTo("Арендатор");
        assertThat(dto.getStatus()).isEqualTo(BookingStatus.APPROVED);

        Booking created = BookingMapper.toBooking(new BookingCreateDto(10L, start, end));

        assertThat(created.getStart()).isEqualTo(start);
        assertThat(created.getEnd()).isEqualTo(end);
        assertThat(new BookingMapper()).isNotNull();
    }

    @Test
    void itemRequestMapperConvertsBothWays() {
        ItemRequestDto dto = ItemRequestMapper.toItemRequestDto(request, List.of(item));

        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getDescription()).isEqualTo("Нужна дрель");
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getItems().get(0).getOwnerId()).isEqualTo(1L);

        ItemRequest created = ItemRequestMapper.toItemRequest(new ItemRequestCreateDto("Нужен молоток"));

        assertThat(created.getDescription()).isEqualTo("Нужен молоток");
        assertThat(new ItemRequestMapper()).isNotNull();
    }

    @Test
    void itemRequestMapperBuildsDtoWithoutAnswers() {
        assertThat(ItemRequestMapper.toItemRequestDto(request, List.of()).getItems()).isEmpty();
    }
}

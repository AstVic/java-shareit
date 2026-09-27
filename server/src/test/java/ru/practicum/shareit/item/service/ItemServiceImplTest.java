package ru.practicum.shareit.item.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.Comment;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.dto.ItemWithCommentsDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ItemRequestRepository itemRequestRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    private User owner;
    private User booker;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = new User(1L, "Владелец", "owner@example.com");
        booker = new User(2L, "Арендатор", "booker@example.com");

        item = new Item();
        item.setId(10L);
        item.setName("Дрель");
        item.setDescription("Аккумуляторная дрель");
        item.setAvailable(true);
        item.setOwner(owner);
    }

    @Test
    void createSavesItemWithoutRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        ItemDto result = itemService.create(1L, new ItemDto(null, "Дрель", "Аккумуляторная дрель", true, null));

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getRequestId()).isNull();
        verify(itemRequestRepository, never()).findById(anyLong());
    }

    @Test
    void createLinksItemToRequest() {
        ItemRequest request = new ItemRequest();
        request.setId(5L);
        request.setDescription("Нужна дрель");
        request.setRequester(booker);
        request.setCreated(LocalDateTime.now());

        item.setRequest(request);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        ItemDto result = itemService.create(1L, new ItemDto(null, "Дрель", "Аккумуляторная дрель", true, 5L));

        assertThat(result.getRequestId()).isEqualTo(5L);
    }

    @Test
    void createThrowsWhenRequestIsMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> itemService.create(1L, new ItemDto(null, "Дрель", "Описание", true, 99L)));
    }

    @Test
    void createThrowsWhenOwnerIsMissing() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> itemService.create(42L, new ItemDto(null, "Дрель", "Описание", true, null)));
    }

    @Test
    void createThrowsWhenNameIsBlank() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThrows(ValidationException.class,
                () -> itemService.create(1L, new ItemDto(null, "  ", "Описание", true, null)));
    }

    @Test
    void createThrowsWhenDescriptionIsBlank() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThrows(ValidationException.class,
                () -> itemService.create(1L, new ItemDto(null, "Дрель", "", true, null)));
    }

    @Test
    void createThrowsWhenAvailableIsNull() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThrows(ValidationException.class,
                () -> itemService.create(1L, new ItemDto(null, "Дрель", "Описание", null, null)));
    }

    @Test
    void updateChangesAllFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        ItemDto result = itemService.update(1L, 10L,
                new ItemDto(null, "Перфоратор", "Мощный", false, null));

        assertThat(result.getName()).isEqualTo("Перфоратор");
        assertThat(result.getDescription()).isEqualTo("Мощный");
        assertThat(result.getAvailable()).isFalse();
    }

    @Test
    void updateKeepsFieldsWhenDtoIsEmpty() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        ItemDto result = itemService.update(1L, 10L, new ItemDto());

        assertThat(result.getName()).isEqualTo("Дрель");
        assertThat(result.getDescription()).isEqualTo("Аккумуляторная дрель");
        assertThat(result.getAvailable()).isTrue();
    }

    @Test
    void updateThrowsWhenUserIsNotOwner() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(NotFoundException.class, () -> itemService.update(2L, 10L, new ItemDto()));
    }

    @Test
    void updateThrowsWhenItemIsMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.update(1L, 99L, new ItemDto()));
    }

    @Test
    void getByIdReturnsBookingsForOwner() {
        LocalDateTime now = LocalDateTime.now();
        Booking past = booking(100L, now.minusDays(2), now.minusDays(1));
        Booking future = booking(101L, now.plusDays(1), now.plusDays(2));

        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(commentRepository.findAllByItemId(10L)).thenReturn(List.of(comment(now)));
        when(bookingRepository.findAllByItemIdInAndStatus(List.of(10L), BookingStatus.APPROVED))
                .thenReturn(List.of(past, future));

        ItemWithCommentsDto result = itemService.getById(1L, 10L);

        assertThat(result.getLastBooking().getId()).isEqualTo(100L);
        assertThat(result.getNextBooking().getId()).isEqualTo(101L);
        assertThat(result.getComments()).hasSize(1);
    }

    @Test
    void getByIdHidesBookingsFromNonOwner() {
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(commentRepository.findAllByItemId(10L)).thenReturn(List.of());

        ItemWithCommentsDto result = itemService.getById(2L, 10L);

        assertThat(result.getLastBooking()).isNull();
        assertThat(result.getNextBooking()).isNull();
        verify(bookingRepository, never()).findAllByItemIdInAndStatus(anyCollection(), any(BookingStatus.class));
    }

    @Test
    void getByIdHidesBookingsWhenUserIsAnonymous() {
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(commentRepository.findAllByItemId(10L)).thenReturn(List.of());

        ItemWithCommentsDto result = itemService.getById(null, 10L);

        assertThat(result.getLastBooking()).isNull();
    }

    @Test
    void getByIdThrowsWhenItemIsMissing() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.getById(1L, 99L));
    }

    @Test
    void getAllByOwnerReturnsItemsWithBookingsAndComments() {
        LocalDateTime now = LocalDateTime.now();
        Booking past = booking(100L, now.minusDays(2), now.minusDays(1));

        when(itemRepository.findAllByOwnerIdOrderByIdAsc(1L)).thenReturn(List.of(item));
        when(bookingRepository.findAllByItemIdInAndStatus(List.of(10L), BookingStatus.APPROVED))
                .thenReturn(List.of(past));
        when(commentRepository.findAllByItemIdIn(List.of(10L))).thenReturn(List.of(comment(now)));

        List<ItemWithBookingsDto> result = itemService.getAllByOwner(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLastBooking().getId()).isEqualTo(100L);
        assertThat(result.get(0).getNextBooking()).isNull();
        assertThat(result.get(0).getComments()).hasSize(1);
    }

    @Test
    void getAllByOwnerReturnsEmptyListWhenNoItems() {
        when(itemRepository.findAllByOwnerIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(bookingRepository.findAllByItemIdInAndStatus(List.of(), BookingStatus.APPROVED))
                .thenReturn(List.of());
        when(commentRepository.findAllByItemIdIn(List.of())).thenReturn(List.of());

        assertThat(itemService.getAllByOwner(1L)).isEmpty();
    }

    @Test
    void searchReturnsMatchingItems() {
        when(itemRepository.search("дрель")).thenReturn(List.of(item));

        assertThat(itemService.search("дрель")).hasSize(1);
    }

    @Test
    void searchReturnsEmptyListForBlankText() {
        assertThat(itemService.search("   ")).isEmpty();
        assertThat(itemService.search(null)).isEmpty();
        verify(itemRepository, never()).search(any());
    }

    @Test
    void addCommentSavesComment() {
        LocalDateTime now = LocalDateTime.now();

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.existsByItemIdAndBookerIdAndStatusAndEndBefore(
                eq(10L), eq(2L), eq(BookingStatus.APPROVED), any(LocalDateTime.class))).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenReturn(comment(now));

        CommentDto result = itemService.addComment(2L, 10L, new CommentCreateDto("Отличная дрель"));

        assertThat(result.getText()).isEqualTo("Отличная дрель");
        assertThat(result.getAuthorName()).isEqualTo("Арендатор");
    }

    @Test
    void addCommentThrowsWhenTextIsBlank() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(ValidationException.class,
                () -> itemService.addComment(2L, 10L, new CommentCreateDto("  ")));
    }

    @Test
    void addCommentThrowsWhenTextIsNull() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(ValidationException.class,
                () -> itemService.addComment(2L, 10L, new CommentCreateDto(null)));
    }

    @Test
    void addCommentThrowsWithoutFinishedBooking() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.existsByItemIdAndBookerIdAndStatusAndEndBefore(
                eq(10L), eq(2L), eq(BookingStatus.APPROVED), any(LocalDateTime.class))).thenReturn(false);

        assertThrows(ValidationException.class,
                () -> itemService.addComment(2L, 10L, new CommentCreateDto("Отличная дрель")));
    }

    private Booking booking(long id, LocalDateTime start, LocalDateTime end) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setStart(start);
        booking.setEnd(end);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.APPROVED);
        return booking;
    }

    private Comment comment(LocalDateTime created) {
        Comment comment = new Comment();
        comment.setId(1000L);
        comment.setText("Отличная дрель");
        comment.setItem(item);
        comment.setAuthor(booker);
        comment.setCreated(created);
        return comment;
    }
}

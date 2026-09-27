package ru.practicum.shareit.item.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.dto.ItemWithCommentsDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemServiceIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private ItemRequestService itemRequestService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private long ownerId;
    private long bookerId;

    @BeforeEach
    void setUp() {
        ownerId = userService.create(new UserDto(null, "Владелец", "owner-int@example.com")).getId();
        bookerId = userService.create(new UserDto(null, "Арендатор", "booker-int@example.com")).getId();
    }

    @Test
    void createPersistsItem() {
        ItemDto created = itemService.create(ownerId,
                new ItemDto(null, "Дрель", "Аккумуляторная дрель", true, null));

        assertThat(created.getId()).isNotNull();
        assertThat(itemRepository.findById(created.getId())).isPresent();
    }

    @Test
    void createLinksItemToStoredRequest() {
        ItemRequestDto request = itemRequestService.create(bookerId, new ItemRequestCreateDto("Нужна дрель"));

        ItemDto created = itemService.create(ownerId,
                new ItemDto(null, "Дрель", "Аккумуляторная дрель", true, request.getId()));

        assertThat(created.getRequestId()).isEqualTo(request.getId());
        assertThat(itemRepository.findAllByRequestId(request.getId())).hasSize(1);
    }

    @Test
    void updateChangesStoredItem() {
        ItemDto created = itemService.create(ownerId,
                new ItemDto(null, "Дрель", "Аккумуляторная дрель", true, null));

        itemService.update(ownerId, created.getId(), new ItemDto(null, "Перфоратор", null, false, null));

        Item stored = itemRepository.findById(created.getId()).orElseThrow();
        assertThat(stored.getName()).isEqualTo("Перфоратор");
        assertThat(stored.getDescription()).isEqualTo("Аккумуляторная дрель");
        assertThat(stored.getAvailable()).isFalse();
    }

    @Test
    void getAllByOwnerReturnsItemsWithBookingsAndComments() {
        ItemDto first = itemService.create(ownerId,
                new ItemDto(null, "Дрель", "Аккумуляторная дрель", true, null));
        itemService.create(ownerId, new ItemDto(null, "Молоток", "Тяжёлый молоток", true, null));

        LocalDateTime now = LocalDateTime.now();
        saveBooking(first.getId(), now.minusDays(3), now.minusDays(2));
        saveBooking(first.getId(), now.plusDays(1), now.plusDays(2));
        itemService.addComment(bookerId, first.getId(), new CommentCreateDto("Отличная дрель"));

        List<ItemWithBookingsDto> result = itemService.getAllByOwner(ownerId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Дрель");
        assertThat(result.get(0).getLastBooking()).isNotNull();
        assertThat(result.get(0).getNextBooking()).isNotNull();
        assertThat(result.get(0).getComments()).hasSize(1);
        assertThat(result.get(1).getName()).isEqualTo("Молоток");
        assertThat(result.get(1).getLastBooking()).isNull();
        assertThat(result.get(1).getComments()).isEmpty();
    }

    @Test
    void getByIdReturnsCommentsForAnyUser() {
        ItemDto created = itemService.create(ownerId,
                new ItemDto(null, "Дрель", "Аккумуляторная дрель", true, null));
        LocalDateTime now = LocalDateTime.now();
        saveBooking(created.getId(), now.minusDays(3), now.minusDays(2));
        itemService.addComment(bookerId, created.getId(), new CommentCreateDto("Отличная дрель"));

        ItemWithCommentsDto forOwner = itemService.getById(ownerId, created.getId());
        ItemWithCommentsDto forBooker = itemService.getById(bookerId, created.getId());

        assertThat(forOwner.getLastBooking()).isNotNull();
        assertThat(forBooker.getLastBooking()).isNull();
        assertThat(forBooker.getComments()).hasSize(1);
    }

    @Test
    void searchFindsAvailableItemsByText() {
        itemService.create(ownerId, new ItemDto(null, "Дрель", "Аккумуляторная дрель", true, null));
        itemService.create(ownerId, new ItemDto(null, "Молоток", "Тяжёлый молоток", false, null));

        assertThat(itemService.search("дрель")).hasSize(1);
        assertThat(itemService.search("молоток")).isEmpty();
    }

    @Test
    void addCommentPersistsCommentAfterFinishedBooking() {
        ItemDto created = itemService.create(ownerId,
                new ItemDto(null, "Дрель", "Аккумуляторная дрель", true, null));
        LocalDateTime now = LocalDateTime.now();
        saveBooking(created.getId(), now.minusDays(3), now.minusDays(2));

        CommentDto comment = itemService.addComment(bookerId, created.getId(),
                new CommentCreateDto("Отличная дрель"));

        assertThat(comment.getId()).isNotNull();
        assertThat(comment.getAuthorName()).isEqualTo("Арендатор");
    }

    @Test
    void addCommentRejectedWithoutFinishedBooking() {
        ItemDto created = itemService.create(ownerId,
                new ItemDto(null, "Дрель", "Аккумуляторная дрель", true, null));

        assertThrows(ValidationException.class, () -> itemService.addComment(bookerId, created.getId(),
                new CommentCreateDto("Отличная дрель")));
    }

    private void saveBooking(long itemId, LocalDateTime start, LocalDateTime end) {
        Item item = itemRepository.findById(itemId).orElseThrow();
        User booker = userRepository.findById(bookerId).orElseThrow();

        Booking booking = new Booking();
        booking.setStart(start);
        booking.setEnd(end);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(booking);
    }
}

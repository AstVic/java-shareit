package ru.practicum.shareit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.item.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    private User owner;
    private User booker;
    private Item item;
    private ItemRequest request;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new User(null, "Владелец", "owner@example.com"));
        booker = userRepository.save(new User(null, "Арендатор", "booker@example.com"));

        request = new ItemRequest();
        request.setDescription("Нужна дрель");
        request.setRequester(booker);
        request.setCreated(LocalDateTime.now().minusDays(1));
        request = itemRequestRepository.save(request);

        item = new Item();
        item.setName("Дрель");
        item.setDescription("Аккумуляторная дрель");
        item.setAvailable(true);
        item.setOwner(owner);
        item.setRequest(request);
        item = itemRepository.save(item);
    }

    @Test
    void existsByEmailFindsSavedUser() {
        assertThat(userRepository.existsByEmail("owner@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void findAllByOwnerIdReturnsOwnerItems() {
        assertThat(itemRepository.findAllByOwnerIdOrderByIdAsc(owner.getId())).hasSize(1);
        assertThat(itemRepository.findAllByOwnerIdOrderByIdAsc(booker.getId())).isEmpty();
    }

    @Test
    void searchIgnoresCaseAndMatchesNameOrDescription() {
        assertThat(itemRepository.search("ДРЕЛЬ")).hasSize(1);
        assertThat(itemRepository.search("аккумулятор")).hasSize(1);
        assertThat(itemRepository.search("молоток")).isEmpty();
    }

    @Test
    void searchSkipsUnavailableItems() {
        item.setAvailable(false);
        itemRepository.save(item);

        assertThat(itemRepository.search("дрель")).isEmpty();
    }

    @Test
    void findAllByRequestIdReturnsAnswers() {
        assertThat(itemRepository.findAllByRequestId(request.getId())).hasSize(1);
        assertThat(itemRepository.findAllByRequestIdIn(List.of(request.getId()))).hasSize(1);
        assertThat(itemRepository.findAllByRequestIdIn(List.of())).isEmpty();
    }

    @Test
    void findAllByRequesterIdSortsFromNewestToOldest() {
        ItemRequest newer = new ItemRequest();
        newer.setDescription("Нужен молоток");
        newer.setRequester(booker);
        newer.setCreated(LocalDateTime.now());
        itemRequestRepository.save(newer);

        List<ItemRequest> own = itemRequestRepository.findAllByRequesterIdOrderByCreatedDesc(booker.getId());

        assertThat(own).hasSize(2);
        assertThat(own.get(0).getDescription()).isEqualTo("Нужен молоток");
        assertThat(itemRequestRepository.findAllByRequesterIdNotOrderByCreatedDesc(owner.getId())).hasSize(2);
        assertThat(itemRequestRepository.findAllByRequesterIdNotOrderByCreatedDesc(booker.getId())).isEmpty();
    }

    @Test
    void bookingQueriesFilterByStateAndOwner() {
        LocalDateTime now = LocalDateTime.now();
        bookingRepository.save(booking(now.minusDays(3), now.minusDays(2), BookingStatus.APPROVED));
        bookingRepository.save(booking(now.plusDays(1), now.plusDays(2), BookingStatus.WAITING));
        bookingRepository.save(booking(now.minusHours(1), now.plusHours(1), BookingStatus.APPROVED));

        assertThat(bookingRepository.findAllByBookerIdOrderByStartDesc(booker.getId())).hasSize(3);
        assertThat(bookingRepository.findAllByBookerIdAndEndBeforeOrderByStartDesc(booker.getId(), now)).hasSize(1);
        assertThat(bookingRepository.findAllByBookerIdAndStartAfterOrderByStartDesc(booker.getId(), now)).hasSize(1);
        assertThat(bookingRepository.findAllByBookerIdAndStartBeforeAndEndAfterOrderByStartDesc(
                booker.getId(), now, now)).hasSize(1);
        assertThat(bookingRepository.findAllByBookerIdAndStatusOrderByStartDesc(
                booker.getId(), BookingStatus.WAITING)).hasSize(1);

        assertThat(bookingRepository.findAllByItemOwnerIdOrderByStartDesc(owner.getId())).hasSize(3);
        assertThat(bookingRepository.findAllByItemOwnerIdAndEndBeforeOrderByStartDesc(owner.getId(), now)).hasSize(1);
        assertThat(bookingRepository.findAllByItemOwnerIdAndStartAfterOrderByStartDesc(owner.getId(), now)).hasSize(1);
        assertThat(bookingRepository.findAllByItemOwnerIdAndStartBeforeAndEndAfterOrderByStartDesc(
                owner.getId(), now, now)).hasSize(1);
        assertThat(bookingRepository.findAllByItemOwnerIdAndStatusOrderByStartDesc(
                owner.getId(), BookingStatus.APPROVED)).hasSize(2);

        assertThat(bookingRepository.findAllByItemIdInAndStatus(
                List.of(item.getId()), BookingStatus.APPROVED)).hasSize(2);
        assertThat(bookingRepository.existsByItemIdAndBookerIdAndStatusAndEndBefore(
                item.getId(), booker.getId(), BookingStatus.APPROVED, now)).isTrue();
        assertThat(bookingRepository.existsByItemIdAndBookerIdAndStatusAndEndBefore(
                item.getId(), owner.getId(), BookingStatus.APPROVED, now)).isFalse();
    }

    @Test
    void commentQueriesFindCommentsByItem() {
        Comment comment = new Comment();
        comment.setText("Отличная дрель");
        comment.setItem(item);
        comment.setAuthor(booker);
        comment.setCreated(LocalDateTime.now());
        commentRepository.save(comment);

        assertThat(commentRepository.findAllByItemId(item.getId())).hasSize(1);
        assertThat(commentRepository.findAllByItemIdIn(List.of(item.getId()))).hasSize(1);
        assertThat(commentRepository.findAllByItemIdIn(List.of())).isEmpty();
    }

    private Booking booking(LocalDateTime start, LocalDateTime end, BookingStatus status) {
        Booking booking = new Booking();
        booking.setStart(start);
        booking.setEnd(end);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(status);
        return booking;
    }
}

package ru.practicum.shareit.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User owner;
    private User booker;
    private Item item;
    private Booking booking;
    private LocalDateTime start;
    private LocalDateTime end;

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

        start = LocalDateTime.now().plusDays(1);
        end = LocalDateTime.now().plusDays(2);

        booking = new Booking();
        booking.setId(100L);
        booking.setStart(start);
        booking.setEnd(end);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);
    }

    @Test
    void createSavesBookingWithWaitingStatus() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingDto result = bookingService.create(2L, new BookingCreateDto(10L, start, end));

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(result.getItem().getName()).isEqualTo("Дрель");
        assertThat(result.getBooker().getId()).isEqualTo(2L);
    }

    @Test
    void createThrowsWhenBookerIsMissing() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> bookingService.create(42L, new BookingCreateDto(10L, start, end)));
    }

    @Test
    void createThrowsWhenDatesAreNull() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));

        assertThrows(ValidationException.class,
                () -> bookingService.create(2L, new BookingCreateDto(10L, null, end)));
        assertThrows(ValidationException.class,
                () -> bookingService.create(2L, new BookingCreateDto(10L, start, null)));
    }

    @Test
    void createThrowsWhenEndIsBeforeStart() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));

        assertThrows(ValidationException.class,
                () -> bookingService.create(2L, new BookingCreateDto(10L, end, start)));
    }

    @Test
    void createThrowsWhenEndEqualsStart() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));

        assertThrows(ValidationException.class,
                () -> bookingService.create(2L, new BookingCreateDto(10L, start, start)));
    }

    @Test
    void createThrowsWhenStartIsInThePast() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));

        assertThrows(ValidationException.class, () -> bookingService.create(2L,
                new BookingCreateDto(10L, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1))));
    }

    @Test
    void createThrowsWhenItemIdIsNull() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));

        assertThrows(ValidationException.class,
                () -> bookingService.create(2L, new BookingCreateDto(null, start, end)));
    }

    @Test
    void createThrowsWhenItemIsMissing() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> bookingService.create(2L, new BookingCreateDto(99L, start, end)));
    }

    @Test
    void createThrowsWhenItemIsNotAvailable() {
        item.setAvailable(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(ValidationException.class,
                () -> bookingService.create(2L, new BookingCreateDto(10L, start, end)));
    }

    @Test
    void createThrowsWhenOwnerBooksOwnItem() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(NotFoundException.class,
                () -> bookingService.create(1L, new BookingCreateDto(10L, start, end)));
    }

    @Test
    void approveSetsApprovedStatus() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThat(bookingService.approve(1L, 100L, true).getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void approveSetsRejectedStatus() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThat(bookingService.approve(1L, 100L, false).getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void approveThrowsWhenUserIsNotOwner() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThrows(ForbiddenException.class, () -> bookingService.approve(2L, 100L, true));
    }

    @Test
    void approveThrowsWhenBookingIsAlreadyReviewed() {
        booking.setStatus(BookingStatus.APPROVED);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThrows(ValidationException.class, () -> bookingService.approve(1L, 100L, true));
    }

    @Test
    void approveThrowsWhenBookingIsMissing() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.approve(1L, 99L, true));
    }

    @Test
    void getByIdReturnsBookingForBooker() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThat(bookingService.getById(2L, 100L).getId()).isEqualTo(100L);
    }

    @Test
    void getByIdReturnsBookingForOwner() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThat(bookingService.getById(1L, 100L).getId()).isEqualTo(100L);
    }

    @Test
    void getByIdThrowsForStranger() {
        User stranger = new User(3L, "Посторонний", "stranger@example.com");
        when(userRepository.findById(3L)).thenReturn(Optional.of(stranger));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThrows(NotFoundException.class, () -> bookingService.getById(3L, 100L));
    }

    @Test
    void getAllByBookerReturnsAll() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdOrderByStartDesc(2L)).thenReturn(List.of(booking));

        assertThat(bookingService.getAllByBooker(2L, "ALL")).hasSize(1);
    }

    @Test
    void getAllByBookerReturnsCurrent() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndStartBeforeAndEndAfterOrderByStartDesc(
                eq(2L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(booking));

        assertThat(bookingService.getAllByBooker(2L, "CURRENT")).hasSize(1);
    }

    @Test
    void getAllByBookerReturnsPast() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndEndBeforeOrderByStartDesc(eq(2L), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));

        assertThat(bookingService.getAllByBooker(2L, "PAST")).hasSize(1);
    }

    @Test
    void getAllByBookerReturnsFuture() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndStartAfterOrderByStartDesc(eq(2L), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));

        assertThat(bookingService.getAllByBooker(2L, "FUTURE")).hasSize(1);
    }

    @Test
    void getAllByBookerReturnsWaiting() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndStatusOrderByStartDesc(2L, BookingStatus.WAITING))
                .thenReturn(List.of(booking));

        assertThat(bookingService.getAllByBooker(2L, "waiting")).hasSize(1);
    }

    @Test
    void getAllByBookerReturnsRejected() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndStatusOrderByStartDesc(2L, BookingStatus.REJECTED))
                .thenReturn(List.of(booking));

        assertThat(bookingService.getAllByBooker(2L, "REJECTED")).hasSize(1);
    }

    @Test
    void getAllByBookerThrowsOnUnknownState() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));

        assertThrows(ValidationException.class, () -> bookingService.getAllByBooker(2L, "UNSUPPORTED"));
    }

    @Test
    void getAllByOwnerReturnsAll() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findAllByItemOwnerIdOrderByStartDesc(1L)).thenReturn(List.of(booking));

        assertThat(bookingService.getAllByOwner(1L, "ALL")).hasSize(1);
    }

    @Test
    void getAllByOwnerReturnsCurrent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findAllByItemOwnerIdAndStartBeforeAndEndAfterOrderByStartDesc(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(booking));

        assertThat(bookingService.getAllByOwner(1L, "CURRENT")).hasSize(1);
    }

    @Test
    void getAllByOwnerReturnsPast() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findAllByItemOwnerIdAndEndBeforeOrderByStartDesc(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));

        assertThat(bookingService.getAllByOwner(1L, "PAST")).hasSize(1);
    }

    @Test
    void getAllByOwnerReturnsFuture() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findAllByItemOwnerIdAndStartAfterOrderByStartDesc(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));

        assertThat(bookingService.getAllByOwner(1L, "FUTURE")).hasSize(1);
    }

    @Test
    void getAllByOwnerReturnsWaiting() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findAllByItemOwnerIdAndStatusOrderByStartDesc(1L, BookingStatus.WAITING))
                .thenReturn(List.of(booking));

        assertThat(bookingService.getAllByOwner(1L, "WAITING")).hasSize(1);
    }

    @Test
    void getAllByOwnerReturnsRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findAllByItemOwnerIdAndStatusOrderByStartDesc(1L, BookingStatus.REJECTED))
                .thenReturn(List.of(booking));

        assertThat(bookingService.getAllByOwner(1L, "REJECTED")).hasSize(1);
    }

    @Test
    void getAllByOwnerThrowsOnUnknownState() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThrows(ValidationException.class, () -> bookingService.getAllByOwner(1L, "UNSUPPORTED"));
    }
}

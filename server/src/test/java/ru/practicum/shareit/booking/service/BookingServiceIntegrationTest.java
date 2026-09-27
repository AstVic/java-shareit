package ru.practicum.shareit.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingServiceIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    private long ownerId;
    private long bookerId;
    private long itemId;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        ownerId = userService.create(new UserDto(null, "Владелец", "owner-book@example.com")).getId();
        bookerId = userService.create(new UserDto(null, "Арендатор", "booker-book@example.com")).getId();
        itemId = itemService.create(ownerId,
                new ItemDto(null, "Дрель", "Аккумуляторная дрель", true, null)).getId();

        start = LocalDateTime.now().plusDays(1);
        end = LocalDateTime.now().plusDays(2);
    }

    @Test
    void createPersistsBookingInWaitingStatus() {
        BookingDto created = bookingService.create(bookerId, new BookingCreateDto(itemId, start, end));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(bookingService.getById(bookerId, created.getId()).getItem().getId()).isEqualTo(itemId);
    }

    @Test
    void approveChangesStoredStatus() {
        BookingDto created = bookingService.create(bookerId, new BookingCreateDto(itemId, start, end));

        bookingService.approve(ownerId, created.getId(), true);

        assertThat(bookingService.getById(ownerId, created.getId()).getStatus())
                .isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void approveRejectedByNonOwner() {
        BookingDto created = bookingService.create(bookerId, new BookingCreateDto(itemId, start, end));

        assertThrows(ForbiddenException.class, () -> bookingService.approve(bookerId, created.getId(), true));
    }

    @Test
    void getByIdHiddenFromStranger() {
        long strangerId = userService.create(new UserDto(null, "Посторонний", "stranger@example.com")).getId();
        BookingDto created = bookingService.create(bookerId, new BookingCreateDto(itemId, start, end));

        assertThrows(NotFoundException.class, () -> bookingService.getById(strangerId, created.getId()));
    }

    @Test
    void getAllByBookerReturnsBookingsByState() {
        bookingService.create(bookerId, new BookingCreateDto(itemId, start, end));

        assertThat(bookingService.getAllByBooker(bookerId, "ALL")).hasSize(1);
        assertThat(bookingService.getAllByBooker(bookerId, "FUTURE")).hasSize(1);
        assertThat(bookingService.getAllByBooker(bookerId, "WAITING")).hasSize(1);
        assertThat(bookingService.getAllByBooker(bookerId, "PAST")).isEmpty();
        assertThat(bookingService.getAllByBooker(bookerId, "CURRENT")).isEmpty();
        assertThat(bookingService.getAllByBooker(bookerId, "REJECTED")).isEmpty();
    }

    @Test
    void getAllByOwnerReturnsBookingsByState() {
        BookingDto created = bookingService.create(bookerId, new BookingCreateDto(itemId, start, end));
        bookingService.approve(ownerId, created.getId(), false);

        assertThat(bookingService.getAllByOwner(ownerId, "ALL")).hasSize(1);
        assertThat(bookingService.getAllByOwner(ownerId, "REJECTED")).hasSize(1);
        assertThat(bookingService.getAllByOwner(ownerId, "WAITING")).isEmpty();
    }
}

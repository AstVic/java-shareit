package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.service.BookingService;

import java.util.List;

/**
 * TODO Sprint add-bookings.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/bookings")
public class BookingController {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    private final BookingService bookingService;

    @PostMapping
    public BookingDto create(@RequestHeader(USER_HEADER) long userId,
                             @RequestBody BookingCreateDto bookingCreateDto) {
        return bookingService.create(userId, bookingCreateDto);
    }

    @PatchMapping("/{bookingId}")
    public BookingDto approve(@RequestHeader(USER_HEADER) long userId,
                              @PathVariable long bookingId,
                              @RequestParam boolean approved) {
        return bookingService.approve(userId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public BookingDto getById(@RequestHeader(USER_HEADER) long userId,
                              @PathVariable long bookingId) {
        return bookingService.getById(userId, bookingId);
    }

    @GetMapping
    public List<BookingDto> getAllByBooker(@RequestHeader(USER_HEADER) long userId,
                                           @RequestParam(defaultValue = "ALL") String state) {
        return bookingService.getAllByBooker(userId, state);
    }

    @GetMapping("/owner")
    public List<BookingDto> getAllByOwner(@RequestHeader(USER_HEADER) long userId,
                                          @RequestParam(defaultValue = "ALL") String state) {
        return bookingService.getAllByOwner(userId, state);
    }
}

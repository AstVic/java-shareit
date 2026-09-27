package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;

import java.util.List;

public interface BookingService {
    BookingDto create(long userId, BookingCreateDto bookingCreateDto);

    BookingDto approve(long userId, long bookingId, boolean approved);

    BookingDto getById(long userId, long bookingId);

    List<BookingDto> getAllByBooker(long userId, String state);

    List<BookingDto> getAllByOwner(long userId, String state);
}

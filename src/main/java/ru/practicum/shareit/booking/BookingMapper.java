package ru.practicum.shareit.booking;

import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.item.dto.ItemShortDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.dto.UserShortDto;

public class BookingMapper {
    public static BookingDto toBookingDto(Booking booking) {
        Item item = booking.getItem();
        User booker = booking.getBooker();
        return new BookingDto(
                booking.getId(),
                booking.getStart(),
                booking.getEnd(),
                new ItemShortDto(item.getId(), item.getName()),
                new UserShortDto(booker.getId(), booker.getName()),
                booking.getStatus()
        );
    }

    public static Booking toBooking(BookingCreateDto bookingCreateDto) {
        Booking booking = new Booking();
        booking.setStart(bookingCreateDto.getStart());
        booking.setEnd(bookingCreateDto.getEnd());
        return booking;
    }
}

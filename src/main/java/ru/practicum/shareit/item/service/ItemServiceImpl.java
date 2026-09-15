package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.Comment;
import ru.practicum.shareit.item.CommentMapper;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithCommentsDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public ItemDto create(long userId, ItemDto itemDto) {
        User owner = getUser(userId);
        checkItem(itemDto);

        Item item = ItemMapper.toItem(itemDto);
        item.setOwner(owner);
        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    @Transactional
    public ItemDto update(long userId, long itemId, ItemDto itemDto) {
        getUser(userId);
        Item item = getItem(itemId);
        if (!item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Редактировать вещь может только её владелец");
        }

        if (itemDto.getName() != null) {
            item.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null) {
            item.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }
        return ItemMapper.toItemDto(item);
    }

    @Override
    public ItemWithCommentsDto getById(Long userId, long itemId) {
        Item item = getItem(itemId);
        List<CommentDto> comments = commentRepository.findAllByItemId(itemId).stream()
                .map(CommentMapper::toCommentDto)
                .toList();

        BookingShortDto lastBooking = null;
        BookingShortDto nextBooking = null;
        if (userId != null && item.getOwner().getId().equals(userId)) {
            List<Booking> bookings = bookingRepository
                    .findAllByItemIdInAndStatus(List.of(itemId), BookingStatus.APPROVED);
            LocalDateTime now = LocalDateTime.now();
            lastBooking = toBookingShortDto(findLastBooking(bookings, now));
            nextBooking = toBookingShortDto(findNextBooking(bookings, now));
        }
        return ItemMapper.toItemWithCommentsDto(item, lastBooking, nextBooking, comments);
    }

    @Override
    public List<ItemWithBookingsDto> getAllByOwner(long userId) {
        List<Item> items = itemRepository.findAllByOwnerIdOrderByIdAsc(userId);
        List<Long> itemIds = items.stream()
                .map(Item::getId)
                .toList();

        Map<Long, List<Booking>> bookingsByItem = bookingRepository
                .findAllByItemIdInAndStatus(itemIds, BookingStatus.APPROVED).stream()
                .collect(Collectors.groupingBy(booking -> booking.getItem().getId()));

        Map<Long, List<CommentDto>> commentsByItem = commentRepository.findAllByItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(comment -> comment.getItem().getId(),
                        Collectors.mapping(CommentMapper::toCommentDto, Collectors.toList())));

        LocalDateTime now = LocalDateTime.now();
        return items.stream()
                .map(item -> {
                    List<Booking> bookings = bookingsByItem.getOrDefault(item.getId(), List.of());
                    return ItemMapper.toItemWithBookingsDto(
                            item,
                            toBookingShortDto(findLastBooking(bookings, now)),
                            toBookingShortDto(findNextBooking(bookings, now)),
                            commentsByItem.getOrDefault(item.getId(), List.of()));
                })
                .toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Override
    @Transactional
    public CommentDto addComment(long userId, long itemId, CommentCreateDto commentCreateDto) {
        User author = getUser(userId);
        Item item = getItem(itemId);

        if (commentCreateDto.getText() == null || commentCreateDto.getText().isBlank()) {
            throw new ValidationException("Текст комментария обязателен");
        }
        boolean hasFinishedBooking = bookingRepository.existsByItemIdAndBookerIdAndStatusAndEndBefore(
                itemId, userId, BookingStatus.APPROVED, LocalDateTime.now());
        if (!hasFinishedBooking) {
            throw new ValidationException("Оставить отзыв может только пользователь с завершённой арендой");
        }

        Comment comment = CommentMapper.toComment(commentCreateDto);
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now());
        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }

    private Booking findLastBooking(List<Booking> bookings, LocalDateTime now) {
        return bookings.stream()
                .filter(booking -> !booking.getStart().isAfter(now))
                .max(Comparator.comparing(Booking::getStart))
                .orElse(null);
    }

    private Booking findNextBooking(List<Booking> bookings, LocalDateTime now) {
        return bookings.stream()
                .filter(booking -> booking.getStart().isAfter(now))
                .min(Comparator.comparing(Booking::getStart))
                .orElse(null);
    }

    private BookingShortDto toBookingShortDto(Booking booking) {
        if (booking == null) {
            return null;
        }
        return new BookingShortDto(booking.getId(), booking.getBooker().getId());
    }

    private User getUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователя с таким id нет"));
    }

    private Item getItem(long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещи с таким id нет"));
    }

    private void checkItem(ItemDto itemDto) {
        if (itemDto.getName() == null || itemDto.getName().isBlank()) {
            throw new ValidationException("Название вещи обязательно");
        }
        if (itemDto.getDescription() == null || itemDto.getDescription().isBlank()) {
            throw new ValidationException("Описание вещи обязательно");
        }
        if (itemDto.getAvailable() == null) {
            throw new ValidationException("Статус доступности обязателен");
        }
    }
}

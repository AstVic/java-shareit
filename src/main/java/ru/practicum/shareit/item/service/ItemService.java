package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithCommentsDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;

import java.util.List;

public interface ItemService {
    ItemDto create(long userId, ItemDto itemDto);

    ItemDto update(long userId, long itemId, ItemDto itemDto);

    ItemWithCommentsDto getById(Long userId, long itemId);

    List<ItemWithBookingsDto> getAllByOwner(long userId);

    List<ItemDto> search(String text);

    CommentDto addComment(long userId, long itemId, CommentCreateDto commentCreateDto);
}

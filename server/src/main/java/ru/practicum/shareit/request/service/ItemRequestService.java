package ru.practicum.shareit.request.service;

import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.util.List;

public interface ItemRequestService {
    ItemRequestDto create(long userId, ItemRequestCreateDto itemRequestCreateDto);

    List<ItemRequestDto> getOwn(long userId);

    List<ItemRequestDto> getAll(long userId);

    ItemRequestDto getById(long userId, long requestId);
}

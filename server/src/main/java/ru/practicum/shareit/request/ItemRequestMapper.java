package ru.practicum.shareit.request;

import ru.practicum.shareit.item.dto.ItemForRequestDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.util.List;

public class ItemRequestMapper {

    public static ItemRequestDto toItemRequestDto(ItemRequest itemRequest, List<Item> items) {
        return new ItemRequestDto(
                itemRequest.getId(),
                itemRequest.getDescription(),
                itemRequest.getCreated(),
                items.stream()
                        .map(ItemRequestMapper::toItemForRequestDto)
                        .toList()
        );
    }

    public static ItemForRequestDto toItemForRequestDto(Item item) {
        return new ItemForRequestDto(
                item.getId(),
                item.getName(),
                item.getOwner().getId()
        );
    }

    public static ItemRequest toItemRequest(ItemRequestCreateDto itemRequestCreateDto) {
        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setDescription(itemRequestCreateDto.getDescription());
        return itemRequest;
    }
}

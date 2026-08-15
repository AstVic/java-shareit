package ru.practicum.shareit.item.repository;

import ru.practicum.shareit.item.model.Item;

import java.util.List;

public interface ItemRepository {
    Item save(Item item);
    Item findById(long itemId);
    Item update(Item item);
    List<Item> findAllByOwner(long userId);
    List<Item> search(String text);
}

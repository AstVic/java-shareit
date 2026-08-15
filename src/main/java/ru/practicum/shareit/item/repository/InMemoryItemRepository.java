package ru.practicum.shareit.item.repository;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class InMemoryItemRepository implements ItemRepository {

    private final Map<Long, Item> items = new HashMap<>();
    private long counter = 0;

    @Override
    public Item save(Item item) {
        long id = getNextId();
        item.setId(id);
        items.put(id, item);
        return item;
    }

    @Override
    public Item findById(long itemId) {
        return items.getOrDefault(itemId, null);
    }

    @Override
    public Item update(Item item) {
        items.put(item.getId(), item);
        return item;
    }

    @Override
    public List<Item> findAllByOwner(long userId) {
        return items.values().stream()
                .filter(item -> item.getOwner().getId() == userId)
                .toList();
    }

    @Override
    public List<Item> search(String text) {
        String lower = text.toLowerCase();
        return items.values().stream()
                .filter(item -> Boolean.TRUE.equals(item.getAvailable()))
                .filter(item -> item.getName().toLowerCase().contains(lower)
                        || item.getDescription().toLowerCase().contains(lower))
                .toList();
    }

    private long getNextId() {
        return ++counter;
    }
}

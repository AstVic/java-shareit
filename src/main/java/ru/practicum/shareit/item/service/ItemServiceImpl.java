package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    public ItemDto create(long userId, ItemDto itemDto) {
        User owner = getUser(userId);
        checkItem(itemDto);

        Item item = ItemMapper.toItem(itemDto);
        item.setOwner(owner);
        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    public ItemDto update(long userId, long itemId, ItemDto itemDto) {
        getUser(userId);
        Item item = getItem(itemId);
        if (item.getOwner().getId() != userId) {
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
        return ItemMapper.toItemDto(itemRepository.update(item));
    }

    @Override
    public ItemDto getById(long itemId) {
        return ItemMapper.toItemDto(getItem(itemId));
    }

    @Override
    public List<ItemDto> getAllByOwner(long userId) {
        return itemRepository.findAllByOwner(userId).stream()
                .map(ItemMapper::toItemDto)
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

    private User getUser(long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new NotFoundException("Пользователя с таким id нет");
        }
        return user;
    }

    private Item getItem(long itemId) {
        Item item = itemRepository.findById(itemId);
        if (item == null) {
            throw new NotFoundException("Вещи с таким id нет");
        }
        return item;
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

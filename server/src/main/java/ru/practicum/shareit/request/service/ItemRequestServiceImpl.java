package ru.practicum.shareit.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestMapper;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {
    private final ItemRequestRepository itemRequestRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ItemRequestDto create(long userId, ItemRequestCreateDto itemRequestCreateDto) {
        User requester = getUser(userId);
        if (itemRequestCreateDto.getDescription() == null || itemRequestCreateDto.getDescription().isBlank()) {
            throw new ValidationException("Описание запроса обязательно");
        }

        ItemRequest itemRequest = ItemRequestMapper.toItemRequest(itemRequestCreateDto);
        itemRequest.setRequester(requester);
        itemRequest.setCreated(LocalDateTime.now());
        return ItemRequestMapper.toItemRequestDto(itemRequestRepository.save(itemRequest), List.of());
    }

    @Override
    public List<ItemRequestDto> getOwn(long userId) {
        getUser(userId);
        return toDtoList(itemRequestRepository.findAllByRequesterIdOrderByCreatedDesc(userId));
    }

    @Override
    public List<ItemRequestDto> getAll(long userId) {
        getUser(userId);
        return toDtoList(itemRequestRepository.findAllByRequesterIdNotOrderByCreatedDesc(userId));
    }

    @Override
    public ItemRequestDto getById(long userId, long requestId) {
        getUser(userId);
        ItemRequest itemRequest = itemRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запроса с таким id нет"));

        return ItemRequestMapper.toItemRequestDto(itemRequest, itemRepository.findAllByRequestId(requestId));
    }

    private List<ItemRequestDto> toDtoList(List<ItemRequest> requests) {
        List<Long> requestIds = requests.stream()
                .map(ItemRequest::getId)
                .toList();

        Map<Long, List<Item>> itemsByRequest = itemRepository.findAllByRequestIdIn(requestIds).stream()
                .collect(Collectors.groupingBy(item -> item.getRequest().getId()));

        return requests.stream()
                .map(request -> ItemRequestMapper.toItemRequestDto(
                        request,
                        itemsByRequest.getOrDefault(request.getId(), List.of())))
                .toList();
    }

    private User getUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователя с таким id нет"));
    }
}

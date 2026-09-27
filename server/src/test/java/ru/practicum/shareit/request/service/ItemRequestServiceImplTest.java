package ru.practicum.shareit.request.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemRequestServiceImplTest {

    @Mock
    private ItemRequestRepository itemRequestRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ItemRequestServiceImpl itemRequestService;

    private User requester;
    private User owner;
    private ItemRequest request;
    private Item item;

    @BeforeEach
    void setUp() {
        requester = new User(1L, "Автор запроса", "requester@example.com");
        owner = new User(2L, "Владелец", "owner@example.com");

        request = new ItemRequest();
        request.setId(5L);
        request.setDescription("Нужна дрель");
        request.setRequester(requester);
        request.setCreated(LocalDateTime.now());

        item = new Item();
        item.setId(10L);
        item.setName("Дрель");
        item.setDescription("Аккумуляторная дрель");
        item.setAvailable(true);
        item.setOwner(owner);
        item.setRequest(request);
    }

    @Test
    void createSavesRequestWithEmptyAnswers() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(itemRequestRepository.save(any(ItemRequest.class))).thenReturn(request);

        ItemRequestDto result = itemRequestService.create(1L, new ItemRequestCreateDto("Нужна дрель"));

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getDescription()).isEqualTo("Нужна дрель");
        assertThat(result.getCreated()).isNotNull();
        assertThat(result.getItems()).isEmpty();
    }

    @Test
    void createThrowsWhenDescriptionIsBlank() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));

        assertThrows(ValidationException.class,
                () -> itemRequestService.create(1L, new ItemRequestCreateDto("   ")));
        verify(itemRequestRepository, never()).save(any(ItemRequest.class));
    }

    @Test
    void createThrowsWhenDescriptionIsNull() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));

        assertThrows(ValidationException.class,
                () -> itemRequestService.create(1L, new ItemRequestCreateDto(null)));
    }

    @Test
    void createThrowsWhenUserIsMissing() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> itemRequestService.create(42L, new ItemRequestCreateDto("Нужна дрель")));
    }

    @Test
    void getOwnReturnsRequestsWithAnswers() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(itemRequestRepository.findAllByRequesterIdOrderByCreatedDesc(1L)).thenReturn(List.of(request));
        when(itemRepository.findAllByRequestIdIn(List.of(5L))).thenReturn(List.of(item));

        List<ItemRequestDto> result = itemRequestService.getOwn(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItems()).hasSize(1);
        assertThat(result.get(0).getItems().get(0).getId()).isEqualTo(10L);
        assertThat(result.get(0).getItems().get(0).getName()).isEqualTo("Дрель");
        assertThat(result.get(0).getItems().get(0).getOwnerId()).isEqualTo(2L);
    }

    @Test
    void getOwnReturnsRequestWithoutAnswers() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(itemRequestRepository.findAllByRequesterIdOrderByCreatedDesc(1L)).thenReturn(List.of(request));
        when(itemRepository.findAllByRequestIdIn(List.of(5L))).thenReturn(List.of());

        assertThat(itemRequestService.getOwn(1L).get(0).getItems()).isEmpty();
    }

    @Test
    void getOwnThrowsWhenUserIsMissing() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemRequestService.getOwn(42L));
    }

    @Test
    void getAllReturnsRequestsOfOtherUsers() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(itemRequestRepository.findAllByRequesterIdNotOrderByCreatedDesc(2L)).thenReturn(List.of(request));
        when(itemRepository.findAllByRequestIdIn(List.of(5L))).thenReturn(List.of(item));

        List<ItemRequestDto> result = itemRequestService.getAll(2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(5L);
    }

    @Test
    void getAllThrowsWhenUserIsMissing() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemRequestService.getAll(42L));
    }

    @Test
    void getByIdReturnsRequestWithAnswers() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(itemRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(itemRepository.findAllByRequestId(5L)).thenReturn(List.of(item));

        ItemRequestDto result = itemRequestService.getById(2L, 5L);

        assertThat(result.getDescription()).isEqualTo("Нужна дрель");
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    void getByIdThrowsWhenRequestIsMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(itemRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemRequestService.getById(1L, 99L));
    }

    @Test
    void getByIdThrowsWhenUserIsMissing() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemRequestService.getById(42L, 5L));
    }
}

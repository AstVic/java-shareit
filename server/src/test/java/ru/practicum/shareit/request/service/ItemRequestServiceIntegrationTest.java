package ru.practicum.shareit.request.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemRequestServiceIntegrationTest {

    @Autowired
    private ItemRequestService itemRequestService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    private long requesterId;
    private long ownerId;

    @BeforeEach
    void setUp() {
        requesterId = userService.create(new UserDto(null, "Автор", "requester-int@example.com")).getId();
        ownerId = userService.create(new UserDto(null, "Владелец", "owner-req@example.com")).getId();
    }

    @Test
    void createPersistsRequest() {
        ItemRequestDto created = itemRequestService.create(requesterId, new ItemRequestCreateDto("Нужна дрель"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getCreated()).isNotNull();
        assertThat(created.getItems()).isEmpty();
        assertThat(itemRequestRepository.findById(created.getId())).isPresent();
    }

    @Test
    void getOwnReturnsRequestsFromNewestToOldestWithAnswers() {
        ItemRequestDto older = itemRequestService.create(requesterId, new ItemRequestCreateDto("Нужна дрель"));
        ItemRequestDto newer = itemRequestService.create(requesterId, new ItemRequestCreateDto("Нужен молоток"));
        itemService.create(ownerId, new ItemDto(null, "Дрель", "Аккумуляторная дрель", true, older.getId()));

        List<ItemRequestDto> result = itemRequestService.getOwn(requesterId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(newer.getId());
        assertThat(result.get(0).getItems()).isEmpty();
        assertThat(result.get(1).getId()).isEqualTo(older.getId());
        assertThat(result.get(1).getItems()).hasSize(1);
        assertThat(result.get(1).getItems().get(0).getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    void getAllReturnsOnlyOtherUsersRequests() {
        itemRequestService.create(requesterId, new ItemRequestCreateDto("Нужна дрель"));

        assertThat(itemRequestService.getAll(ownerId)).hasSize(1);
        assertThat(itemRequestService.getAll(requesterId)).isEmpty();
    }

    @Test
    void getByIdReturnsRequestWithAnswers() {
        ItemRequestDto created = itemRequestService.create(requesterId, new ItemRequestCreateDto("Нужна дрель"));
        itemService.create(ownerId, new ItemDto(null, "Дрель", "Аккумуляторная дрель", true, created.getId()));

        ItemRequestDto result = itemRequestService.getById(ownerId, created.getId());

        assertThat(result.getDescription()).isEqualTo("Нужна дрель");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getName()).isEqualTo("Дрель");
    }

    @Test
    void getByIdRejectsMissingRequest() {
        assertThrows(NotFoundException.class, () -> itemRequestService.getById(requesterId, 9999L));
    }
}

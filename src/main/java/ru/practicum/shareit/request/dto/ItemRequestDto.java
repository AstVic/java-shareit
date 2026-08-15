package ru.practicum.shareit.request.dto;

import lombok.Data;
import ru.practicum.shareit.user.User;

import java.time.LocalDateTime;

/**
 * TODO Sprint add-item-requests.
 */

@Data
public class ItemRequestDto {
    private Long id;
    private String description;
    private User requester;
    private LocalDateTime created;
}

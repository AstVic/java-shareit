package ru.practicum.shareit.user.repository;

import ru.practicum.shareit.user.User;

import java.util.Collection;

public interface UserRepository {
    User save(User user);
    boolean existsByEmail(String email);
    User findById(long userId);
    Collection<User> findAll();
    User update(User user);
    void delete(long userId);
}
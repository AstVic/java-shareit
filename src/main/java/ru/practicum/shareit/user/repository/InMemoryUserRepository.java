package ru.practicum.shareit.user.repository;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.user.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> users = new HashMap<>();
    private long counter = 0;

    @Override
    public User save(User user) {
        Long id = getNextId();
        users.put(id, user);
        user.setId(id);
        return user;
    }

    @Override
    public boolean existsByEmail(String email) {
        return users.values().stream()
                .anyMatch(user -> user.getEmail().equals(email));
    }

    @Override
    public User findById(long userId) {
        return users.getOrDefault(userId, null);
    }

    @Override
    public Collection<User> findAll() {
        return users.values();
    }

    @Override
    public User update(User user) {
        if (user != null) {
            Long id = user.getId();
            users.put(id, user);
        }
        return user;
    }

    @Override
    public void delete(long userId) {
        users.remove(userId);
    }

    private long getNextId() {
        return ++counter;
    }
}

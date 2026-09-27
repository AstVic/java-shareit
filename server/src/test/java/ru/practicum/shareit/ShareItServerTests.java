package ru.practicum.shareit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ShareItServerTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoadsWithAllServices() {
        assertThat(applicationContext.getBean(UserService.class)).isNotNull();
        assertThat(applicationContext.getBean(ItemService.class)).isNotNull();
        assertThat(applicationContext.getBean(BookingService.class)).isNotNull();
        assertThat(applicationContext.getBean(ItemRequestService.class)).isNotNull();
    }
}

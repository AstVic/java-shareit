package ru.practicum.shareit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import ru.practicum.shareit.booking.BookingClient;
import ru.practicum.shareit.item.ItemClient;
import ru.practicum.shareit.request.ItemRequestClient;
import ru.practicum.shareit.user.UserClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ShareItGatewayTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoadsWithAllClients() {
        assertThat(applicationContext.getBean(UserClient.class)).isNotNull();
        assertThat(applicationContext.getBean(ItemClient.class)).isNotNull();
        assertThat(applicationContext.getBean(BookingClient.class)).isNotNull();
        assertThat(applicationContext.getBean(ItemRequestClient.class)).isNotNull();
    }
}

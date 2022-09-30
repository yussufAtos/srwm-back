package com.afp.iris.sr.wm.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.PingHealthIndicator;
import org.springframework.boot.actuate.system.DiskSpaceHealthIndicator;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatusController.class)
class StatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PingHealthIndicator pingHealthIndicator;

    @MockBean
    private DiskSpaceHealthIndicator diskSpaceHealthIndicator;

    @Test
    void should_return_OK_when_all_health_indicators_are_up() throws Exception {

        when(pingHealthIndicator.getHealth(true)).thenReturn(up());
        when(diskSpaceHealthIndicator.getHealth(true)).thenReturn(up());

        this.mockMvc.perform(get("/status"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        verify(pingHealthIndicator, times(1)).getHealth(true);
        verify(diskSpaceHealthIndicator, times(1)).getHealth(true);
    }

    @Test
    void should_return_KO_when_ping_health_indicator_is_down() throws Exception {

        when(pingHealthIndicator.getHealth(true)).thenReturn(down());
        when(diskSpaceHealthIndicator.getHealth(true)).thenReturn(up());

        this.mockMvc.perform(get("/status"))
                .andExpect(status().isOk())
                .andExpect(content().string("KO"));

        verify(pingHealthIndicator, times(1)).getHealth(true);
        verify(diskSpaceHealthIndicator, times(1)).getHealth(true);
    }

    @Test
    void should_return_KO_when_disk_space_health_indicator_is_down() throws Exception {

        when(pingHealthIndicator.getHealth(true)).thenReturn(up());
        when(diskSpaceHealthIndicator.getHealth(true)).thenReturn(down());

        this.mockMvc.perform(get("/status"))
                .andExpect(status().isOk())
                .andExpect(content().string("KO"));

        verify(pingHealthIndicator, times(1)).getHealth(true);
        verify(diskSpaceHealthIndicator, times(1)).getHealth(true);
    }

    private Health down() {
        return Health.down().build();
    }

    private Health up() {
        return Health.up().build();
    }

}
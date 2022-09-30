package com.afp.iris.sr.wm.presentation;

import com.afp.iris.sr.wm.config.monitoring.SoftConfigurations;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MonitoringController.class)
class MonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SoftConfigurations softConfigurations;

    public static final String SOFT_CONFIGURATIONS_URL = "/softsconfigurations";

    @Test
    void should_return_application_soft_configurations() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        this.mockMvc.perform(get(SOFT_CONFIGURATIONS_URL))
                .andExpect(content().string(objectMapper.writeValueAsString(softConfigurations)))
                .andExpect(status().isOk());
    }
}
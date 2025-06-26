package br.com.cloudport.servicoyard.controllers;

import br.com.cloudport.servicoyard.model.Container;
import br.com.cloudport.servicoyard.service.ContainerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContainerController.class)
class ContainerControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContainerService containerService;

    @Test
    void listContainers() throws Exception {
        Container c = new Container(1L, "C1", "A1");
        when(containerService.listContainers()).thenReturn(Collections.singletonList(c));

        mockMvc.perform(get("/yard/containers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void addContainer() throws Exception {
        Container c = new Container(null, "C1", "A1");
        Container saved = new Container(1L, "C1", "A1");
        when(containerService.addContainer(c)).thenReturn(saved);

        mockMvc.perform(post("/yard/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(c)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }
}

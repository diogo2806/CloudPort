package br.com.cloudport.servicoautenticacao.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoleControllerNotFoundIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/roles/{name} deve retornar 404 quando o role não existir")
    void shouldReturn404WhenRoleIsNotFoundByName() throws Exception {
        mockMvc.perform(get("/api/roles/{name}", "role-inexistente"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/roles/{id} deve retornar 404 quando o role não existir")
    void shouldReturn404WhenRoleIsNotFoundOnUpdate() throws Exception {
        mockMvc.perform(put("/api/roles/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Novo Nome\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/roles/{id} deve retornar 404 quando o role não existir")
    void shouldReturn404WhenRoleIsNotFoundOnDelete() throws Exception {
        mockMvc.perform(delete("/api/roles/{id}", 999L))
                .andExpect(status().isNotFound());
    }
}

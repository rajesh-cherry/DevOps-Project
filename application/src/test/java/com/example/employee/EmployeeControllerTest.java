package com.example.employee;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerTest {
    @Autowired MockMvc mvc;

    @Test
    void returnsEmployees() throws Exception {
        mvc.perform(get("/api/employees"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Ava Patel"));
    }

    @Test
    void returnsEmployeeById() throws Exception {
        mvc.perform(get("/api/employees/2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("QA Engineer"));
    }
}

package com.example.transactionmonitoring.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RuleValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateAmountThresholdRuleWithoutVelocityParameters() throws Exception {
        mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test Amount Rule",
                                  "type": "AMOUNT_THRESHOLD",
                                  "description": "test",
                                  "threshold": 10000,
                                  "severity": "HIGH",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("AMOUNT_THRESHOLD"))
                .andExpect(jsonPath("$.threshold").value(10000));
    }

    @Test
    void shouldCreateVelocityRuleWithoutThreshold() throws Exception {
        mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test Velocity Rule",
                                  "type": "VELOCITY",
                                  "description": "test",
                                  "timeWindow": 10,
                                  "maxCount": 5,
                                  "severity": "HIGH",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("VELOCITY"))
                .andExpect(jsonPath("$.timeWindow").value(10))
                .andExpect(jsonPath("$.maxCount").value(5));
    }

    @Test
    void shouldRejectVelocityRuleWithoutRequiredParameters() throws Exception {
        mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Bad Velocity Rule",
                                  "type": "VELOCITY",
                                  "severity": "HIGH",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}

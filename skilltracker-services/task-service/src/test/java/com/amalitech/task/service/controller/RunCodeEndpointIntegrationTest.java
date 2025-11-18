package com.amalitech.task.service.controller;

import com.amalitech.task.service.dto.request.RunCodeRequest;
import com.amalitech.task.service.dto.response.RunCodeResponse;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskDefinition;
import com.amalitech.task.service.model.content.impl.CodingTaskContent;
import com.amalitech.task.service.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the run-code endpoint.
 * Tests the full flow from REST API to service layer.
 * 
 * Note: These tests are disabled by default as they require:
 * - Full Spring Boot context initialization
 * - Config server availability
 * - Judge0 instance availability
 * 
 * They should be run in a full integration test environment.
 */
@Disabled("Requires full integration test environment with config server and Judge0")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "client.judge0-api.base-url=http://localhost:2358",
        "client.connect-timeout-ms=5000",
        "client.judge0-api.response-timeout-ms=30000"
})
class RunCodeEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    private UUID taskId;
    private Task task;

    @BeforeEach
    void setUp() {
        // Create test task with coding content
        taskId = UUID.randomUUID();

        CodingTaskContent content = CodingTaskContent.builder()
                .prompt("Write a function that reverses a string")
                .examples(List.of(
                        CodingTaskContent.Example.builder()
                                .input("hello")
                                .output("olleh")
                                .build()
                ))
                .constraints("Use string slicing")
                .testCases(List.of(
                        CodingTaskContent.TestCase.builder()
                                .input("hello")
                                .expectedOutput("olleh")
                                .build(),
                        CodingTaskContent.TestCase.builder()
                                .input("world")
                                .expectedOutput("dlrow")
                                .build()
                ))
                .build();

        task = Task.builder()
                .id(taskId)
                .title("String Reversal")
                .description("Reverse a string")
                .content(content)
                .build();
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void testRunCode_MissingTaskId() throws Exception {
        RunCodeRequest request = new RunCodeRequest(null, "code", 71);

        mockMvc.perform(post("/api/v1/submissions/run-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void testRunCode_MissingCode() throws Exception {
        RunCodeRequest request = new RunCodeRequest(taskId, null, 71);

        mockMvc.perform(post("/api/v1/submissions/run-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void testRunCode_MissingLanguageId() throws Exception {
        RunCodeRequest request = new RunCodeRequest(taskId, "code", null);

        mockMvc.perform(post("/api/v1/submissions/run-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void testRunCode_TaskNotFound() throws Exception {
        RunCodeRequest request = new RunCodeRequest(UUID.randomUUID(), "code", 71);

        mockMvc.perform(post("/api/v1/submissions/run-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void testRunCode_Unauthenticated() throws Exception {
        RunCodeRequest request = new RunCodeRequest(taskId, "code", 71);

        mockMvc.perform(post("/api/v1/submissions/run-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void testRunCode_ValidRequestStructure() throws Exception {
        // Save task to repository
        taskRepository.save(task);

        RunCodeRequest request = new RunCodeRequest(
                taskId,
                "def reverse(s): return s[::-1]",
                71
        );

        MvcResult result = mockMvc.perform(post("/api/v1/submissions/run-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Code executed successfully"))
                .andExpect(jsonPath("$.data").exists())
                .andReturn();

        // Verify response structure
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("testResults"));
        assertTrue(content.contains("allTestsPassed"));
        assertTrue(content.contains("testsPassed"));
        assertTrue(content.contains("testsTotal"));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void testRunCode_ResponseHasRequiredFields() throws Exception {
        taskRepository.save(task);

        RunCodeRequest request = new RunCodeRequest(
                taskId,
                "def reverse(s): return s[::-1]",
                71
        );

        mockMvc.perform(post("/api/v1/submissions/run-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.testResults").isArray())
                .andExpect(jsonPath("$.data.allTestsPassed").isBoolean())
                .andExpect(jsonPath("$.data.testsPassed").isNumber())
                .andExpect(jsonPath("$.data.testsTotal").isNumber())
                .andExpect(jsonPath("$.data.avgExecutionTimeMs").exists())
                .andExpect(jsonPath("$.data.avgMemoryUsedKb").exists());
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void testRunCode_TestResultHasRequiredFields() throws Exception {
        taskRepository.save(task);

        RunCodeRequest request = new RunCodeRequest(
                taskId,
                "def reverse(s): return s[::-1]",
                71
        );

        mockMvc.perform(post("/api/v1/submissions/run-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.testResults[0].passed").isBoolean())
                .andExpect(jsonPath("$.data.testResults[0].input").exists())
                .andExpect(jsonPath("$.data.testResults[0].expectedOutput").exists())
                .andExpect(jsonPath("$.data.testResults[0].actualOutput").exists())
                .andExpect(jsonPath("$.data.testResults[0].executionTimeMs").exists())
                .andExpect(jsonPath("$.data.testResults[0].memoryUsedKb").exists())
                .andExpect(jsonPath("$.data.testResults[0].statusDescription").exists());
    }
}

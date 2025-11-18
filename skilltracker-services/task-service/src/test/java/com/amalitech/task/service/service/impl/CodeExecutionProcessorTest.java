package com.amalitech.task.service.service.impl;

import com.amalitech.task.service.dto.client.request.Judge0SubmissionRequest;
import com.amalitech.task.service.dto.client.response.Judge0SubmissionResponse;
import com.amalitech.task.service.dto.response.RunCodeResponse;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.content.impl.CodingTaskContent;
import com.amalitech.task.service.repository.TaskRepository;
import com.amalitech.task.service.service.Judge0Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodeExecutionProcessorTest {

    @Mock
    private Judge0Client judge0Client;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private CodeExecutionProcessor codeExecutionProcessor;

    private UUID taskId;
    private String code;
    private Integer languageId;
    private Task task;
    private CodingTaskContent taskContent;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        code = "print('hello')";
        languageId = 71; // Python

        taskContent = CodingTaskContent.builder()
                .prompt("Write a program that prints hello")
                .examples(List.of(
                        CodingTaskContent.Example.builder()
                                .input("")
                                .output("hello")
                                .build()
                ))
                .constraints("No constraints")
                .testCases(List.of(
                        CodingTaskContent.TestCase.builder()
                                .input("")
                                .expectedOutput("hello")
                                .build(),
                        CodingTaskContent.TestCase.builder()
                                .input("")
                                .expectedOutput("hello")
                                .build()
                ))
                .build();

        task = Task.builder()
                .id(taskId)
                .title("Hello World")
                .description("Test")
                .content(taskContent)
                .build();
    }

    @Test
    void testExecuteCode_Success() {
        Judge0SubmissionResponse response1 = createSuccessResponse("hello\n");
        Judge0SubmissionResponse response2 = createSuccessResponse("hello\n");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(judge0Client.executeSubmission(any(Judge0SubmissionRequest.class)))
                .thenReturn(Mono.just(response1), Mono.just(response2));

        StepVerifier.create(codeExecutionProcessor.executeCode(taskId, code, languageId))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(2, result.getTestsTotal());
                    assertEquals(2, result.getTestsPassed());
                    assertTrue(result.isAllTestsPassed());
                    assertEquals(2, result.getTestResults().size());
                    assertTrue(result.getTestResults().get(0).isPassed());
                })
                .verifyComplete();
    }

    @Test
    void testExecuteCode_TaskNotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        StepVerifier.create(codeExecutionProcessor.executeCode(taskId, code, languageId))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException 
                        && e.getMessage().contains("Task not found"))
                .verify();
    }

    @Test
    void testExecuteCode_NotACodingTask() {
        // Create a non-coding task - use a different content type or null
        Task nonCodingTask = Task.builder()
                .id(taskId)
                .title("Test")
                .description("Test")
                .content(null) // Null content
                .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(nonCodingTask));

        StepVerifier.create(codeExecutionProcessor.executeCode(taskId, code, languageId))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException 
                        && e.getMessage().contains("not a coding task"))
                .verify();
    }

    @Test
    void testExecuteCode_NoTestCases() {
        CodingTaskContent contentNoTests = CodingTaskContent.builder()
                .prompt("Test")
                .examples(List.of())
                .constraints("Test")
                .testCases(List.of())
                .build();

        Task taskNoTests = Task.builder()
                .id(taskId)
                .content(contentNoTests)
                .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(taskNoTests));

        StepVerifier.create(codeExecutionProcessor.executeCode(taskId, code, languageId))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException 
                        && e.getMessage().contains("no test cases"))
                .verify();
    }

    @Test
    void testExecuteCode_PartialFailure() {
        Judge0SubmissionResponse successResponse = createSuccessResponse("hello\n");
        Judge0SubmissionResponse failureResponse = createFailureResponse("world\n");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(judge0Client.executeSubmission(any(Judge0SubmissionRequest.class)))
                .thenReturn(Mono.just(successResponse), Mono.just(failureResponse));

        StepVerifier.create(codeExecutionProcessor.executeCode(taskId, code, languageId))
                .assertNext(result -> {
                    assertEquals(2, result.getTestsTotal());
                    assertEquals(1, result.getTestsPassed());
                    assertFalse(result.isAllTestsPassed());
                    assertTrue(result.getTestResults().get(0).isPassed());
                    assertFalse(result.getTestResults().get(1).isPassed());
                    assertEquals("Wrong Answer", result.getTestResults().get(1).getStatusDescription());
                })
                .verifyComplete();
    }

    @Test
    void testExecuteCode_ExecutionMetrics() {
        Judge0SubmissionResponse response = createSuccessResponse("hello\n");
        response.setTime(0.05); // 50ms
        response.setMemory(1024); // 1024KB

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(judge0Client.executeSubmission(any(Judge0SubmissionRequest.class)))
                .thenReturn(Mono.just(response), Mono.just(response));

        StepVerifier.create(codeExecutionProcessor.executeCode(taskId, code, languageId))
                .assertNext(result -> {
                    assertEquals(50.0, result.getAvgExecutionTimeMs());
                    assertEquals(1024, result.getAvgMemoryUsedKb());
                })
                .verifyComplete();
    }

    @Test
    void testExecuteCode_OutputNormalization() {
        Judge0SubmissionResponse response = createSuccessResponse("hello\r\n\r\n");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(judge0Client.executeSubmission(any(Judge0SubmissionRequest.class)))
                .thenReturn(Mono.just(response), Mono.just(response));

        StepVerifier.create(codeExecutionProcessor.executeCode(taskId, code, languageId))
                .assertNext(result -> {
                    assertTrue(result.getTestResults().get(0).isPassed());
                    assertEquals("hello", result.getTestResults().get(0).getActualOutput());
                    assertEquals("hello", result.getTestResults().get(0).getExpectedOutput());
                })
                .verifyComplete();
    }

    @Test
    void testExecuteCode_CompilationError() {
        Judge0SubmissionResponse response = new Judge0SubmissionResponse();
        response.setStderr("Syntax error");
        Judge0SubmissionResponse.Judge0Status status = new Judge0SubmissionResponse.Judge0Status();
        status.setId(6); // Compilation error
        status.setDescription("Compilation Error");
        response.setStatus(status);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(judge0Client.executeSubmission(any(Judge0SubmissionRequest.class)))
                .thenReturn(Mono.just(response), Mono.just(response));

        StepVerifier.create(codeExecutionProcessor.executeCode(taskId, code, languageId))
                .assertNext(result -> {
                    assertEquals(2, result.getTestsTotal());
                    assertEquals(0, result.getTestsPassed());
                    assertFalse(result.isAllTestsPassed());
                    assertEquals("Compilation Error", result.getTestResults().get(0).getStatusDescription());
                })
                .verifyComplete();
    }

    @Test
    void testExecuteCode_StdoutStderr() {
        Judge0SubmissionResponse response = createSuccessResponse("hello\n");
        response.setStderr("warning: unused variable");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(judge0Client.executeSubmission(any(Judge0SubmissionRequest.class)))
                .thenReturn(Mono.just(response), Mono.just(response));

        StepVerifier.create(codeExecutionProcessor.executeCode(taskId, code, languageId))
                .assertNext(result -> {
                    assertEquals("hello\n", result.getStdout());
                    assertEquals("warning: unused variable", result.getStderr());
                })
                .verifyComplete();
    }

    private Judge0SubmissionResponse createSuccessResponse(String output) {
        Judge0SubmissionResponse response = new Judge0SubmissionResponse();
        response.setStdout(output);
        Judge0SubmissionResponse.Judge0Status status = new Judge0SubmissionResponse.Judge0Status();
        status.setId(3);
        status.setDescription("Accepted");
        response.setStatus(status);
        response.setTime(0.1);
        response.setMemory(512);
        return response;
    }

    private Judge0SubmissionResponse createFailureResponse(String output) {
        Judge0SubmissionResponse response = new Judge0SubmissionResponse();
        response.setStdout(output);
        Judge0SubmissionResponse.Judge0Status status = new Judge0SubmissionResponse.Judge0Status();
        status.setId(3);
        status.setDescription("Accepted");
        response.setStatus(status);
        response.setTime(0.1);
        response.setMemory(512);
        return response;
    }
}

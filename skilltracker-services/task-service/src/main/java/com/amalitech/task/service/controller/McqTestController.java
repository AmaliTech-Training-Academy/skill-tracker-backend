package com.amalitech.task.service.controller;

import com.amalitech.task.service.dto.request.GenerateTaskRequest;
import com.amalitech.task.service.events.TaskEventProducer;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/api/test/mcq")
@RequiredArgsConstructor
@Tag(name = "Test MCQ Generation", description = "Test endpoints for MCQ generation")
public class McqTestController {

    private final TaskEventProducer taskEventProducer;

    @PostMapping("/generate")
    @Operation(summary = "Test MCQ generation",
            description = "Trigger a single MCQ generation for testing")
    public ResponseEntity<Map<String, String>> testMcqGeneration(
            @RequestParam String skillName,
            @RequestParam(defaultValue = "EASY") TaskDifficulty difficulty,
            @RequestParam(defaultValue = "General Concepts") String topic
    ) {
        GenerateTaskRequest request = new GenerateTaskRequest(
                TaskType.MULTIPLE_CHOICE,
                skillName,
                difficulty,
                topic,
                null
        );

        taskEventProducer.requestSpecificTaskGeneration(request);

        String checkAtUrl = UriComponentsBuilder
                .fromPath("/api/tasks/availability")
                .queryParam("skillName", skillName)
                .queryParam("difficulty", difficulty.name())
                .build()
                .toUriString();

        String safeMessage = "MCQ generation request accepted and queued.";

        return ResponseEntity.accepted().body(Map.of(
                "status", "accepted",
                "message", safeMessage,
                "checkAt", checkAtUrl
        ));
    }
}
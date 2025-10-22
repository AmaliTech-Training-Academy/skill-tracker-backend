//package com.amalitech.task.service.controller;
//
//import com.amalitech.task.service.dto.SubmissionResultDTO;
//import com.amalitech.task.service.model.enums.TaskType;
//import com.amalitech.task.service.model.submission.impl.CodingSubmissionAnswer;
//import com.amalitech.task.service.model.submission.impl.EssaySubmissionAnswer;
//import com.amalitech.task.service.model.submission.impl.McqSubmissionAnswer;
////import com.amalitech.task.service.service.CodingService;
////import com.amalitech.task.service.service.EssayService;
////import com.amalitech.task.service.service.McqService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/api/submissions")
//@RequiredArgsConstructor
//@Tag(name = "Submissions", description = "Task submission endpoints")
//public class SubmissionController {
//
//    private final McqService mcqService;
//    private final CodingService codingService;
//    private final EssayService essayService;
//
//    @PostMapping
//    @Operation(summary = "Submit answer", description = "Submit answer for any task type")
//    public ResponseEntity<SubmissionResultDTO> submitAnswer(
//            @Valid @RequestBody SubmitAnswerRequest request,
//            @RequestHeader("X-User-Id") UUID userId
//    ) {
//        // Route based on answer type
//        if (request.getAnswer() instanceof McqSubmissionAnswer) {
//            return ResponseEntity.ok(mcqService.submitMcqAnswer(userId, request));
//        } else if (request.getAnswer() instanceof CodingSubmissionAnswer) {
//            return ResponseEntity.ok(codingService.submitCodingAnswer(userId, request));
//        } else if (request.getAnswer() instanceof EssaySubmissionAnswer) {
//            return ResponseEntity.ok(essayService.submitEssayAnswer(userId, request));
//        }
//
//        return ResponseEntity.badRequest().build();
//    }
//
//    // Backward compatibility endpoints
//    @PostMapping("/mcq")
//    @Operation(summary = "Submit MCQ answer")
//    public ResponseEntity<SubmissionResultDTO> submitMcq(
//            @Valid @RequestBody SubmitAnswerRequest request,
//            @RequestHeader("X-User-Id") UUID userId
//    ) {
//        return ResponseEntity.ok(mcqService.submitMcqAnswer(userId, request));
//    }
//
//    @PostMapping("/coding")
//    @Operation(summary = "Submit coding solution")
//    public ResponseEntity<SubmissionResultDTO> submitCoding(
//            @Valid @RequestBody SubmitAnswerRequest request,
//            @RequestHeader("X-User-Id") UUID userId
//    ) {
//        return ResponseEntity.ok(codingService.submitCodingAnswer(userId, request));
//    }
//
//    @PostMapping("/essay")
//    @Operation(summary = "Submit essay")
//    public ResponseEntity<SubmissionResultDTO> submitEssay(
//            @Valid @RequestBody SubmitAnswerRequest request,
//            @RequestHeader("X-User-Id") UUID userId
//    ) {
//        return ResponseEntity.ok(essayService.submitEssayAnswer(userId, request));
//    }
//}

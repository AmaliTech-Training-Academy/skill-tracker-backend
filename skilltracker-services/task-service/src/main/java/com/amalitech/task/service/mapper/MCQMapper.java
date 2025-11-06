package com.amalitech.task.service.mapper;

import com.amalitech.task.service.dto.MCQquestionDTO;
import com.amalitech.task.service.dto.response.McqResponseDTO;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class MCQMapper {

    public static McqResponseDTO mapJsonToMcqResponse(String jsonResponse) {
        Gson gson = new Gson();
        JsonArray questionsArray = gson.fromJson(jsonResponse, JsonArray.class);

        List<MCQquestionDTO> mcqQuestions = new ArrayList<>();

        for (int i = 0; i < questionsArray.size(); i++) {
            JsonObject questionJson = questionsArray.get(i).getAsJsonObject();

            MCQquestionDTO mcqQuestion = MCQquestionDTO.builder()
                    .question_duration(questionJson.get("question_minute_duration").getAsInt())
                    .question_text(questionJson.get("question_text").getAsString())
                    .options(gson.fromJson(questionJson.get("options"), List.class))
                    .hint(questionJson.get("question_hint").getAsString())
                    .correct_answer(questionJson.get("correct_answer_index").getAsString())
                    .explanation(questionJson.get("explanation").getAsString())
                    .build();

            mcqQuestions.add(mcqQuestion);
        }

        return McqResponseDTO.builder()
                .mcqQuestion(mcqQuestions)
                .build();
    }

//    public static void main(String[] args) {
//        String jsonResponse = "[...]"; // Your JSON array from Gemini
//
//        McqResponseDTO response = mapJsonToMcqResponse(jsonResponse);
//
//        System.out.println("Total questions: " + response.getMcqQuestion().size());
//    }
}
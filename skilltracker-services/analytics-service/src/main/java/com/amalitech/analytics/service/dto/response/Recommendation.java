package com.amalitech.analytics.service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Recommendation {
    private String skillGapArea; // e.g., "Java Loops" (AC 4)
    private String recommendationText; // "You seem to be struggling..." (AC 9)
}
package com.amalitech.analytics.service.client;
import com.amalitech.analytics.service.dto.response.Recommendation;
import com.amalitech.analytics.service.dto.response.SkillProgressSummary;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
public class FeedbackServiceClient {

    /**
     * Calls the Feedback Service to get AI recommendations (AC 4, 9)
     * It sends the user's progress summary as context.
     */
    public List<Recommendation> getRecommendations(List<SkillProgressSummary> summaries) {
        if (summaries.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(
                new Recommendation(
                        summaries.get(0).getSkillName(),
                        "Mock AI Recommendation: Keep practicing your " + summaries.get(0).getSkillName() + " skills!"
                )
        );
    }
}
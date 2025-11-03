package com.amalitech.analytics.service.service;

import com.amalitech.analytics.service.client.FeedbackServiceClient;
import com.amalitech.analytics.service.client.TaskServiceClient;
import com.amalitech.analytics.service.dto.response.*;
import com.amalitech.analytics.service.events.NotificationEventPublisher;
import com.amalitech.analytics.service.model.SkillProgress;
import com.amalitech.analytics.service.model.UserGoal;
import com.amalitech.analytics.service.model.enums.GoalStatus;
import com.amalitech.analytics.service.model.enums.TaskType;
import com.amalitech.analytics.service.repository.SkillProgressRepository;
import com.amalitech.analytics.service.repository.UserGoalRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final SkillProgressRepository progressRepository;
    private final TaskServiceClient taskServiceClient;
    private final UserGoalRepository goalRepository;
    private final NotificationEventPublisher notificationPublisher;
    private final FeedbackServiceClient feedbackServiceClient;

    /**
     * Basic dashboard data fetch. No filtering yet.
     */
    public DashboardResponse getDashboardData(
            UUID userId,
            Optional<UUID> skillId,
            Optional<LocalDate> startDate,
            Optional<LocalDate> endDate) {

        log.info(userId.toString());
        LocalDateTime startDateTime = startDate.map(LocalDate::atStartOfDay).orElse(LocalDateTime.now().minusDays(90));
        LocalDateTime endDateTime = endDate.map(d -> d.atTime(23, 59, 59)).orElse(LocalDateTime.now());

        List<SkillProgress> progressData = progressRepository.findUserData(
                userId,
                skillId,
                startDateTime,
                endDateTime);

        List<SkillProgressSummary> summaries = buildSkillSummaries(progressData);
        log.info(summaries.toString());
        List<GoalProgress> goalProgressList = buildGoalProgress(userId, summaries);
        List<Recommendation> recommendations;
        try {
            recommendations = feedbackServiceClient.getRecommendations(summaries);
        } catch (Exception e) {
            log.warn("Could not fetch AI recommendations for user {}: {}", userId, e.getMessage());
            recommendations = Collections.emptyList();
        }

        return DashboardResponse.builder()
                .skillSummaries(summaries)
                .goalProgress(goalProgressList)
                .build();
    }

    /**
     * This method is triggered by the TaskEventListener.
     * It saves new progress and checks for milestone completion.
     */
    public void processNewProgress(UUID userId, UUID skillId, UUID taskId, Double score,
                                   TaskType taskType, Map<String, Double> rubricsScores) {
        SkillProgress newProgress = new SkillProgress(
                userId, skillId, taskId, score,
                taskType, rubricsScores
        );
        progressRepository.save(newProgress);


        List<UserGoal> activeGoals = goalRepository.findByUserIdAndSkillIdAndStatus(
                userId, skillId, GoalStatus.ACTIVE
        );

        for (UserGoal goal : activeGoals) {
            if (score >= goal.getTargetScore()) {
                goal.setStatus(GoalStatus.COMPLETED);
                goalRepository.save(goal);


                notificationPublisher.publishMilestoneEvent(userId,
                        "Goal Achieved!",
                        "You've completed your goal: " + goal.getGoalDescription());
            }
        }
    }

    private List<SkillProgressSummary> buildSkillSummaries(List<SkillProgress> progressData) {

        Map<UUID, List<SkillProgress>> groupedBySkill = progressData.stream()
                .collect(Collectors.groupingBy(SkillProgress::getSkillId));

        return groupedBySkill.entrySet().stream().map(entry -> {
            UUID skillId = entry.getKey();
            List<SkillProgress> skillEvents = entry.getValue();

            String skillName = taskServiceClient.getSkillName(skillId);


            List<DataPoint> dataPoints = skillEvents.stream()
                    .map(sp -> new DataPoint(sp.getTimestamp(), sp.getScore(), sp.getTaskType(), sp.getRubricsScores()))
                    .collect(Collectors.toList());

            return SkillProgressSummary.builder()
                    .skillId(skillId)
                    .skillName(skillName)
                    .currentScore(skillEvents.get(skillEvents.size() - 1).getScore())
                    .historicalData(dataPoints)
                    .build();
        }).collect(Collectors.toList());
    }

    public UserGoal createUserGoal(UUID userId, UserGoalDto goalDto) {
        UserGoal goal = new UserGoal();
        goal.setUserId(userId);
        goal.setSkillId(goalDto.getSkillId());
        goal.setGoalDescription(goalDto.getGoalDescription());
        goal.setTargetScore(goalDto.getTargetScore());
        goal.setTargetDate(goalDto.getTargetDate());
        goal.setStatus(GoalStatus.ACTIVE);

        return goalRepository.save(goal);
    }

    public List<UserGoal> getActiveGoals(UUID userId) {
        return goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE);
    }

    private List<GoalProgress> buildGoalProgress(UUID userId, List<SkillProgressSummary> summaries) {
        Map<UUID, Double> currentScores = summaries.stream()
                .collect(Collectors.toMap(SkillProgressSummary::getSkillId, SkillProgressSummary::getCurrentScore));

        List<UserGoal> activeGoals = getActiveGoals(userId);

        return activeGoals.stream().map(goal -> {
            Double currentScore = currentScores.getOrDefault(goal.getSkillId(), 0.0);
            Double percentage = (currentScore / goal.getTargetScore()) * 100.0;

            return GoalProgress.builder()
                    .goalDescription(goal.getGoalDescription())
                    .currentScore(currentScore)
                    .targetScore(goal.getTargetScore())
                    .percentageComplete(Math.min(percentage, 100.0))
                    .build();
        }).collect(Collectors.toList());
    }
}
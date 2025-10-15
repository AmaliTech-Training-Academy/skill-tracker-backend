group "default" {
  targets = [
    "user-service",
    "task-service",
    "analytics-service",
    "feedback-service",
    "gamification-service",
    "notification-service",
    "payment-service",
    "practice-service",
    "bff-service",
  ]
}

target "user-service" {
  context = "."
  dockerfile = "skilltracker-services/user-service/Dockerfile"
  tags = ["skilltracker/user-service:latest"]
}

target "task-service" {
  context = "."
  dockerfile = "skilltracker-services/task-service/Dockerfile"
  tags = ["skilltracker/task-service:latest"]
}

target "analytics-service" {
  context = "."
  dockerfile = "skilltracker-services/analytics-service/Dockerfile"
  tags = ["skilltracker/analytics-service:latest"]
}

target "feedback-service" {
  context = "."
  dockerfile = "skilltracker-services/feedback-service/Dockerfile"
  tags = ["skilltracker/feedback-service:latest"]
}

target "gamification-service" {
  context = "."
  dockerfile = "skilltracker-services/gamification-service/Dockerfile"
  tags = ["skilltracker/gamification-service:latest"]
}

target "notification-service" {
  context = "."
  dockerfile = "skilltracker-services/notification-service/Dockerfile"
  tags = ["skilltracker/notification-service:latest"]
}

target "payment-service" {
  context = "."
  dockerfile = "skilltracker-services/payment-service/Dockerfile"
  tags = ["skilltracker/payment-service:latest"]
}

target "practice-service" {
  context = "."
  dockerfile = "skilltracker-services/practice-service/Dockerfile"
  tags = ["skilltracker/practice-service:latest"]
}

target "bff-service" {
  context = "."
  dockerfile = "skilltracker-services/bff-service/Dockerfile"
  tags = ["skilltracker/bff-service:latest"]
}

classDiagram
direction BT
class AbstractAuditable {
    Date  createdDate
    Date  lastModifiedDate
}
class AbstractPersistable {
    PK  id
}
class ProgrammingLanguage {
    UUID  id
    String  name
}
class SkillView {
    UUID  id
    String  description
    String  name
}
class Task {
    UUID  id
    TaskContent  content
    LocalDateTime  createdAt
    String  description
    TaskDifficulty  difficulty
    Integer  estimatedDurationInMinutes
    Boolean  isPublished
    String  title
    TaskType  type
    LocalDateTime  updatedAt
    int  version
    Integer  xpReward
}
class TaskDefinition {
    UUID  id
    LocalDateTime  createdAt
    int  latestVersion
    String  title
    LocalDateTime  updatedAt
}
class TaskPrerequisite {
    UUID  id
    LocalDateTime  createdAt
    UUID  prerequisiteTaskId
}
class TaskStarterCode {
    UUID  id
    String  code
    LocalDateTime  createdAt
}
class TaskSubmission {
    UUID  id
    SubmissionAnswer  answer
    LocalDateTime  evaluatedAt
    SubmissionFeedback  feedback
    Boolean  isCorrect
    Integer  scoreEarned
    SubmissionStatus  status
    LocalDateTime  submittedAt
    UUID  userId
}
class UserView {
    UUID  id
    String  email
    String  fullName
    String  role
}

AbstractAuditable  --|>  AbstractPersistable 
Task "0..*" --> "1" TaskDefinition 
TaskDefinition "0..*" --> "1" SkillView 
TaskPrerequisite "0..*" --> "0..1" Task 
TaskStarterCode "0..*" --> "1" ProgrammingLanguage 
TaskStarterCode "0..*" --> "0..1" Task 
TaskSubmission "0..*" --> "0..1" Task 

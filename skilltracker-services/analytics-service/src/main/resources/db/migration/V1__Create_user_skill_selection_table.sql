-- Create user_skill_selection table
CREATE TABLE IF NOT EXISTS user_skill_selection (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skill_id UUID NOT NULL,
    skill_name VARCHAR(255),
    initial_claim_level VARCHAR(255) NOT NULL,
    event_id UUID
);

-- Create user_skill_supported_task_types table for ElementCollection
CREATE TABLE IF NOT EXISTS user_skill_supported_task_types (
    user_skill_selection_id UUID NOT NULL,
    task_type VARCHAR(255),
    PRIMARY KEY (user_skill_selection_id, task_type),
    CONSTRAINT fk_user_skill_selection 
        FOREIGN KEY (user_skill_selection_id) 
        REFERENCES user_skill_selection(id) ON DELETE CASCADE
);

-- Create index for event_id
CREATE INDEX IF NOT EXISTS idx_user_skill_selection_event_id 
    ON user_skill_selection(event_id);

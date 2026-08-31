-- Add recurrence support to tasks table
ALTER TABLE tasks ADD COLUMN parent_task_id UUID;
ALTER TABLE tasks ADD COLUMN recurrence_frequency VARCHAR(10);
ALTER TABLE tasks ADD COLUMN recurrence_end_date DATE;
ALTER TABLE tasks ADD COLUMN recurrence_weekday INTEGER;

-- Add constraints
ALTER TABLE tasks ADD CONSTRAINT fk_parent_task_id FOREIGN KEY (parent_task_id) REFERENCES tasks(id);
ALTER TABLE tasks ADD CONSTRAINT check_recurrence_weekday CHECK (recurrence_weekday >= 0 AND recurrence_weekday <= 6);

-- Add indexes for recurrence queries
CREATE INDEX idx_tasks_parent_task_id ON tasks(parent_task_id);
CREATE INDEX idx_tasks_parent_completed ON tasks(parent_task_id, completed);

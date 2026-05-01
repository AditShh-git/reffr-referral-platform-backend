ALTER TABLE user_skills
    RENAME COLUMN skill TO skill_name;

ALTER TABLE user_skills
    ADD COLUMN IF NOT EXISTS category VARCHAR(30);

ALTER TABLE user_skills
    DROP CONSTRAINT IF EXISTS user_skills_user_id_skill_key;

ALTER TABLE user_skills
    DROP CONSTRAINT IF EXISTS uk_user_skills_user_id_skill_name;

ALTER TABLE user_skills
    ADD CONSTRAINT uk_user_skills_user_id_skill_name UNIQUE (user_id, skill_name);

DROP INDEX IF EXISTS idx_user_skills_skill;
CREATE INDEX IF NOT EXISTS idx_user_skills_skill_name ON user_skills(skill_name);

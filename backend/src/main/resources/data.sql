-- System user for auditing fallback (batch jobs, startup operations)
-- Password: "system" encoded with BCrypt
-- Note: Using lowercase table name for H2 compatibility (DATABASE_TO_LOWER=TRUE)
INSERT INTO app_user (username, password)
VALUES ('system', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG');

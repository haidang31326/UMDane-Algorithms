CREATE TABLE submissions (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             problem_id BIGINT NOT NULL,
                             code TEXT NOT NULL,
                             language VARCHAR(20) NOT NULL,
                             status VARCHAR(30) NOT NULL, -- PENDING, ACCEPTED, WRONG_ANSWER, COMPILE_ERROR
                             runtime_ms INT DEFAULT 0,
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT fk_submissions_problems FOREIGN KEY (problem_id) REFERENCES problems(id)
);
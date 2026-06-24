CREATE TABLE problems (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          topic VARCHAR(50) NOT NULL,
                          keyword VARCHAR(50) NOT NULL,
                          title VARCHAR(255) NOT NULL,
                          description TEXT NOT NULL,
                          hint VARCHAR(50),
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Seed Users
-- Password is 'password' for both users (BCrypt hash: $2a$10$DGP6auoXk14kI61.dEuf.e5d3k.m6k8mP.J3P3U6B6hWzWn9LmWXy)
INSERT INTO users (username, password, email, role) VALUES 
('admin', '$2a$10$DGP6auoXk14kI61.dEuf.e5d3k.m6k8mP.J3P3U6B6hWzWn9LmWXy', 'admin@umdane.com', 'ADMIN'),
('user', '$2a$10$DGP6auoXk14kI61.dEuf.e5d3k.m6k8mP.J3P3U6B6hWzWn9LmWXy', 'user@umdane.com', 'USER');

-- Seed Problems
INSERT INTO problems (id, topic, keyword, title, description, hint) VALUES
(1, 'Array', 'two-sum', 'Two Sum', 'Viết chương trình đọc vào hai số nguyên từ standard input (cách nhau bởi khoảng trắng) và in ra tổng của chúng.', 'Đọc hai số nguyên bằng Scanner.'),
(2, 'Math', 'factorial', 'Factorial', 'Viết chương trình đọc vào một số nguyên N (0 <= N <= 12) từ standard input và in ra giai thừa của N.', 'Giai thừa của 0 là 1.'),
(3, 'String', 'reverse', 'Reverse String', 'Viết chương trình đọc vào một chuỗi ký tự từ standard input và in ra chuỗi đảo ngược của nó.', 'Sử dụng StringBuilder.reverse().');

-- Seed Test Cases for Two Sum (Problem 1)
INSERT INTO test_cases (problem_id, input_data, expected_output, is_hidden) VALUES
(1, '3 5', '8', FALSE),
(1, '-2 7', '5', FALSE),
(1, '0 0', '0', FALSE);

-- Seed Test Cases for Factorial (Problem 2)
INSERT INTO test_cases (problem_id, input_data, expected_output, is_hidden) VALUES
(2, '5', '120', FALSE),
(2, '0', '1', FALSE),
(2, '10', '3628800', FALSE);

-- Seed Test Cases for Reverse String (Problem 3)
INSERT INTO test_cases (problem_id, input_data, expected_output, is_hidden) VALUES
(3, 'hello', 'olleh', FALSE),
(3, 'world', 'dlrow', FALSE),
(3, 'a', 'a', FALSE);

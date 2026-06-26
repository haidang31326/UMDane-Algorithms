ALTER TABLE problems ADD COLUMN constraints TEXT;
ALTER TABLE problems ADD COLUMN time_limit INT DEFAULT 2000;
ALTER TABLE problems ADD COLUMN memory_limit INT DEFAULT 128;

-- Seed default constraints and limits for existing problems
UPDATE problems SET constraints = '1 <= N <= 10^5\nCác phần tử trong mảng có giá trị tuyệt đối không vượt quá 10^9.' WHERE constraints IS NULL;

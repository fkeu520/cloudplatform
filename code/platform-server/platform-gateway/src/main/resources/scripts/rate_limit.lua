-- rate_limit.lua
-- Redis 滑动窗口限流脚本
-- KEYS[1] = rate limit key (e.g. "rate:ip:1.2.3.4")
-- ARGV[1] = current timestamp (ms)
-- ARGV[2] = window size (ms)
-- ARGV[3] = max requests per window
-- ARGV[4] = unique request id (用于 ZADD member，确保同毫秒多次请求不冲突)
--
-- 返回值:
--   1 = 允许
--   0 = 拒绝 (当前窗口已达上限)

local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local max = tonumber(ARGV[3])
local reqId = ARGV[4]

-- 1. 清理窗口外的过期请求
redis.call('ZREMRANGEBYSCORE', key, 0, now - window)

-- 2. 统计当前窗口内的请求数
local count = redis.call('ZCARD', key)

-- 3. 判断是否超限
if count >= max then
  return 0
end

-- 4. 记录本次请求 (score = 时间戳, member = 唯一 reqId)
redis.call('ZADD', key, now, reqId)

-- 5. 设置 key 过期时间 (比窗口略大, 防止内存泄漏)
redis.call('PEXPIRE', key, window + 1000)

return 1

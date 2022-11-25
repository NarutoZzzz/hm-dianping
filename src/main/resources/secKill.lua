-- 1.参数列表
-- 1.1.优惠券id
local voucherId = ARGV[1]
-- 1.2.用户Id
local userId = ARGV[2]

-- 数据key
-- 库存id
local stockKey = "secKill:stock:" .. voucherId
-- 订单id
local orderKey = "secKill:order:" .. voucherId

-- 脚本业务
--判断库存是否充足 get stockKey
local val = redis.call('GET', stockKey)

if ((tonumber(val)) <= 0) then
    --库存不足
    return 1
end

--判断用户是否下单 SISMEMBER orderKey userId
if (redis.call('SISMEMBER', orderKey, userId) == 1) then
    --存在 说明是重复下单,返回2
    return 2
end

--扣库存 INCRBY stockKey -1
redis.call('INCRBY', stockKey, -1)

--下单(保存用户)sadd orderKey userId
redis.call('SADD', orderKey, userId)




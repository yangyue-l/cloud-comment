local voucherId = ARGV[1]
local userId = ARGV[2]

local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

--检查库存
local stock = redis.call('get',stockKey)
if(stock == false or tonumber(stock) <= 0) then
    --库存不足返回1
    return 1
end
--判断用户是否下单
if(redis.call('sismember',orderKey,userId) == 1) then
    --存在说明重复下单 返回2
    return 2
end
--减库存
redis.call('incrby', stockKey, -1)
--下单 保存用户
redis.call('sadd', orderKey, userId)

return 0
--local userKey=KEYS[1]用户键
--local stockKey=KEYS[2]库存键
--local saleFalg=KEYS[3]买完键
--local count=ARGV[1]减少的数量
--local falg=ARGV[2]
--用户已经存在就直接放回
if (redis.call('exists',KEYS[1])==1) then
    return 1
end
if (redis.call('exists', KEYS[2]) == 1) then
	local count=tonumber(ARGV[1])
	redis.call('incrby', KEYS[2], 0-count)
	local stock = tonumber(redis.call('get', KEYS[2]))
	--扣减成功
	if (stock > 0) then
		redis.call('set',KEYS[1],"true")
		return 2
	end
	--卖完
	if (stock == 0) then
		redis.call('set',KEYS[1],ARGV[1])
		redis.call('set', KEYS[3],"true")
		return 2
	end
	if (stock < 0) then
		--加回来
		redis.call('incrby', KEYS[2], count)
		return 0
	end
	return 0
end

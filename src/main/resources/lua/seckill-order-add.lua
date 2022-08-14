--local args = ngx.req.get_uri_args()
-- id = args["id"]
local redis = require "resty.redis"
local cache = redis:new()
local ok,err = cache:connect("121.37.228.35",6379)
ngx.header.content_type="application/json;charset=utf8"
--引入json库
local cjson = require "cjson"
--引入jwt模块
local jwttoken = require "token"
--引入redis
--local redis = require "redis-cluster"
--引入kafka
--local kafka = require "kafka"

--获取请求头中的令牌数据
local auth_header = ngx.var.http_Authorization
--调用令牌校验
local result = jwttoken.check(auth_header)

--如果code==200表示令牌校验通过
if result.code==200 then
	--响应结果
	local response = {}

	--获取id
	local uri_args = ngx.req.get_uri_args()
	local itemId = uri_args["itemId"]
	local amount = uri_args["amount"]
	local promoId = uri_args["promoId"]
	local promoToken = uri_args["promoToken"]
	--判断该商品用户是否已经在指定时间内购买过
	local userId = result["body"]["payload"]["userId"]
	--local userKey= "USER"..username.."ID"..id
	local token_model = redis.cache(userId)
	
     if token_model == ngx.null or token_model == nil then
           response["code"]=401
           response["message"]="登录过期，请重新登录"
           ngx.say(cjson.encode(response))
           return
     else
          ngx.exec("/seckill/order/createorder?id="..id"&itemId="..itemId"&amount="..amount"&promoId="..promoId"&promoToken="..promoToken)
     end
else
  -- 输出结果
	ngx.say(cjson.encode(result))
	ngx.exit(result.code)
end


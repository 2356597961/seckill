package com.xiaoliu.seckill.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.yeauty.annotation.*;
import org.yeauty.pojo.Session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*****
 * @Author: xiaoliu
 * @Description: com.seckill.message.config.NettyWebSocketServer
 ****/
@Component
@ServerEndpoint(path = "/ws/{userid}",port = "${ws.port}",host = "${ws.host}")
public class NettyWebSocketServer {
    /****
     * 定义一个Map存储所有会话
     */
    //用map管理endpoint对象
    //坑，一个要注意key是string,不然取不到值。
    public static Map<String, NettyWebSocketServer> sessionMap = new ConcurrentHashMap<>();

    //声明session对象，通过该对象可以发送消息给指定用户(Websocket的Session)
    private Session session;

    private String userId;
    //private static Map<String, Session> sessionMap = new HashMap<String,Session>();

    @Autowired
    private RedisTemplate redisTemplate;

    /****
     * 1.建立连接
     */
    @OnOpen
    public void onOpen(@PathVariable(value = "userid") String userid, Session session){
        this.userId=userid;
        this.session=session;
        //获取Session的ID
        String id = session.channel().id().toString();
        System.out.println("连接成功");
        //获取用户对应的会话对象
        NettyWebSocketServer nettyWebSocketServer = sessionMap.get(userid);
        if(nettyWebSocketServer!=null){
            //清理会话信息
            sessionMap.remove(userid);
            redisTemplate.boundHashOps("NettyWebSocketUser").delete(nettyWebSocketServer.session.channel().id().toString());
        }

        //存储用户会话信息
        sessionMap.put(userid,this);
        System.out.println(sessionMap.get(userid).session);
        //存储SessionID和userid的映射关系
        redisTemplate.boundHashOps("NettyWebSocketUser").put(id,userid);
    }


    /****
     * 2.关闭关闭
     */
    @OnClose
    public void onClose(Session session){
        //根据SessionID从Redis中查找userid
        String userid = redisTemplate.boundHashOps("NettyWebSocketUser").get(session.channel().id().toString()).toString();

        //根据userid删除SessionMap中的Session
        sessionMap.remove(userid);
        //删除Redis中userid的映射信息
        redisTemplate.boundHashOps("NettyWebSocketUser").delete(session.channel().id().toString());
    }


    /***
     * 3.发生异常
     */
    @OnError
    public void onError(Session session,Throwable throwable){
        Object userid = redisTemplate.boundHashOps("NettyWebSocketUser").get(session.channel().id().toString());
        System.out.println("用户ID"+userid+",通信发生异常！");
    }


    /****
     * 4.接收客户端发送的消息
     */
    @OnMessage
    public void onMessage(String message,Session session){
        String userid = (String)redisTemplate.boundHashOps("NettyWebSocketUser").get(session.channel().id().toString());
        System.out.println("用户ID"+userid+"发送的消息是："+message);
        //回复客户端
        session.sendText("您发送的消息是："+message);
    }


    /****
     * 5.主动给客户端发消息
     */
    public void sendMessage(String message,String userid){
        //获取用户会话
        NettyWebSocketServer nettyWebSocketServer = sessionMap.get(userid);
        //发送消息
        if(nettyWebSocketServer!=null){
            System.out.println("下单成功");
            nettyWebSocketServer.session.sendText(message);
        }else {
            System.out.println("发送消失失败");
        }
    }
}

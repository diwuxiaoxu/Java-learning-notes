
import lombok.extern.java.Log;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


@Log
public class NIOServer {
    // 消息缓存区
    private static ByteBuffer readBuf = ByteBuffer.allocate(1024);
    //保存的客户端
    static Set<SocketChannel> clients = new HashSet<SocketChannel>();
    // 选择器
    private static Selector selector;


    public static void main(String[] args) throws Exception {

        server();
    }

    private static void server() throws Exception{
        //获取一个channel
        ServerSocketChannel channel = ServerSocketChannel.open();
        //配置是否阻塞
        channel.configureBlocking(false);
        //获取socket, 一次服务
        ServerSocket server = channel.socket();
        //绑定服务及端口
        server.bind(new InetSocketAddress(InetAddress.getLocalHost(), 10010));
        //获取选择器
        selector = Selector.open();
        //将channel注册到selector，用于连接
        channel.register(selector, SelectionKey.OP_ACCEPT);

        log.info ("server is starting......");

        //Selector选择器一直在处理
        while(true){
            //等待需要处理的新事件，阻塞将一直持续到下一个传入事件
            selector.select();
            //获取所有接收事件（accept）的selection-key实例
            Set<SelectionKey> readKeys = selector.selectedKeys();

            Iterator<SelectionKey> iterator = readKeys.iterator();

            while (iterator.hasNext()){
                try {
                    SelectionKey key = iterator.next();
                    //检查事件是否是一个新的且已经准备就绪可以被接受的连接
                    if (key.isAcceptable()){
                        getConnect(key, selector);
                    }
                    //检查是否可以读取数据
                    if (key.isReadable()){
                        getMessage(key);
                    }
                    //处理完之后要将key删除，防止重复处理
                    iterator.remove();
                }catch (Exception e) {
                    log.info("错误："+ e);
                    e.printStackTrace();
                }

            }
        }
    }

    private static void getConnect(SelectionKey key, Selector selector) throws Exception{
        //要操作的message
        ByteBuffer msg = ByteBuffer.wrap("connect success".getBytes());
        ServerSocketChannel serverSocket = (ServerSocketChannel)key.channel();

        //接收一个连接
        SocketChannel client = serverSocket.accept();

        clients.add(client);

        //设置为非阻塞
        client.configureBlocking(false);
        //监听读写事件
        client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        //发送连接成功给客户端

        client.write(msg);

        log.info("a new socket connected...." + client);
    }

    private static void getMessage(SelectionKey key) throws Exception{
        readBuf.clear();
        SocketChannel client = (SocketChannel)key.channel();
        try {
            int count = client.read(readBuf);
            if (count == -1){
                client.shutdownOutput();
                client.shutdownInput();
                client.close();
                log.info("断开连接.....");
                clients.remove(key);
            }
            byte[] bytes = new byte[count];
            readBuf.flip();
            readBuf.get(bytes);
            String message = new String(bytes, 0, count);
            log.info("接收到信息："+ message);
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            sendMessages2Clients(message);
        } catch (IOException e) {
            key.cancel();
            client.close();
            log.info("端开连接");
            clients.remove(key);
        }

    }

    private static void sendMessages2Clients(String message) {
        clients.stream().forEach(client -> {
            try {
                client.write(ByteBuffer.wrap(message.getBytes()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}


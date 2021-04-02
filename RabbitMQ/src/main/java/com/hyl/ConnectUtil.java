package com.hyl;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 建立rabbitMQ的连接
 */
public class ConnectUtil {

    public static Connection getConnection() throws IOException, TimeoutException {

        //定义连接工厂
        ConnectionFactory factory = new ConnectionFactory();

        //定义主机

        factory.setHost("127.0.0.1");

        factory.setPort(5672);

        factory.setUsername("guest");
        factory.setPassword("guest");

        Connection connection = factory.newConnection();

       return connection;

    }
}

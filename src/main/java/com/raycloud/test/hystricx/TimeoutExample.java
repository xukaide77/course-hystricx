package com.raycloud.test.hystricx;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCommandAspect;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.concurrent.ExecutionException;

/**
 * <pre>
 *     这个例子主要描述了超时的处理方式
 *     这段代码包含了三部分内容，方法getUserById方法设置超时500毫秒，方法执行超时之后，将会立即调用fallback指定的defaultUser方法；
 *     第二部分getUserByIdTimeoutInterrupt类似于getUserById，这里超时将会立即抛出超时异常，并且执行的线程会被interrupt；
 *     第三部分getUserByIdTimeoutNonInterrupt和上面一样，只是抛出超时异常之后，不会interrupt线程，而是继续执行
 * </pre>
 * Created by liumingjian on 16/8/12.
 */
public class TimeoutExample {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        UserService userService = ctx.getBean(UserService.class);
        long start = System.currentTimeMillis();
        User user = userService.getUserById(1L);
        System.out.println(user + ", took:" + (System.currentTimeMillis() - start));
        start = System.currentTimeMillis();
        try {
            user = userService.getUserByIdTimeoutInterrupt(1L);
            System.out.println(user + ", took:" + (System.currentTimeMillis() - start));
        }catch (Exception e){
            System.err.println("超时异常:" + e.getMessage());
        }
        try {
            user = userService.getUserByIdTimeoutNonInterrupt(1L);
            System.out.println(user + ", took:" + (System.currentTimeMillis() - start));
        }catch (Exception e){
            System.err.println("超时异常:" + e.getMessage() + "，别着急...线程还未断开");
        }
        Thread.sleep(2000);
    }

    @Configuration
    @EnableAspectJAutoProxy
    public static class Config{

        @Bean
        public UserService userService(){
            return new UserService();
        }

        @Bean
        public HystrixCommandAspect hystrixAspect() {
            return new HystrixCommandAspect();
        }
    }

    public static class UserService {

        @HystrixCommand(commandProperties = {
                @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "500")
        }, fallbackMethod = "defaultUser")
        public User getUserById(Long id) throws InterruptedException {
            Thread.sleep(2000);
            return createUser(id, "Jacky LIU");
        }

        @HystrixCommand(commandProperties = {
                @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "500"),
                @HystrixProperty(name = "execution.isolation.thread.interruptOnTimeout", value = "true")
        })
        public User getUserByIdTimeoutInterrupt(Long id) throws InterruptedException {
            Thread.sleep(2000);
            System.out.println("代码还是执行了...");
            return defaultUser(id);
        }

        @HystrixCommand(commandProperties = {
                @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "500"),
                @HystrixProperty(name = "execution.isolation.thread.interruptOnTimeout", value = "false")
        })
        public User getUserByIdTimeoutNonInterrupt(final Long id) throws InterruptedException {
            Thread.sleep(2000);
            System.out.println("代码还是执行了...");
            return createUser(id, "Jacky LIU");
        }

        public User defaultUser(Long id){
            return createUser(id, "default Jacky LIU");
        }

        private User createUser(Long id, String name){
            User user = new User();
            user.setId(id);
            user.setName(name);
            user.setAge(23);
            user.setSex(1);
            return user;
        }
    }
}

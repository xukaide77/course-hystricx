package com.raycloud.test.hystricx;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCommandAspect;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

/**
 * <pre>
 * 这个例子描述了节流的使用场景
 * </pre>
 * Created by liumingjian on 16/8/12.
 */
public class MultiThreadExample {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        final UserService userService = ctx.getBean(UserService.class);
        // 多开几个线程一起处理
        List<Thread> threadList = new ArrayList<Thread>();
        int count = 10;
        final CountDownLatch latch = new CountDownLatch(count);
        for(int i = 0;i < count;i++){
            final long id = i + 1;
            threadList.add(new Thread(new Runnable() {
                public void run() {
                    try {
                        User user = userService.getUserById(id);
                        System.out.println(String.format("[%s]执行成功:%s", Thread.currentThread().getName(), user.toString()));
                    } catch (Exception e) {
                        System.err.println(String.format("线程[%s]缓冲队列满啦，赶紧丢请求...", Thread.currentThread().getName()));
                    }finally {
                        latch.countDown();
                    }
                }
            }));
        }
        for(Thread thread : threadList){
            thread.start();
        }
        latch.await();

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
                @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "100")
        },threadPoolProperties = {
                @HystrixProperty(name = "coreSize", value = "1"),
                @HystrixProperty(name = "maxQueueSize", value = "10"),
                @HystrixProperty(name = "keepAliveTimeMinutes", value = "2"),
                @HystrixProperty(name = "queueSizeRejectionThreshold", value = "1"),
                @HystrixProperty(name = "metrics.rollingStats.numBuckets", value = "12"),
                @HystrixProperty(name = "metrics.rollingStats.timeInMilliseconds", value = "1440")
        })
        public User getUserById(Long id) throws InterruptedException {
            Thread.sleep(10);
            return defaultUser(id);
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

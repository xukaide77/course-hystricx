package com.raycloud.test.hystricx;

import com.netflix.config.ConfigurationManager;
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
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <pre>
 * 这个例子和{@link MultiThreadExample}相似，这里主要展现当异常出现的频率超过阀值了，那么断路器将会熔断保险丝，
 * 然后通过设置，会在一段时间再次接上保险丝，如果流量或者出错的频率还是过高，依然会烧断保险丝。
 * </pre>
 * Created by liumingjian on 16/8/13.
 */
public class CircuitBreakerExample {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        org.apache.log4j.BasicConfigurator.configure();
        ApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        final UserService userService = ctx.getBean(UserService.class);
        Properties properties = ConfigurationManager.getConfigInstance().getProperties("");
        // 多开几个线程一起处理
        List<Thread> threadList = new ArrayList<Thread>();
        int threadCount = 3;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final int total = 100;

        final AtomicLong lastErrorTime = new AtomicLong(0); // 上一次不可用的时间
        for(int i = 0;i < threadCount;i++){
            final long id = i + 1;
            threadList.add(new Thread(new Runnable() {
                public void run() {
                    for (int i = 0; i < total; i++) {
                        try {
                            User user = userService.getUserById((long) i, i % 2 == 0);
                            if (lastErrorTime.get() > 0L) {
                                System.out.println(String.format("保险丝已经接上，花费了%s ms，准备再次打开请求路口", (System.currentTimeMillis() - lastErrorTime.get())));
                                lastErrorTime.set(0L);
                            }
                            System.out.println(String.format("[%s]执行成功:%s", Thread.currentThread().getName(), user.toString()));
                        } catch (Exception e) {
                            if (e.getMessage().contains("could not be queued for execution"))
                                System.err.println(String.format("线程[%s]缓冲队列满啦，赶紧丢请求...错误：%s", Thread.currentThread().getName(), e.getMessage()));
                            else if(e.getMessage().contains("failed and")){
                                System.err.println(String.format("线程[%s]消息执行出错啦，错误：%s", Thread.currentThread().getName(), e.getMessage()));
                            }
                            else {
                                System.err.println(String.format("线程[%s]保险丝烧断了，正准备接新的保险丝，稍等，错误：%s", Thread.currentThread().getName(), e.getMessage()));
                            }
                            if (lastErrorTime.get() == 0)
                                lastErrorTime.set(System.currentTimeMillis());
                        } finally {
                            latch.countDown();
                        }
                        try {
                            Thread.sleep(800);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
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
                @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "1000"),
                @HystrixProperty(name = "circuitBreaker.enabled", value = "true"),
                @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "2"),
                @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "5000")/*,
                @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "2")*/
        },threadPoolProperties = {
                @HystrixProperty(name = "coreSize", value = "1"),
                @HystrixProperty(name = "maxQueueSize", value = "1"),
                @HystrixProperty(name = "keepAliveTimeMinutes", value = "2"),
                @HystrixProperty(name = "queueSizeRejectionThreshold", value = "1"),
                @HystrixProperty(name = "metrics.rollingStats.numBuckets", value = "2"),
                @HystrixProperty(name = "metrics.rollingStats.timeInMilliseconds", value = "1000")
        })
        public User getUserById(Long id, boolean failture) throws InterruptedException {
            if(failture)
                throw new RuntimeException("发生异常了");
            Thread.sleep(800);
            System.out.println("我执行成功啦");
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

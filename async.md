异步任务
```java
@Component
public class Task {

    // @Async所修饰的函数不要定义为static类型，这样异步调用不会生效
    @Async
    public Future<String> doTask1() throws Exception {
      long start = System.currentTimeMillis();
      Thread.sleep(random.nextInt(10000));
      long end = System.currentTimeMillis();
      System.out.println("time: " + (end - start));
      return new AsyncResult<>("complete");
    }

    @Async
    public void doTask2() throws Exception {
    }
}
```
```java
@SpringBootApplication
@EnableAsync
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
```
```java
@Test
public void test() throws Exception {

  long start = System.currentTimeMillis();

  Future<String> task1 = task.doTask1();
  Future<String> task2 = task.doTask2();

  while(true) {
    if(task1.isDone() && task2.isDone()) {
      break;
    }
    Thread.sleep(10);
  }

  long end = System.currentTimeMillis();
  System.out.println("time: " + (end - start));
}
```

控制台日志级别控制
```sh
java -jar myapp.jar --debug
```

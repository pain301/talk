```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>1.5.1.RELEASE</version>
</parent>

<dependencies>
</dependencies>
```

pom.xml 文件中默认两个模块
```xml
<dependencies>
  <!-- 核心模块，包括自动配置支持、日志和YAML -->
  <dependency>
        <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
  </dependency>

  <!-- 测试模块，包括JUnit、Hamcrest、Mockito -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

引入 web
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

热启动
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <optional>true</optional>
   </dependency>
</dependencies>
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <fork>true</fork>
            </configuration>
        </plugin>
   </plugins>
</build>
```

自定义属性
```java
@Component
public class RedisProperties {
  @Value("${redis.minIdle}")
  private String minIdle;

  @Value("${redis.maxIdle}")
  private String maxIdle;

  // get, set
}
```
@ConfigurationProperties(prefix = "redis") 注解将配置文件中以 redis 前缀的属性值自动绑定到对应的字段中
```java
@Component
@ConfigurationProperties(prefix = "redis")
public class RedisProperties {
  private String minIdle;
  private String maxIdle;

  // get, set
}
```

```
redis.minIdle=5
redis.maxIdle=20
redis.maxTotal=${redis.maxIdle}

# 随机字符串
com.pain.salt=${random.value}
# 随机 int
com.pain.time=${random.int}
# 随机 long
com.pain.msec=${random.long}
# 10 以内的随机数
com.pain.retry=${random.int(10)}
# 10-20 的随机数
com.pain.restart=${random.int[10, 20]}

# 环境隔离
# application-dev.properties
# application-prod.properties
spring.profiles.active=dev
```
properties 配置文件中文乱码解决
设置 File Encodings 的 Transparent native-to-ascii conversion 为 true
设置 File Encodings 的 文件编码为 UTF-8

yml 配置文件
Spring Boot 以 iso-8859 的编码方式读取 application.properties 配置文件，会出现中文乱码问题，而 application.yml 不会
```
redis:
  mixIdle: 5
  maxIdle: ${redis.minIdle}
```

键值对 user.name 会读取不到对应的属性值

```sh
# 命令行设置属性值
java -jar flame.jar --server.port=8888

java -jar flame.jar --spring.profiles.active=test
java -jar flame.jar --spring.profiles.active=prod

mvn package
java -jar -Dspring.profiles.active=prod flame.jar
```

```java
// 屏蔽命令行对属性的设置
SpringApplication.setAddCommandLineProperties(false);
```

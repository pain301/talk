redis
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-redis</artifactId>
</dependency>
```

```sh
# Begin redis config

# Redis数据库索引（默认为0）
spring.redis.database=0

# Redis服务器地址
# spring.redis.host=localhost
spring.redis.host=127.0.0.1

# Redis服务器连接端口
spring.redis.port=6379

# Redis服务器连接密码（默认为空）
spring.redis.password=

# 连接池最大连接数（使用负值表示没有限制）
spring.redis.pool.max-active=8

# 连接池最大阻塞等待时间（使用负值表示没有限制）
spring.redis.pool.max-wait=-1

# 连接池中的最大空闲连接
spring.redis.pool.max-idle=8

# 连接池中的最小空闲连接
spring.redis.pool.min-idle=0

# 连接超时时间（毫秒）
spring.redis.timeout=0

# End redis config
```

```java
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(Application.class)
public class ApplicationTests {

  @Autowired
  private StringRedisTemplate stringRedisTemplate;

  @Test
  public void test() throws Exception {
    // StringRedisTemplate就相当于RedisTemplate<String, String>的实现
    stringRedisTemplate.opsForValue().set("aaa", "111");
    Assert.assertEquals("111", stringRedisTemplate.opsForValue().get("aaa"));

  }
}

public class User implements Serializable {

    private static final long serialVersionUID = -1L;

    private String username;
    private Integer age;

    public User(String username, Integer age) {
        this.username = username;
        this.age = age;
    }
}

public class RedisObjectSerializer implements RedisSerializer<Object> {

  private Converter<Object, byte[]> serializer = new SerializingConverter();
  private Converter<byte[], Object> deserializer = new DeserializingConverter();

  static final byte[] EMPTY_ARRAY = new byte[0];

  public Object deserialize(byte[] bytes) {
    if (isEmpty(bytes)) {
      return null;
    }

    try {
      return deserializer.convert(bytes);
    } catch (Exception ex) {
      throw new SerializationException("Cannot deserialize", ex);
    }
  }

  public byte[] serialize(Object object) {
    if (object == null) {
      return EMPTY_ARRAY;
    }

    try {
      return serializer.convert(object);
    } catch (Exception ex) {
      return EMPTY_ARRAY;
    }
  }

  private boolean isEmpty(byte[] data) {
    return (data == null || data.length == 0);
  }
}

@Configuration
public class RedisConfig {

    @Bean
    JedisConnectionFactory jedisConnectionFactory() {
        return new JedisConnectionFactory();
    }

    @Bean
    public RedisTemplate<String, User> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, User> template = new RedisTemplate<String, User>();
        template.setConnectionFactory(jedisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new RedisObjectSerializer());
        return template;
    }
}

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(Application.class)
public class ApplicationTests {

  @Autowired
  private RedisTemplate<String, User> redisTemplate;

  @Test
  public void test() throws Exception {

    User user = new User("pain", 20);
    redisTemplate.opsForValue().set(user.getUsername(), user);
    Assert.assertEquals(20, redisTemplate.opsForValue().get("pain").getAge().longValue());
  }
}
```

```java
ValueOperations<String, User> operations = redisTemplate.opsForValue();
boolean hasKey = redisTemplate.hasKey(key);
if (hasKey) {
  User user = operations.get(key);
}

operations.set(key, user, 10, TimeUnit.SECONDS);

redisTemplate.delete(key);
```

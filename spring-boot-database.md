```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>${mybatis-spring-boot}</version>
</dependency>
```

```
spring.datasource.url=jdbc:mysql://localhost:3306/springbootdb?useUnicode=true&characterEncoding=utf8
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.jdbc.Driver

mybatis.typeAliasesPackage=com.pain.flame.mapper
mybatis.mapperLocations=classpath:mappers/*.xml
```
```java
@SpringBootApplication
@MapperScan("com.pain.flame.mapper")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class,args);
    }
}
```

```java
@Mapper
public interface OrderMapper {
    @Select("SELECT * FROM order WHERE id = #{id}")
    @Results({
          @Result(property = "id", column = "id"),
          @Result(property = "total", column = "total"),
          @Result(property = "quantity", column = "quantity")
    })
    Order selectById(@Param("id") Integer id);
}
```

多数据源
```java
@Configuration
@MapperScan(basePackages = UserDataSourceConfig.PACKAGE, sqlSessionFactoryRef = "userSqlSessionFactory")
public class UserDataSourceConfig {

    static final String PACKAGE = "com.pain.flame.mapper.user";
    static final String MAPPER_LOCATION = "classpath:mappers/user/*.xml";

    @Value("${user.datasource.url}")
    private String url;

    @Value("${user.datasource.username}")
    private String user;

    @Value("${user.datasource.password}")
    private String password;

    @Value("${user.datasource.driverClassName}")
    private String driverClass;

    @Bean(name = "userDataSource")
    // @Primary 表示多个同类 Bean 候选时该 Bean 优先被考虑
    // 多数据源配置时必须要有一个主数据源，用 @Primary 注解
    @Primary
    public DataSource userDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(driverClass);
        dataSource.setUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean(name = "userTransactionManager")
    @Primary
    public DataSourceTransactionManager userTransactionManager() {
        return new DataSourceTransactionManager(userDataSource());
    }

    @Bean(name = "userSqlSessionFactory")
    @Primary
    public SqlSessionFactory userSqlSessionFactory(@Qualifier("userDataSource") DataSource userDataSource) throws Exception {
        final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(userDataSource);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(UserDataSourceConfig.MAPPER_LOCATION));
        return sessionFactory.getObject();
    }
}
```

application.properties 数据源
```
spring.datasource.url=jdbc:mysql://localhost:3306/test
spring.datasource.username=
spring.datasource.password=
spring.datasource.driver-class-name=com.mysql.jdbc.Driver
```
JdbcTemplate 是自动配置的
```java
public interface UserService {

    void create(String name, Integer age);

    void deleteByName(String name);

    Integer getAllUsers();

    void deleteAllUsers();
}

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void create(String name, Integer age) {
        jdbcTemplate.update("insert into USER(NAME, AGE) values(?, ?)", name, age);
    }

    @Override
    public void deleteByName(String name) {
        jdbcTemplate.update("delete from USER where NAME = ?", name);
    }

    @Override
    public Integer getAllUsers() {
        return jdbcTemplate.queryForObject("select count(1) from USER", Integer.class);
    }

    @Override
    public void deleteAllUsers() {
        jdbcTemplate.update("delete from USER");
    }
}

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(Application.class)
public class ApplicationTests {

  @Autowired
  private UserService userSerivce;

  @Before
  public void setUp() {
    userSerivce.deleteAllUsers();
  }

  @Test
  public void test() throws Exception {
    userSerivce.create("a", 1);
    userSerivce.create("b", 2);

    Assert.assertEquals(2, userSerivce.getAllUsers().intValue());
    userSerivce.deleteByName("a");
    Assert.assertEquals(1, userSerivce.getAllUsers().intValue());
  }
}
```


mybatis
```java
// param
@Insert("INSERT INTO USER(NAME, AGE) VALUES(#{name}, #{age})")
int insert(@Param("name") String name, @Param("age") Integer age);
```

```java
Map<String, Object> map = new HashMap<>();
map.put("name", "pain");
map.put("age", 20);
userMapper.insertByMap(map);
```
```java
// map
@Insert("INSERT INTO USER(NAME, AGE) VALUES(#{name,jdbcType=VARCHAR}, #{age,jdbcType=INTEGER})")
int insertByMap(Map<String, Object> map);
```

```java
// Object
@Insert("INSERT INTO USER(NAME, AGE) VALUES(#{name}, #{age})")
int insertByUser(User user);
```
```java
public interface UserMapper {

    @Select("SELECT * FROM user WHERE name = #{name}")
    User findByName(@Param("name") String name);

    @Insert("INSERT INTO user(name, age) VALUES(#{name}, #{age})")
    int insert(@Param("name") String name, @Param("age") Integer age);

    @Update("UPDATE user SET age=#{age} WHERE name=#{name}")
    void update(User user);

    @Delete("DELETE FROM user WHERE id=#{id}")
    void delete(Long id);
}
```

```java
@Results({
    @Result(property = "name", column = "name"),
    @Result(property = "age", column = "age")
})
@Select("SELECT name, age FROM user")
List<User> findAll();
```

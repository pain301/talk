获取列表：GET     /users
创建：POST        /users
获取一个：GET     /users/{id}
更新一个：PUT     /users/{id}
删除一个：DELETE  /users/{id}

```java
@RestController 
@RequestMapping(value="/users")
public class UserController {

    static Map<Long, User> users = Collections.synchronizedMap(new HashMap<Long, User>()); 
 
    @RequestMapping(value="/", method=RequestMethod.GET) 
    public List<User> getUserList() { 
        List<User> result = new ArrayList<User>(users.values()); 
        return result;
    } 
 
    @RequestMapping(value="/", method=RequestMethod.POST)
    public String postUser(@ModelAttribute User user) {
        users.put(user.getId(), user);
        return "success";
    }
 
    @RequestMapping(value="/{id}", method=RequestMethod.GET) 
    public User getUser(@PathVariable Long id) {
        return users.get(id);
    }
 
    @RequestMapping(value="/{id}", method=RequestMethod.PUT) 
    public String putUser(@PathVariable Long id, @ModelAttribute User user) { 
        User u = users.get(id); 
        u.setName(user.getName()); 
        u.setAge(user.getAge()); 
        users.put(id, u); 
        return "success"; 
    }
 
    @RequestMapping(value="/{id}", method=RequestMethod.DELETE)
    public String deleteUser(@PathVariable Long id) {
        users.remove(id);
        return "success";
    }
}
```

```java
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 单元测试
@RunWith(SpringJUnit4ClassRunner.class)
// @SpringBootTest new version
@SpringApplicationConfiguration(classes = MockServletContext.class)
@WebAppConfiguration
public class ApplicationTests {

  private MockMvc mvc;

  @Before
  public void setUp() throws Exception {
    mvc = MockMvcBuilders.standaloneSetup(new UserController()).build();
  }

  @Test
  public void testUser() throws Exception {
    RequestBuilder request = null;
    request = get("/users/");
    mvc.perform(request)
       .andExpect(status().isOk())
       .andExpect(content().string(equalTo("[]")));

    request = post("/users/")
        .param("id", "1") 
        .param("name", "pain") 
        .param("age", "20");
    
    mvc.perform(request) 
       .andExpect(content().string(equalTo("success")));

    request = get("/users/");
    mvc.perform(request) 
       .andExpect(status().isOk()) 
       .andExpect(content().string(equalTo("[{\"id\":1,\"name\":\"pain\",\"age\":20}]")));

    request = put("/users/1") 
        .param("name", "page")
        .param("age", "30");
    mvc.perform(request) 
       .andExpect(content().string(equalTo("success")));

    request = get("/users/1");
    mvc.perform(request) 
       .andExpect(content().string(equalTo("{\"id\":1,\"name\":\"page\",\"age\":30}")));

    request = delete("/users/1");
    mvc.perform(request)
       .andExpect(content().string(equalTo("success")));

    request = get("/users/");
    mvc.perform(request) 
       .andExpect(status().isOk()) 
       .andExpect(content().string(equalTo("[]")));

    mvc.perform(MockMvcRequestBuilders.get("/users").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo("Hello World")));
  }
}
```

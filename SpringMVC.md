### DispatcherServlet
```xml
<!-- web.xml -->
<!-- 注册 ServletContext 监听器：创建 spring 容器对象并为 ServletContext 设置-->
<listener>
  <listener-class>
    org.springframework.web.context.ContextLoaderListener
  </listener-class>
</listener>

<!-- 字符集过滤器 -->
<filter>
  <filter-name>CharacterEncodingFilter</filter-name>
  <filter-class>
    org.springframework.web.filter.CharaterEncodingFilter
  </filter-class>
  <init-param>
    <param-name>encoding</param-name>
    <param-value>utf-8</param-value>
  </init-param>
  <init-param>
    <param-name>forceEncoding</param-name>
    <!-- 忽略代码中设置的编码 -->
    <param-value>true</param-value>
  </init-param>
</filter>
<filter-mapping>
  <filter-name>CharacterEncodingFilter</filter-name>
  <filter-pattern>/*</filter-pattern>
</filter-mapping>

<!-- 注册中央处理器 -->
<servlet>
  <servlet-name>springmvc</servlet-name>
  <servlet-class>org.springframework.web.servlet.DispatcherServlet
  </servlet-class>
  <init-param>
    <param-name>contextConfigLocation</param-name>
    <param-value>classpath:springmvc.xml</param-value>
  </init-param>
  <!-- Tomcat 启动时创建此 Servlet -->
  <load-on-startup>1</load-on-startup>
</servlet>
<servlet-mapping>
  <servlet-name>springmvc</servlet-name>
  <url-pattern>*.html</url-pattern>
</servlet-mapping>
```

### 路径
```xml
<!-- http://127.0.0.1:8080/app -->
<!-- 后台路径，参照路径为 WEB 应用根路径 -->
<bean id="/home.html" class="com.pain.controller.MyController" />
```
```html
<!-- http://127.0.0.1:8080 -->
<!-- 前台路径，参照路径为 WEB 服务器的根 -->
<a href="/home.html"></a>

<!-- 参照路径为当前访问路径 -->
<a href="home.html"></a>
```

### 控制器
#### 配置式
```java
public class MyController implements Controller{
  public ModelAndView handleRequest(HttpServletRequest request,
    HttpServletResponse response){
    ModelAndView view = new ModelAndView();

    // 添加到 request 域中
    view.addObject("msg", "login ok");

    // 逻辑视图
    view.setViewName("index");
    return view;
  }
}
```
```html
${msg}
```
```xml
<bean class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping">
  <property name="mappings">
    <props>
      <prop key="/hello">indexController</prop>
    </props>
  </property>
</bean>
<bean id="indexController" class="com.pain.controller.IndexController">
```
```xml
<bean class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping">
  <property name="urlMap">
    <map>
      <entry key="/hello" value="indexController"></entry>
    </map>
  </property>
</bean>
<bean id="indexController" class="com.pain.controller.IndexController">
  <property name="supportedMethods" value="POST" />
</bean>
```
#### 注解式
```java
// value 指定命名空间，params 指定携带的参数
@Controller
@RequestMapping(value="/pain", params={"name"}, method=RequestMethod.POST)
public class MyController{

  // /*/index => 路径级数绝对匹配
  // /**/index => 可以包含多级或没有
  @RequestMapping({"/index*","/*home"})
  public ModelAndView handleRequest(HttpServletRequest request,
    HttpServletResponse response){
    ModelView mv = new ModelView();
    mv.setViewName("/WEB-INF/index.jsp")
    return mv;
  }
}
```
```xml
<!-- springmvc.xml -->
<!-- 组件扫描器 -->
<context:component-scan base-package="com.pain.controller" />
```
#### 处理器方法常用参数
HttpSession
HttpServletRequest
HttpServletResponse
##### Model
```java
@RequestMapping({"/index"})
public string index(String name, Model model){
  ModelAndView mv = new ModelAndView();

  // 携带参数
  model.addAttribute("name", name);

  // 重定向到 controller
  mv.setViewName("redirect:home"); // 后台路径
  return mv;
}

@RequestMapping({"/home"})
public string home(String name){
  ModelAndView mv = new ModelAndView();
  mv.setViewName("/home.jsp");
  return mv;
}
```

##### 请求参数
```java
// 参数映射
public ModelAndView index(@RequestParam("pname") String name) {
  ModelAndView mv = new ModelAndView();
  mv.addObject("name", name);
  return mv;
}

// 表单参数名与对象属性名一致
public ModelAndView index(Student stu){
  ModelAndView mv = new ModelAndView();
  mv.addObject("stu", stu);
  return mv;
}

// 路径变量
@RequestMapping({"/index/{page}"})
public ModelAndView index(@PathVariable("page") int page){
  ModelAndView mv = new ModelAndView();
  mv.addObject("page", page);
  return mv;
}
```
```java
@RequestMapping(value = {"/index"}, produces="text/html;charset=utf-8")
@ResponseBody  // 返回数据放入响应体
public Object index(){
  // 可以返回自定义对象、map、list 等
  return "hello";
}
```

### 重定向
```java
@RequestMapping({"/index"})
public string index(String name){
  ModelAndView mv = new ModelAndView();

  // 携带数据，以请求参数形式放到请求 URL 后面
  mv.addObject("name", name);
  // 请求转发
  // mv.setViewName("forward:/WEB-INF/home.jsp");

  // 重定向将数据携带在请求参数中
  mv.setViewName("redirect:/home.jsp"); // 此处为后台路径
  return mv;
}
```
```html
<!-- 相当于 request.getParameter("name"); -->
name = ${param.name}
```

### 异常处理
```java
@RequestMapping({"/home"})
public string home(String name){
  int i = 3 / 0;
  ModelAndView mv = new ModelAndView();
  mv.setViewName("/home.jsp");
  return mv;
}
```
```xml
<!-- 异常处理 -->
<bean class="SimpleMappingExceptionResolver">
  <property name="defaultErrorView" value="/error.jsp"></property>
</bean>
```

### 拦截器
```java
public class MyInterceptor implements HandlerInterceptor{

  // 处理器方法执行之前执行
  public boolean preHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler){
    // 继续执行处理器方法
    return true;
  }
  // 处理器方法之后执行
  // 方法中包含 modelAndView 参数可以修改最终结果
  public void postHandle(HttpServletRequest request,
                         HttpServletResponse response,
                         Object handler,
                         ModelAndView modelAndView){

  }

  // 中央调度器渲染响应页面之后执行
  public void afterCompletion(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler,
                              Exception ex){

  }
}
```
```xml
<!-- 注册拦截器 -->
<mvc:interceptors>
  <mvc:interceptor>
    <mvc:mapping path="/**" />
    <bean class="com.pain.interceptors.MyInterceptor" />
  </mvc:interceptor>
</mvc:interceptors>
```

### 文件上传
#### 单文件
```java
public ModelAndView upload(MultipartFile img, HttpSession session){
  ModelAndView mv = new ModelAndView();
  if (img.getSize() <= 0){
    return mv;
  }
  String path = session.getServletContext().getRealPath("/images");
  File file = new File("D:/path", img.getOriginalFilename());
  img.transferTo(file);
  return mv;
}
```
```xml
<bean id="multipartResolver" class="org.springframework.web.multipart.commons.CommonsMultipartResolver">
  <property name="defaultEncoding" value="utf-8"></property>
  <property name="maxUploadSize" value="10240"></property>
</bean>
<mvc:annotation-driven />
```
#### 多个文件
```java
public ModelAndView upload(@RequestParam MultipartFile[] imgs, HttpSession session){
  ModelAndView mv = new ModelAndView();
  String path = session.getServletContext().getRealPath("/images");
  for (MultipartFile img : imgs) {
    if (img.getSize > 0){}
  }
  return mv;
}
```

### 数据绑定
```java
@Controller
public class TestController {

  // age 不能为空
  @RequestMapping(value = "test1")
  @ResponseBody
  public String baseType(int age) {
    return "age: " + age;
  }

  // age 为包装类型可以为空
  @RequestMapping(value = "test2")
  @ResponseBody
  public String packageType(@RequestParam("xage") Integer age) {
    return "age: " + age;
  }

  // http://localhost/test3?name=jack&name=pain
  @RequestMapping(value = "test3")
  @ResponseBody
  public String arrayType(String[] name) {
    return "array: " + name;
  }

  // http://localhost:8080/test4?name=pain&age=10&address.city=bj
  @RequestMapping(value = "test4")
  @ResponseBody
  public String objectType(User user) {
    return "User: " + user;
  }

  // 同属性对象
  // http://localhost:8080/test5?user.name=pain&admin.name=jack&age=10
  // age 没有指定前缀，user 与 manager 共享
  @RequestMapping(value = "test5")
  @ResponseBody
  public String multiObjectType(User user, Manager manager) {
    return "User: " + user;
  }

  @InitBinder("user")
  public void initUser(WebDataBinder binder) {
    binder.setFieldDefaultPrefix("user.");
  }

  @InitBinder("manager")
  public void initUser(WebDataBinder binder) {
    binder.setFieldDefaultPrefix("manager.");
  }

  // http://localhost:8080/test6?users[0].name=pain&users[1].name=jack
  @RequestMapping(value = "test6")
  @ResponseBody
  public String listType(UserListForm userListForm) {
    return "UserList: " + userListForm;
  }

  // http://localhost:8080/test6?users['a'].name=pain&users['b'].name=jack
  @RequestMapping(value = "test7")
  @ResponseBody
  public String mapType(UserMapForm userMapForm) {
    return "UserMap: " + userMapForm;
  }

  @RequestMapping(value = "test8")
  @ResponseBody
  public String jsonType(@RequestBody User user) {
    return "User: " + user;
  }

  @RequestMapping(value = "test9")
  @ResponseBody
  public String dateType1(Date date) {
    return "Date: " + date;
  }

  @InitBinder("date")
  public void initDate(WebDataBinder binder) {
    binder.registerCustomerEditor(Date.class,
      new CustomerDateEditor(new SimpleDateFormat("yyyy-MM-dd", true)));
  }

  // 使用全局 format 转换
  @RequestMapping(value = "test10")
  @ResponseBody
  public String dateType2(Date date1) {
    return "Date: " + date1;
  }
}

public class UserListForm {
  private List<User> users;

  public List<User> getUsers() {
    return users;
  }
}

public class UserMapForm {
  private Map<String, User> users;

  public Map<String, User> getUsers() {
    return users;
  }
}
```

```java
public class MyDateFormatter implements Formatter<Date> {
  public Date parse(Strint text, Locale locale) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    return sdf.parse(text);
  }
}
```
```xml
<mvc:annotation-driven conversion-service="myDateFormatter" />

<bean id="myDateFormatter" class="org.springframework.format.support.Formatting...">
  <property name="formatters">
    <set>
      <bean class="com.pain.MyDateFormatter"></bean>
    </set>
  </property>
</bean>
```

```java
public class MyDateConverter implements Converter<String, Date> {
  public Date convert(String source) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    return sdf.parse(source);
  }
}
```
```xml
<mvc:annotation-driven conversion-service="myDateConverter" />

<bean id="myDateConverter" class="org.springframework.format.support.FormattingConvert...">
  <property name="converters">
    <set>
      <bean class="com.pain.MyDateConverter"></bean>
    </set>
  </property>
</bean>
```
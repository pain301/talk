180919957205

https://mp.weixin.qq.com/s/tBQ5tjSqk94_AtrgYgO0xA
csrf: https://mp.weixin.qq.com/s/sYoccR4-qM4crgkQBYvSpA
xss: https://mp.weixin.qq.com/s/kWxnYcCTLAQp5CGFrw30mQ

http://www.iocoder.cn/Tomcat/Tomcat-collection/

jvm
https://juejin.im/post/5a0d5b176fb9a04504076def

AOP
https://juejin.im/post/5a64b5056fb9a01ca9158cb1
https://juejin.im/post/5aa8edf06fb9a028d0432584?utm_medium=be&utm_source=weixinqun

B 弹幕
https://juejin.im/post/5a635fee518825734f52cc78

http
https://juejin.im/entry/58a7aaba570c3500699e5cbd?utm_medium=fe&utm_source=weixinqun&from=timeline

tomcat
https://juejin.im/post/5a6d77916fb9a01c9c1f4440?utm_medium=be&utm_source=weixinqun
https://juejin.im/post/5a75ab4b6fb9a063592ba9db?utm_medium=be&utm_source=weixinqun
https://juejin.im/post/5a75b0be5188254e761781d7?utm_medium=be&utm_source=weixinqun

oom
https://juejin.im/post/5a72762c6fb9a01cbe65a4eb?utm_medium=be&utm_source=weixinqun

高并发
https://juejin.im/entry/5a7a77906fb9a06335319409?utm_medium=be&utm_source=weixinqun
https://juejin.im/entry/5a9e0301f265da23a334bae4?utm_medium=be&utm_source=weixinqun

tcp
https://juejin.im/post/5a7835a46fb9a063606eb801?utm_medium=be&utm_source=weixinqun

监控
https://juejin.im/post/5a7a9e0af265da4e914b46f1?utm_medium=be&utm_source=weixinqun


爬虫
https://juejin.im/post/5a90eaf4f265da4e9a4973f5?utm_medium=be&utm_source=weixinqun

mycat
https://juejin.im/entry/5a9dbe25f265da237a4c8386?utm_medium=be&utm_source=weixinqun


缓存穿透：查询一个一定不存在的数据
方法一：隆过滤器，将所有可能存在的数据哈希到一个足够大的bitmap中，一个一定不存在的数据会被 这个bitmap拦截掉
方法二：如果一个查询返回的数据为空（不管是数据不存在，还是系统故障），我们仍然把这个空结果进行缓存，但它的过期时间会很短，最长不超过五分钟

缓存雪崩：设置缓存时采用了相同的过期时间或者其他情况，导致缓存在某一时刻同时失效，请求全部转发到DB，DB瞬时压力过重雪崩
方法：在原有的失效时间基础上增加一个随机值，比如1-5分钟随机，这样每一个缓存的过期时间的重复率就会降低，就很难引发集体失效的事件

缓存击穿：缓存在某个时间点过期的时候，恰好在这个时间点对这个Key有大量的并发请求过来，这些请求发现缓存过期一般都会从后端DB加载数据并回设到缓存，这个时候大并发的请求可能会瞬间把后端DB压垮
方法：在缓存失效的时候（判断拿出来的值为空），不是立即去load db，而是先使用缓存工具的某些带成功操作返回值的操作（比如Redis的SETNX或者Memcache的ADD）去set一个mutex key，当操作返回成功时，再进行load db的操作并回设缓存；否则，就重试整个get缓存的方法

```
public interface UserService {
    void addUser(Integer userId) throws SQLException;
}

public class UserServiceImpl implements UserService {
    @Override
    public void addUser(Integer userId) throws SQLException {
        System.out.println(String.format("UserService add user: %d", userId));
        throw new SQLException("Test checked Exception");
    }
}
```
具体方法实现中抛出SQLException被反射包装为会被包装成InvocationTargetException，这是个受检异常，而代理类在处理异常时发现该异常在接口中没有声明，所以包装为UndeclaredThrowableException
```
public class UserServiceProxy implements InvocationHandler {

    private UserService userService;

    public UserServiceProxy(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(userService, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
```

```
public final class $Proxy0 extends Proxy implements UserService {
    private static Method m3;

    public $Proxy0(InvocationHandler var1) throws  {
        super(var1);
    }

    public final void addUser(Integer var1) throws SQLException {
        try {
            super.h.invoke(this, m3, new Object[]{var1});
        } catch (RuntimeException | SQLException | Error var3) {
            throw var3;
        } catch (Throwable var4) {
            throw new UndeclaredThrowableException(var4);
        }
    }

    static {
        try {
            m3 = Class.forName("com.pain.quora.service.UserService").getMethod("addUser", new Class[]{Class.forName("java.lang.Integer")});
        } catch (NoSuchMethodException var2) {
            throw new NoSuchMethodError(var2.getMessage());
        } catch (ClassNotFoundException var3) {
            throw new NoClassDefFoundError(var3.getMessage());
        }
    }
}
```

```
System.getProperties().setProperty("sun.misc.ProxyGenerator.saveGeneratedFiles", "true");
UserService userService = new UserServiceImpl();
UserService proxyUserService = (UserService) Proxy.newProxyInstance(userService.getClass().getClassLoader(),
        userService.getClass().getInterfaces(),
        new UserServiceProxy(userService));

try {
    proxyUserService.addUser(1);
} catch (SQLException e) {
    e.printStackTrace();
}
```


LongAdder vs AtomicLong
getAndAddLong方法会以volatile的语义去读需要自增的域的最新值，然后通过CAS去尝试更新，正常情况下会直接成功后返回，但是在高并发下可能会同时有很多线程同时尝试这个过程，也就是说线程A读到的最新值可能实际已经过期了，因此需要在while循环中不断的重试，造成很多不必要的开销



ContextLoaderListener：contextConfigLocation [ROOT Context]
ContextLoaderListener 通过调用继承自 ContextLoader 的 initWebApplicationContext 方法实例化 Spring Ioc 容器

Spring根上下文的加载与初始化
ContextLoader.initWebApplicationContext

Spring MVC对应的上下文加载与初始化
FrameworkServlet.initWebApplicationContext

DispatcherServlet

Spring的启动过程：


首先，对于一个web应用，其部署在web容器中，web容器提供其一个全局的上下文环境，这个上下文就是ServletContext，其为后面的spring IoC容器提供宿主环境；


其次，在web.xml中会提供有contextLoaderListener。在web容器启动时，会触发容器初始化事件，此时contextLoaderListener会监听到这个事件，其contextInitialized方法会被调用，在这个方法中，spring会初始化一个启动上下文，这个上下文被称为根上下文，即WebApplicationContext，这是一个接口类，确切的说，其实际的实现类是XmlWebApplicationContext。 这个就是spring的IoC容器，其对应的Bean定义的配置由web.xml中的context-param标签指定。在这个IoC容器初始化完毕后，spring以 WebApplicationContext.ROOTWEBAPPLICATIONCONTEXTATTRIBUTE 为属性Key，将其存储到ServletContext中，便于获取；


再次，contextLoaderListener监听器初始化完毕后，开始初始化web.xml中配置的Servlet，这个servlet可以配置多个，以最常见的DispatcherServlet为例，这个servlet实际上是一个标准的前端控制器，用以转发、匹配、处理每个servlet请求。DispatcherServlet上下文在初始化的时候会建立自己的IoC上下文，用以持有spring mvc相关的bean。在建立DispatcherServlet自己的IoC上下文时，会利用 WebApplicationContext.ROOTWEBAPPLICATIONCONTEXTATTRIBUTE 先从ServletContext中获取之前的根上下文(即WebApplicationContext)作为自己上下文的parent上下文。有了这个parent上下文之后，再初始化自己持有的上下文。这个DispatcherServlet初始化自己上下文的工作在其initStrategies方法中可以看到，大概的工作就是初始化处理器映射、视图解析等。这个servlet自己持有的上下文默认实现类也是XmlWebApplicationContext。初始化完毕后，spring以与servlet的名字相关(此处不是简单的以servlet名为Key，而是通过一些转换，具体可自行查看源码)的属性为属性Key，也将其存到ServletContext中，以便后续使用。这样每个servlet就持有自己的上下文，即拥有自己独立的bean空间，同时各个servlet共享相同的bean，即根上下文(第2步中初始化的上下文)定义的那些bean

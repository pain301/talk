Spring MVC启动过程大致分为两个过程
1. ContextLoaderListener初始化，实例化IoC容器，并将此容器实例注册到ServletContext中
```xml
<listener>
  <listener-class>
    org.springframework.web.context.ContextLoaderListener
  </listener-class>
</listener>
```
```java
public class ContextLoaderListener extends ContextLoader implements ServletContextListener {
  public ContextLoaderListener() {}
  public ContextLoaderListener(WebApplicationContext context) {
    super(context);
  }

  public void contextInitialized(ServletContextEvent event) {
    initWebApplicationContext(event.getServletContext());
  }
  public void contextDestroyed(ServletContextEvent event) {
    closeWebApplicationContext(event.getServletContext());
    ContextCleanupListener.cleanupAttributes(event.getServletContext());
  }
}
```
```java
public class ContextLoader {

  static {
      // Load default strategy implementations from properties file.
      // This is currently strictly internal and not meant to be customized
      // by application developers.
      try {
          ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, ContextLoader.class);
          defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
      }
      catch (IOException ex) {
          throw new IllegalStateException("Could not load 'ContextLoader.properties': " + ex.getMessage());
      }
  }

  public ContextLoader(WebApplicationContext context) {
    this.context = context;
  }

  // 实例化Spring Ioc容器
  public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
    // 默认情况下，配置文件的位置和名称是： DEFAULT_CONFIG_LOCATION = "/WEB-INF/applicationContext.xml" 
    // 在整个web应用中，只能有一个根上下文
    if (servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null) {
      throw new IllegalStateException(
          "Cannot initialize context because there is already a root application context present - " +
          "check whether you have multiple ContextLoader* definitions in your web.xml!");
    }

    Log logger = LogFactory.getLog(ContextLoader.class);
    servletContext.log("Initializing Spring root WebApplicationContext");
    if (logger.isInfoEnabled()) {
      logger.info("Root WebApplicationContext: initialization started");
    }
    long startTime = System.currentTimeMillis();

    try {
      // Store context in local instance variable, to guarantee that
      // it is available on ServletContext shutdown.
      if (this.context == null) {
        this.context = createWebApplicationContext(servletContext);
      }
      if (this.context instanceof ConfigurableWebApplicationContext) {
        ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) this.context;
        if (!cwac.isActive()) {
          // The context has not yet been refreshed -> provide services such as
          // setting the parent context, setting the application context id, etc
          if (cwac.getParent() == null) {
            // The context instance was injected without an explicit parent ->
            // determine parent for root web application context, if any.
            ApplicationContext parent = loadParentContext(servletContext);
            cwac.setParent(parent);
          }
          // spring IOC container 初始化
          configureAndRefreshWebApplicationContext(cwac, servletContext);
        }
      }

      // 将根上下文放置在servletContext中
      servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);

      ClassLoader ccl = Thread.currentThread().getContextClassLoader();
      if (ccl == ContextLoader.class.getClassLoader()) {
        currentContext = this.context;
      }
      else if (ccl != null) {
        currentContextPerThread.put(ccl, this.context);
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Published root WebApplicationContext as ServletContext attribute with name [" +
            WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE + "]");
      }
      if (logger.isInfoEnabled()) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        logger.info("Root WebApplicationContext: initialization completed in " + elapsedTime + " ms");
      }

      return this.context;
    }
    catch (RuntimeException ex) {
      logger.error("Context initialization failed", ex);
      servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
      throw ex;
    }
    catch (Error err) {
      logger.error("Context initialization failed", err);
      servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, err);
      throw err;
    }
  }

  protected WebApplicationContext createWebApplicationContext(ServletContext sc) {
    // //根据web.xml中的配置决定使用何种WebApplicationContext。默认情况下使用XmlWebApplicationContext
    // web.xml中相关的配置context-param的名称“contextClass”
    Class<?> contextClass = determineContextClass(sc);
    if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
      throw new ApplicationContextException("Custom context class [" + contextClass.getName() +
          "] is not of type [" + ConfigurableWebApplicationContext.class.getName() + "]");
    }
    //实例化WebApplicationContext的实现类
    return (ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);
  }

  protected Class<?> determineContextClass(ServletContext servletContext) {
    String contextClassName = servletContext.getInitParameter(CONTEXT_CLASS_PARAM);
    if (contextClassName != null) {
        try {
            return ClassUtils.forName(contextClassName, ClassUtils.getDefaultClassLoader());
        }
        catch (ClassNotFoundException ex) {
            throw new ApplicationContextException(
                    "Failed to load custom context class [" + contextClassName + "]", ex);
        }
    } else {
        contextClassName = defaultStrategies.getProperty(WebApplicationContext.class.getName());
        try {
            return ClassUtils.forName(contextClassName, ContextLoader.class.getClassLoader());
        }
        catch (ClassNotFoundException ex) {
            throw new ApplicationContextException(
                    "Failed to load default context class [" + contextClassName + "]", ex);
        }
    }
  }

  protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac, ServletContext sc) {
    if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
      // The application context id is still set to its original default value
      // -> assign a more useful id based on available information
      String idParam = sc.getInitParameter(CONTEXT_ID_PARAM);
      if (idParam != null) {
        wac.setId(idParam);
      }
      else {
        // Generate default id...
        wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
            ObjectUtils.getDisplayString(sc.getContextPath()));
      }
    }

    wac.setServletContext(sc);
    String configLocationParam = sc.getInitParameter(CONFIG_LOCATION_PARAM);
    if (configLocationParam != null) {
      wac.setConfigLocation(configLocationParam);
    }

    // The wac environment's #initPropertySources will be called in any case when the context
    // is refreshed; do it eagerly here to ensure servlet property sources are in place for
    // use in any post-processing or initialization that occurs below prior to #refresh
    ConfigurableEnvironment env = wac.getEnvironment();
    if (env instanceof ConfigurableWebEnvironment) {
      ((ConfigurableWebEnvironment) env).initPropertySources(sc, null);
    }

    customizeContext(sc, wac);
    wac.refresh();
  }
}
```

```java
public interface WebApplicationContext extends ApplicationContext {
  String ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE = WebApplicationContext.class.getName() + ".ROOT";
  ServletContext getServletContext();
}
```

2. DispatcherServlet初始化；
```java
public abstract class FrameworkServlet extends HttpServletBean implements ApplicationContextAware {
  protected WebApplicationContext initWebApplicationContext() {
    //从ServletContext中取得根上下文
    WebApplicationContext rootContext =
        WebApplicationContextUtils.getWebApplicationContext(getServletContext());
    WebApplicationContext wac = null;

    if (this.webApplicationContext != null) {
      // A context instance was injected at construction time -> use it
      wac = this.webApplicationContext;
      if (wac instanceof ConfigurableWebApplicationContext) {
        ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) wac;
        if (!cwac.isActive()) {
          // The context has not yet been refreshed -> provide services such as
          // setting the parent context, setting the application context id, etc
          if (cwac.getParent() == null) {
            // The context instance was injected without an explicit parent -> set
            // the root application context (if any; may be null) as the parent
            cwac.setParent(rootContext);
          }
          configureAndRefreshWebApplicationContext(cwac);
        }
      }
    }
    if (wac == null) {
      // No context instance was injected at construction time -> see if one
      // has been registered in the servlet context. If one exists, it is assumed
      // that the parent context (if any) has already been set and that the
      // user has performed any initialization such as setting the context id

      //先从web容器的ServletContext中查找WebApplicationContext
      wac = findWebApplicationContext();
    }
    if (wac == null) {
      // No context instance is defined for this servlet -> create a local one
      // 创建Spring MVC的上下文，并将根上下文作为起双亲上下文
      wac = createWebApplicationContext(rootContext);
    }

    if (!this.refreshEventReceived) {
      // Either the context is not a ConfigurableApplicationContext with refresh
      // support or the context injected at construction time had already been
      // refreshed -> trigger initial onRefresh manually here.
      onRefresh(wac);
    }

    if (this.publishContext) {
      // Publish the context as a servlet context attribute.
      // 取得context在ServletContext中的名称
      String attrName = getServletContextAttributeName();
      //将Spring MVC的Context放置到ServletContext中
      getServletContext().setAttribute(attrName, wac);
      if (this.logger.isDebugEnabled()) {
        this.logger.debug("Published WebApplicationContext of servlet '" + getServletName() +
            "' as ServletContext attribute with name [" + attrName + "]");
      }
    }

    return wac;
  }

  protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac) {
    if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
      // The application context id is still set to its original default value
      // -> assign a more useful id based on available information
      if (this.contextId != null) {
        wac.setId(this.contextId);
      }
      else {
        // Generate default id...
        wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
            ObjectUtils.getDisplayString(getServletContext().getContextPath()) + "/" + getServletName());
      }
    }

    wac.setServletContext(getServletContext());
    wac.setServletConfig(getServletConfig());
    wac.setNamespace(getNamespace());
    wac.addApplicationListener(new SourceFilteringListener(wac, new ContextRefreshListener()));

    // The wac environment's #initPropertySources will be called in any case when the context
    // is refreshed; do it eagerly here to ensure servlet property sources are in place for
    // use in any post-processing or initialization that occurs below prior to #refresh
    ConfigurableEnvironment env = wac.getEnvironment();
    if (env instanceof ConfigurableWebEnvironment) {
      ((ConfigurableWebEnvironment) env).initPropertySources(getServletContext(), getServletConfig());
    }

    postProcessWebApplicationContext(wac);
    applyInitializers(wac);
    wac.refresh();
  }

  protected WebApplicationContext findWebApplicationContext() {
    String attrName = getContextAttribute();
    if (attrName == null) {
      return null;
    }
    WebApplicationContext wac =
        WebApplicationContextUtils.getWebApplicationContext(getServletContext(), attrName);
    if (wac == null) {
      throw new IllegalStateException("No WebApplicationContext found: initializer not registered?");
    }
    return wac;
  }

  protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
    return createWebApplicationContext((ApplicationContext) parent);
  }

  protected WebApplicationContext createWebApplicationContext(ApplicationContext parent) {
    Class<?> contextClass = getContextClass();
    if (this.logger.isDebugEnabled()) {
      this.logger.debug("Servlet with name '" + getServletName() +
          "' will try to create custom WebApplicationContext context of class '" +
          contextClass.getName() + "'" + ", using parent context [" + parent + "]");
    }
    if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
      throw new ApplicationContextException(
          "Fatal initialization error in servlet with name '" + getServletName() +
          "': custom WebApplicationContext class [" + contextClass.getName() +
          "] is not of type ConfigurableWebApplicationContext");
    }
    ConfigurableWebApplicationContext wac =
        (ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);

    wac.setEnvironment(getEnvironment());
    wac.setParent(parent);
    wac.setConfigLocation(getContextConfigLocation());

    configureAndRefreshWebApplicationContext(wac);

    return wac;
  }

  protected void onRefresh(ApplicationContext context) {
    // For subclasses: do nothing by default.
  }
}
```

对于一个web应用，其部署在web容器中，web容器提供其一个全局的上下文环境，这个上下文就是ServletContext，其为后面的spring IoC容器提供宿主环境；
其次，在web.xml中会提供有contextLoaderListener。在web容器启动时，会触发容器初始化事件，此时contextLoaderListener会监听到这个事件，其contextInitialized方法会被调用，在这个方法中，spring会初始化一个启动上下文，这个上下文被称为根上下文，即WebApplicationContext，这是一个接口类，确切的说，其实际的实现类是XmlWebApplicationContext。这个就是spring的IoC容器，其对应的Bean定义的配置由web.xml中的context-param标签指定。在这个IoC容器初始化完毕后，spring以 WebApplicationContext.ROOTWEBAPPLICATIONCONTEXTATTRIBUTE 为属性Key，将其存储到ServletContext中，便于获取；
contextLoaderListener监听器初始化完毕后，开始初始化web.xml中配置的Servlet，这个servlet可以配置多个，以最常见的DispatcherServlet为例，这个servlet实际上是一个标准的前端控制器，用以转发、匹配、处理每个servlet请求。DispatcherServlet上下文在初始化的时候会建立自己的IoC上下文，用以持有spring mvc相关的bean。在建立DispatcherServlet自己的IoC上下文时，会利用 WebApplicationContext.ROOTWEBAPPLICATIONCONTEXTATTRIBUTE 先从ServletContext中获取之前的根上下文(即WebApplicationContext)作为自己上下文的parent上下文。有了这个parent上下文之后，再初始化自己持有的上下文。这个DispatcherServlet初始化自己上下文的工作在其initStrategies方法中可以看到，大概的工作就是初始化处理器映射、视图解析等。这个servlet自己持有的上下文默认实现类也是XmlWebApplicationContext。初始化完毕后，spring以与servlet的名字相关(此处不是简单的以servlet名为Key，而是通过一些转换，具体可自行查看源码)的属性为属性Key，也将其存到ServletContext中，以便后续使用。这样每个servlet就持有自己的上下文，即拥有自己独立的bean空间，同时各个servlet共享相同的bean，即根上下文(第2步中初始化的上下文)定义的那些bean。

当Spring在执行ApplicationContext的getBean时，如果在自己context中找不到对应的bean，则会在父ApplicationContext中去找。这也解释了为什么我们可以在DispatcherServlet中获取到由ContextLoaderListener对应的ApplicationContext中的bean。
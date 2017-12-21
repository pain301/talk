interface Servlet {
  public void init(ServletConfig config) throws ServletException;
}

abstract class GenericServlet implements Servlet, ServletConfig,
        java.io.Serializable {
  public void init(ServletConfig config) throws ServletException {
    this.config = config;
    this.init();
  }

  // A convenience method which can be overridden so that there's no need to call super.init(config)
  public void init() throws ServletException {}
}

abstract class HttpServletBean extends HttpServlet
        implements EnvironmentCapable, EnvironmentAware {
  // Map config parameters onto bean properties of this servlet, and invoke subclass initialization
  public final void init() throws ServletException {
    // read config parameters from web.xml and set to ServletConfigPropertyValues
    PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
    // set servlet properties
    // subclass init
    initServletBean();
  }
}

// init(config) => init() => initServletBean()
abstract class FrameworkServlet extends HttpServletBean implements ApplicationContextAware {
  protected void onRefresh(ApplicationContext context) {
    // For subclasses: do nothing by default.
  }

  protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac) {
    wac.setServletContext(getServletContext());
    wac.setServletConfig(getServletConfig());
    wac.setNamespace(getNamespace());
    wac.addApplicationListener(new SourceFilteringListener(wac, new ContextRefreshListener()));
    wac.refresh();
  }

  // Instantiate the WebApplicationContext for this servlet
  protected WebApplicationContext createWebApplicationContext(ApplicationContext parent) {
    ConfigurableWebApplicationContext wac =
        (ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);
    wac.setEnvironment(getEnvironment());
    wac.setParent(parent);
    wac.setConfigLocation(getContextConfigLocation());
    configureAndRefreshWebApplicationContext(wac);

    return wac;
  }

  // Initialize and publish the WebApplicationContext for this servlet
  protected WebApplicationContext initWebApplicationContext() {
    // get root context from servlet context
    WebApplicationContext rootContext =
	WebApplicationContextUtils.getWebApplicationContext(getServletContext());

    // 1. A context instance was injected at construction time
    if (this.webApplicationContext != null) {
      wac = this.webApplicationContext;
      if (wac instanceof ConfigurableWebApplicationContext) {
        ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) wac;
        if (!cwac.isActive()) {
          // The context has not yet been refreshed and set the root context as the parent
          if (cwac.getParent() == null) {
            cwac.setParent(rootContext);
          }
          configureAndRefreshWebApplicationContext(cwac);
        }
      }
    }

    // 2. Find if one has been registered in context if no context was injected at construction time
    if (wac == null) {
      wac = findWebApplicationContext();
    }

    // 3. Create a local context instance
    if (wac == null) {
      wac = createWebApplicationContext(rootContext);
    }

    if (!this.refreshEventReceived) {
      onRefresh(wac);
    }

    // Publish the context as a servlet context attribute
    if (this.publishContext) {
      String attrName = getServletContextAttributeName();
      getServletContext().setAttribute(attrName, wac);
    }

    return wac;

    // 1. DispatcherServlet(WebApplicationContext webApplicationContext)
    // 2. DispatcherServlet() and createWebApplicationContext(rootContext), default context class is XmlWebApplicationContext.class
    // createWebApplicationContext(ApplicationContext parent) wac.setParent(parent);
    // onRefresh(wac);
    // getServletContext().setAttribute(attrName, wac);
  }

  protected void initFrameworkServlet() throws ServletException {}

  protected final void initServletBean() throws ServletException {
    this.webApplicationContext = initWebApplicationContext();
    initFrameworkServlet();
  }
}

class DispatcherServlet extends FrameworkServlet {
  protected void onRefresh(ApplicationContext context) {
    initStrategies(context);
  }
}

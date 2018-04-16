StandardServer
VersionLoggerListener
AprLifecycleListener
JreMemoryLeakPreventionListener
GlobalResourcesLifecycleListener
ThreadLocalLeakPreventionListener
StandardService
Connector HTTP
Connector AJP
StandardEngine
StandardHost

启动 Server => 通知 Service 完成启动 => 通知 Connector 完成初始化和启动的过程 => 调用 ProtocolHandler 完成 http 协议解析 => SocketProcessor 解析请求头 => CoyoteAdapter 解析请求行和请求体 => 把解析信息封装到 Request 和 Response对象中 => 把请求交给 Container 容器 => 子容器 Engine 容器 => Engine 容器匹配其所有的虚拟主机 => Host 容器 => Host 容器匹配其所有子容器 Context => Context 匹配其子容器 Wrapper => Wrapper 容器加载对应 Servlet => Connector 构建 Request 及 Response 对象，使用反射调用 Servelt 的 service 方法 => Context 容器把响应消息 Response 对象返回给 Host 容器 => Host 容器把 Response 返回给 Engine 容器 => Engine 容器返回给 Connector => Connetor 容器把 Response 返回给浏览器

```java
public enum LifecycleState {
    NEW(false, null),
    INITIALIZING(false, Lifecycle.BEFORE_INIT_EVENT),
    INITIALIZED(false, Lifecycle.AFTER_INIT_EVENT),
    STARTING_PREP(false, Lifecycle.BEFORE_START_EVENT),
    STARTING(true, Lifecycle.START_EVENT),
    STARTED(true, Lifecycle.AFTER_START_EVENT),
    STOPPING_PREP(true, Lifecycle.BEFORE_STOP_EVENT),
    STOPPING(false, Lifecycle.STOP_EVENT),
    STOPPED(false, Lifecycle.AFTER_STOP_EVENT),
    DESTROYING(false, Lifecycle.BEFORE_DESTROY_EVENT),
    DESTROYED(false, Lifecycle.AFTER_DESTROY_EVENT),
    FAILED(false, null);

    private final boolean available;
    private final String lifecycleEvent;

    private LifecycleState(boolean available, String lifecycleEvent) {
        this.available = available;
        this.lifecycleEvent = lifecycleEvent;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getLifecycleEvent() {
        return lifecycleEvent;
    }
}
```
```java
public final class LifecycleEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    public LifecycleEvent(Lifecycle lifecycle, String type, Object data) {
        super(lifecycle);
        this.type = type;
        this.data = data;
    }

    private final Object data;

    private final String type;

    public Object getData() {
        return (this.data);
    }

    public Lifecycle getLifecycle() {
        return (Lifecycle) getSource();
    }

    public String getType() {
        return (this.type);
    }
}
```
```java
public interface Lifecycle {

    /**
     * The LifecycleEvent type for the "component before init" event.
     */
    public static final String BEFORE_INIT_EVENT = "before_init";


    /**
     * The LifecycleEvent type for the "component after init" event.
     */
    public static final String AFTER_INIT_EVENT = "after_init";


    /**
     * The LifecycleEvent type for the "component start" event.
     */
    public static final String START_EVENT = "start";


    /**
     * The LifecycleEvent type for the "component before start" event.
     */
    public static final String BEFORE_START_EVENT = "before_start";


    /**
     * The LifecycleEvent type for the "component after start" event.
     */
    public static final String AFTER_START_EVENT = "after_start";


    /**
     * The LifecycleEvent type for the "component stop" event.
     */
    public static final String STOP_EVENT = "stop";


    /**
     * The LifecycleEvent type for the "component before stop" event.
     */
    public static final String BEFORE_STOP_EVENT = "before_stop";


    /**
     * The LifecycleEvent type for the "component after stop" event.
     */
    public static final String AFTER_STOP_EVENT = "after_stop";


    /**
     * The LifecycleEvent type for the "component after destroy" event.
     */
    public static final String AFTER_DESTROY_EVENT = "after_destroy";


    /**
     * The LifecycleEvent type for the "component before destroy" event.
     */
    public static final String BEFORE_DESTROY_EVENT = "before_destroy";


    /**
     * The LifecycleEvent type for the "periodic" event.
     */
    public static final String PERIODIC_EVENT = "periodic";


    /**
     * The LifecycleEvent type for the "configure_start" event. Used by those
     * components that use a separate component to perform configuration and
     * need to signal when configuration should be performed - usually after
     * {@link #BEFORE_START_EVENT} and before {@link #START_EVENT}.
     */
    public static final String CONFIGURE_START_EVENT = "configure_start";


    /**
     * The LifecycleEvent type for the "configure_stop" event. Used by those
     * components that use a separate component to perform configuration and
     * need to signal when de-configuration should be performed - usually after
     * {@link #STOP_EVENT} and before {@link #AFTER_STOP_EVENT}.
     */
    public static final String CONFIGURE_STOP_EVENT = "configure_stop";

    public void addLifecycleListener(LifecycleListener listener);

    public LifecycleListener[] findLifecycleListeners();

    public void removeLifecycleListener(LifecycleListener listener);

    public void init() throws LifecycleException;

    public void start() throws LifecycleException;

    public void stop() throws LifecycleException;

    public void destroy() throws LifecycleException;

    public LifecycleState getState();

    public String getStateName();

    public interface SingleUse {
    }
}
```
```java
public interface LifecycleListener {

    public void lifecycleEvent(LifecycleEvent event);
}
```
```java
public final class LifecycleSupport {

    public LifecycleSupport(Lifecycle lifecycle) {
        super();
        this.lifecycle = lifecycle;
    }

    private final Lifecycle lifecycle;

    private final List<LifecycleListener> listeners = new CopyOnWriteArrayList<>();

    public void addLifecycleListener(LifecycleListener listener) {
        listeners.add(listener);
    }

    /**
     * Get the lifecycle listeners associated with this lifecycle. If this
     * Lifecycle has no listeners registered, a zero-length array is returned.
     */
    public LifecycleListener[] findLifecycleListeners() {
        return listeners.toArray(new LifecycleListener[0]);
    }

    public void fireLifecycleEvent(String type, Object data) {
        LifecycleEvent event = new LifecycleEvent(lifecycle, type, data);
        for (LifecycleListener listener : listeners) {
            listener.lifecycleEvent(event);
        }
    }

    public void removeLifecycleListener(LifecycleListener listener) {
        listeners.remove(listener);
    }
}
```
```java
public abstract class LifecycleBase implements Lifecycle {

    private static final Log log = LogFactory.getLog(LifecycleBase.class);

    private static final StringManager sm =
        StringManager.getManager("org.apache.catalina.util");

    private final LifecycleSupport lifecycle = new LifecycleSupport(this);

    private volatile LifecycleState state = LifecycleState.NEW;

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }

    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }

    protected void fireLifecycleEvent(String type, Object data) {
        lifecycle.fireLifecycleEvent(type, data);
    }


    @Override
    public final synchronized void init() throws LifecycleException {
        if (!state.equals(LifecycleState.NEW)) {
            invalidTransition(Lifecycle.BEFORE_INIT_EVENT);
        }

        try {
            setStateInternal(LifecycleState.INITIALIZING, null, false);
            initInternal();
            setStateInternal(LifecycleState.INITIALIZED, null, false);
        } catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            setStateInternal(LifecycleState.FAILED, null, false);
            throw new LifecycleException(
                    sm.getString("lifecycleBase.initFail",toString()), t);
        }
    }

    protected abstract void initInternal() throws LifecycleException;

    @Override
    public final synchronized void start() throws LifecycleException {

        if (LifecycleState.STARTING_PREP.equals(state) || LifecycleState.STARTING.equals(state) ||
                LifecycleState.STARTED.equals(state)) {

            if (log.isDebugEnabled()) {
                Exception e = new LifecycleException();
                log.debug(sm.getString("lifecycleBase.alreadyStarted", toString()), e);
            } else if (log.isInfoEnabled()) {
                log.info(sm.getString("lifecycleBase.alreadyStarted", toString()));
            }

            return;
        }

        if (state.equals(LifecycleState.NEW)) {
            init();
        } else if (state.equals(LifecycleState.FAILED)) {
            stop();
        } else if (!state.equals(LifecycleState.INITIALIZED) &&
                !state.equals(LifecycleState.STOPPED)) {
            invalidTransition(Lifecycle.BEFORE_START_EVENT);
        }

        try {
            setStateInternal(LifecycleState.STARTING_PREP, null, false);
            startInternal();
            if (state.equals(LifecycleState.FAILED)) {
                // This is a 'controlled' failure. The component put itself into the
                // FAILED state so call stop() to complete the clean-up.
                stop();
            } else if (!state.equals(LifecycleState.STARTING)) {
                // Shouldn't be necessary but acts as a check that sub-classes are
                // doing what they are supposed to.
                invalidTransition(Lifecycle.AFTER_START_EVENT);
            } else {
                setStateInternal(LifecycleState.STARTED, null, false);
            }
        } catch (Throwable t) {
            // This is an 'uncontrolled' failure so put the component into the
            // FAILED state and throw an exception.
            ExceptionUtils.handleThrowable(t);
            setStateInternal(LifecycleState.FAILED, null, false);
            throw new LifecycleException(sm.getString("lifecycleBase.startFail", toString()), t);
        }
    }

    protected abstract void startInternal() throws LifecycleException;

    @Override
    public final synchronized void stop() throws LifecycleException {

        if (LifecycleState.STOPPING_PREP.equals(state) || LifecycleState.STOPPING.equals(state) ||
                LifecycleState.STOPPED.equals(state)) {

            if (log.isDebugEnabled()) {
                Exception e = new LifecycleException();
                log.debug(sm.getString("lifecycleBase.alreadyStopped", toString()), e);
            } else if (log.isInfoEnabled()) {
                log.info(sm.getString("lifecycleBase.alreadyStopped", toString()));
            }

            return;
        }

        if (state.equals(LifecycleState.NEW)) {
            state = LifecycleState.STOPPED;
            return;
        }

        if (!state.equals(LifecycleState.STARTED) && !state.equals(LifecycleState.FAILED)) {
            invalidTransition(Lifecycle.BEFORE_STOP_EVENT);
        }

        try {
            if (state.equals(LifecycleState.FAILED)) {
                // Don't transition to STOPPING_PREP as that would briefly mark the
                // component as available but do ensure the BEFORE_STOP_EVENT is
                // fired
                fireLifecycleEvent(BEFORE_STOP_EVENT, null);
            } else {
                setStateInternal(LifecycleState.STOPPING_PREP, null, false);
            }

            stopInternal();

            // Shouldn't be necessary but acts as a check that sub-classes are
            // doing what they are supposed to.
            if (!state.equals(LifecycleState.STOPPING) && !state.equals(LifecycleState.FAILED)) {
                invalidTransition(Lifecycle.AFTER_STOP_EVENT);
            }

            setStateInternal(LifecycleState.STOPPED, null, false);
        } catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            setStateInternal(LifecycleState.FAILED, null, false);
            throw new LifecycleException(sm.getString("lifecycleBase.stopFail",toString()), t);
        } finally {
            if (this instanceof Lifecycle.SingleUse) {
                // Complete stop process first
                setStateInternal(LifecycleState.STOPPED, null, false);
                destroy();
            }
        }
    }

    protected abstract void stopInternal() throws LifecycleException;

    @Override
    public final synchronized void destroy() throws LifecycleException {
        if (LifecycleState.FAILED.equals(state)) {
            try {
                // Triggers clean-up
                stop();
            } catch (LifecycleException e) {
                // Just log. Still want to destroy.
                log.warn(sm.getString(
                        "lifecycleBase.destroyStopFail", toString()), e);
            }
        }

        if (LifecycleState.DESTROYING.equals(state) ||
                LifecycleState.DESTROYED.equals(state)) {

            if (log.isDebugEnabled()) {
                Exception e = new LifecycleException();
                log.debug(sm.getString("lifecycleBase.alreadyDestroyed", toString()), e);
            } else if (log.isInfoEnabled() && !(this instanceof Lifecycle.SingleUse)) {
                // Rather than have every component that might need to call
                // destroy() check for SingleUse, don't log an info message if
                // multiple calls are made to destroy()
                log.info(sm.getString("lifecycleBase.alreadyDestroyed", toString()));
            }

            return;
        }

        if (!state.equals(LifecycleState.STOPPED) &&
                !state.equals(LifecycleState.FAILED) &&
                !state.equals(LifecycleState.NEW) &&
                !state.equals(LifecycleState.INITIALIZED)) {
            invalidTransition(Lifecycle.BEFORE_DESTROY_EVENT);
        }

        try {
            setStateInternal(LifecycleState.DESTROYING, null, false);
            destroyInternal();
            setStateInternal(LifecycleState.DESTROYED, null, false);
        } catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            setStateInternal(LifecycleState.FAILED, null, false);
            throw new LifecycleException(
                    sm.getString("lifecycleBase.destroyFail",toString()), t);
        }
    }


    protected abstract void destroyInternal() throws LifecycleException;

    @Override
    public LifecycleState getState() {
        return state;
    }

    @Override
    public String getStateName() {
        return getState().toString();
    }

    protected synchronized void setState(LifecycleState state)
            throws LifecycleException {
        setStateInternal(state, null, true);
    }

    protected synchronized void setState(LifecycleState state, Object data)
            throws LifecycleException {
        setStateInternal(state, data, true);
    }

    private synchronized void setStateInternal(LifecycleState state,
            Object data, boolean check) throws LifecycleException {

        if (log.isDebugEnabled()) {
            log.debug(sm.getString("lifecycleBase.setState", this, state));
        }

        if (check) {
            // Must have been triggered by one of the abstract methods (assume
            // code in this class is correct)
            // null is never a valid state
            if (state == null) {
                invalidTransition("null");
                // Unreachable code - here to stop eclipse complaining about
                // a possible NPE further down the method
                return;
            }

            // Any method can transition to failed
            // startInternal() permits STARTING_PREP to STARTING
            // stopInternal() permits STOPPING_PREP to STOPPING and FAILED to
            // STOPPING
            if (!(state == LifecycleState.FAILED ||
                    (this.state == LifecycleState.STARTING_PREP &&
                            state == LifecycleState.STARTING) ||
                    (this.state == LifecycleState.STOPPING_PREP &&
                            state == LifecycleState.STOPPING) ||
                    (this.state == LifecycleState.FAILED &&
                            state == LifecycleState.STOPPING))) {
                // No other transition permitted
                invalidTransition(state.name());
            }
        }

        this.state = state;
        String lifecycleEvent = state.getLifecycleEvent();
        if (lifecycleEvent != null) {
            fireLifecycleEvent(lifecycleEvent, data);
        }
    }

    private void invalidTransition(String type) throws LifecycleException {
        String msg = sm.getString("lifecycleBase.invalidTransition", type,
                toString(), state);
        throw new LifecycleException(msg);
    }
}
```

```java
public class Request implements ServletRequest {}
public class RequestFacade implements ServletRequest {
  private ServletRequest request = null;
  public RequestFacade(Request request) {
    this.request = request;
  }
}
```

HttpProcessor
```java
synchronized void assign(Socket socket) {
    while (available) {
        try {
            wait();
        } catch (InterruptedException e) {
        }
    }
    this.socket = socket;
    available = true;
    notifyAll();
    if ((debug >= 1) && (socket != null))
        log(" An incoming request is being assigned");
}
```
```java
public void run() { 
    while (!stopped) { 
        Socket socket = await(); 
        if (socket == null) 
            continue; 
        try { 
            process(socket); 
        } catch (Throwable t) { 
            log("process.invoke", t); 
        } 
        connector.recycle(this); 
    } 
    synchronized (threadSync) { 
        threadSync.notifyAll(); 
    } 
}
```
```java
private void process(Socket socket) {
    boolean ok = true;
    boolean finishResponse = true;
    SocketInputStream input = null;
    OutputStream output = null;
    try {
        input = new SocketInputStream(socket.getInputStream(),connector.getBufferSize());
    } catch (Exception e) {
        log("process.create", e);
        ok = false;
    }
    keepAlive = true;
    while (!stopped && ok && keepAlive) {
        finishResponse = true;
        try {
            request.setStream(input);
            request.setResponse(response);
            output = socket.getOutputStream();
            response.setStream(output);
            response.setRequest(request);
            ((HttpServletResponse) response.getResponse())
                .setHeader("Server", SERVER_INFO);
        } catch (Exception e) {
            log("process.create", e);
            ok = false;
        }

        try {
            if (ok) {
                parseConnection(socket);
                parseRequest(input, output);
                if (!request.getRequest().getProtocol().startsWith("HTTP/0"))
                    parseHeaders(input);
                if (http11) {
                    ackRequest(output);
                    if (connector.isChunkingAllowed())
                        response.setAllowChunking(true);
                }
            }
        }
        try {
            ((HttpServletResponse) response).setHeader
                ("Date", FastHttpDateFormat.getCurrentDate());
            if (ok) {
                connector.getContainer().invoke(request, response);
            }
        }
        try {
            shutdownInput(input);
            socket.close();
        } catch (IOException e) {
            ;
        } catch (Throwable e) {
            log("process.invoke", e);
        }
      }
    socket = null;
}
```

Engine => Host => Context => Wrapper => Servlet
Pipeline => StandardWrapperValve => ApplicationFilterChain => Filter

Catalina 容器一共有四种不同的容器
Engine 容器包含 Host
Engine：表示整个Catalina的servlet引擎，Engine是一个接口，其标准实现类是StandardEngine，其闸门实现是 StandardEngineValve
```java
public void addChild(Container container);
```

Host 包含 Context
Host：表示一个拥有数个上下文的虚拟主机，作用是运行多个应用并管理这些应用，Host标准实现类是 StandardHost，其闸门实现是 StandardHostValve

Context 包含 Wrapper
web 项目中的一个Servlet类对应一个Wrapper，多个Servlet就对应多个Wrapper，当有多个Wrapper的时候就需要Context容器来管理这些Wrapper了，Context容器对应一个工程，部署一个工程到Tomcat中就会新创建一个Context容器

Context 容器是一个 Web 项目的代表，管理 Servlet 实例
Wrapper容器负责管理一个Servlet，包括Servlet的装载、初始化、资源回收。Wrapper是最底层的容器，其不能在添加子容器了。Wrapper是一个接口，其标准实现类是StandardWrapper

allocate方法负责定位该包装器表示的servlet的实例。Allocate方法必须考虑一个servlet是否实现了 SingleThreadModel 接口
load方法负责load和初始化servlet的实例

一个容器可以有一个或多个低层次上的子容器。例如，一个Context有一个或多个wrapper，而wrapper作为容器层次中的最底层，不能包含子容器。将一个容器添加到另一容器中可以使用在Container接口中定义的addChild()方法
删除一个容器可以使用Container接口中定义的removeChild()方法

流水线接口允许添加一个新的阀门或者删除一个阀门。最后，可以使用setBasic方法来分配一个基本阀门给流水线，getBasic方法会得到基本阀门。最后被唤醒的基本阀门，负责处理request和回复response

阀门接口表示一个阀门，该组件负责处理请求

LifecycleSupport类存储所有的生命周期监听器到一个数组中，该数组为listenens
一个实现了Lifecycle的组件可以使用LifecycleSupport类

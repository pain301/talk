1. 初始化
Bootstrap.init()
```java
public void init() throws Exception {

    // 初始化 commonLoader, catalinaLoader, sharedLoader
    initClassLoaders();
    Thread.currentThread().setContextClassLoader(catalinaLoader);
    SecurityClassLoad.securityClassLoad(catalinaLoader);

    // construct Catalina instance
    Class<?> startupClass = catalinaLoader.loadClass("org.apache.catalina.startup.Catalina");
    Object startupInstance = startupClass.getConstructor().newInstance();

    // set parent classloader for Catalina instance
    String methodName = "setParentClassLoader";
    Class<?> paramTypes[] = new Class[1];
    paramTypes[0] = Class.forName("java.lang.ClassLoader");
    Object paramValues[] = new Object[1];
    paramValues[0] = sharedLoader;
    Method method =
        startupInstance.getClass().getMethod(methodName, paramTypes);
    method.invoke(startupInstance, paramValues);

    catalinaDaemon = startupInstance;
}
```

2. 启动 Catalina
2.1 TODO
Bootstrap.setAwait(boolean await)
```java
public void setAwait(boolean await) throws Exception {
    Class<?> paramTypes[] = new Class[1];
    paramTypes[0] = Boolean.TYPE;
    Object paramValues[] = new Object[1];
    paramValues[0] = Boolean.valueOf(await);
    Method method =
        catalinaDaemon.getClass().getMethod("setAwait", paramTypes);
    method.invoke(catalinaDaemon, paramValues);
}
```

2.2 加载 Catalina
Bootstrap.load(String[] arguments)
```java
private void load(String[] arguments) throws Exception {
    String methodName = "load";
    Object param[];
    Class<?> paramTypes[];
    if (arguments==null || arguments.length==0) {
        paramTypes = null;
        param = null;
    } else {
        paramTypes = new Class[1];
        paramTypes[0] = arguments.getClass();
        param = new Object[1];
        param[0] = arguments;
    }
    Method method =
        catalinaDaemon.getClass().getMethod(methodName, paramTypes);
    method.invoke(catalinaDaemon, param);
}
```

2.3 启动 Catalina
Bootstrap.start()
```java
public void start() throws Exception {
    if( catalinaDaemon==null ) init();

    Method method = catalinaDaemon.getClass().getMethod("start", (Class [] )null);
    method.invoke(catalinaDaemon, (Object [])null);
}
```

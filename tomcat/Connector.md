```java
public Connector(String protocol) {
    setProtocol(protocol);
    // Instantiate protocol handler
    ProtocolHandler p = null;
    try {
        Class<?> clazz = Class.forName(protocolHandlerClassName);
        p = (ProtocolHandler) clazz.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
        log.error(sm.getString(
                "coyoteConnector.protocolHandlerInstantiationFailed"), e);
    } finally {
        this.protocolHandler = p;
    }

    if (!Globals.STRICT_SERVLET_COMPLIANCE) {
        URIEncoding = "UTF-8";
        URIEncodingLower = URIEncoding.toLowerCase(Locale.ENGLISH);
    }
}
```

```java
protected void initInternal() throws LifecycleException {

    super.initInternal();

    // Initialize adapter
    adapter = new CoyoteAdapter(this);
    protocolHandler.setAdapter(adapter);

    // Make sure parseBodyMethodsSet has a default
    if( null == parseBodyMethodsSet ) {
        setParseBodyMethods(getParseBodyMethods());
    }

    if (protocolHandler.isAprRequired() &&
            !AprLifecycleListener.isAprAvailable()) {
        throw new LifecycleException(
                sm.getString("coyoteConnector.protocolHandlerNoApr",
                        getProtocolHandlerClassName()));
    }

    try {
        protocolHandler.init();
    } catch (Exception e) {
        throw new LifecycleException
            (sm.getString
             ("coyoteConnector.protocolHandlerInitializationFailed"), e);
    }
}
```

```java
protected void startInternal() throws LifecycleException {

    // Validate settings before starting
    if (getPort() < 0) {
        throw new LifecycleException(sm.getString(
                "coyoteConnector.invalidPort", Integer.valueOf(getPort())));
    }

    setState(LifecycleState.STARTING);

    try {
        protocolHandler.start();
    } catch (Exception e) {
        String errPrefix = "";
        if(this.service != null) {
            errPrefix += "service.getName(): \"" + this.service.getName() + "\"; ";
        }

        throw new LifecycleException
            (errPrefix + " " + sm.getString
             ("coyoteConnector.protocolHandlerStartFailed"), e);
    }
}
```
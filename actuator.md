```
management.endpoint.shutdown.enabled=true
management.security.enabled=false
```

customize the /beans endpoint
```
endpoints.beans.id=springbeans
endpoints.beans.sensitive=false
endpoints.beans.enabled=true
```

```
endpoints.health.sensitive=false
```

```java
@Component
public class HealthCheck implements HealthIndicator {
  
    @Override
    public Health health() {
        int errorCode = check(); // perform some specific health check
        if (errorCode != 0) {
            return Health.down()
              .withDetail("Error Code", errorCode).build();
        }
        return Health.up().build();
    }
     
    public int check() {
        // Our logic to check health
        return 0;
    }
}
```

gather custom metrics
```java
@Service
public class LoginServiceImpl {
 
    private final CounterService counterService;
     
    public LoginServiceImpl(CounterService counterService) {
        this.counterService = counterService;
    }
     
    public boolean login(String userName, char[] password) {
        boolean success;
        if (userName.equals("admin") && "secret".toCharArray().equals(password)) {
            counterService.increment("counter.login.success");
            success = true;
        } else {
            counterService.increment("counter.login.failure");
            success = false;
        }
        return success;
    }
}
```

Creating A New Endpoint
```java
@Component
public class CustomEndpoint implements Endpoint<List<String>> {
     
    @Override
    public String getId() {
        return "customEndpoint";
    }
 
    @Override
    public boolean isEnabled() {
        return true;
    }
 
    @Override
    public boolean isSensitive() {
        return true;
    }
 
    @Override
    public List<String> invoke() {
        // Custom logic to build the output
        List<String> messages = new ArrayList<String>();
        messages.add("This is message 1");
        messages.add("This is message 2");
        return messages;
    }
}
```

```
#port used to expose actuator
management.port=8081 
#CIDR allowed to hit actuator
management.address=127.0.0.1 
#Whether security should be enabled or disabled altogether
management.security.enabled=false

security.user.name=admin
security.user.password=secret
management.security.role=SUPERUSER
```

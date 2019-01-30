```java
public interface Authenticator {
    // 认证成功返回User，认证失败抛出异常，无认证信息返回null:
    User authenticate(HttpServletRequest request, HttpServletResponse response) throws AuthenticateException;
}

public LocalCookieAuthenticator implements Authenticator {
    public User authenticate(HttpServletRequest request, HttpServletResponse response) {
        String cookie = getCookieFromRequest(request, 'cookieName');
        if (cookie == null) {
            return null;
        }
        return getUserByCookie(cookie);
    }
}

public BasicAuthenticator implements Authenticator {
    public User authenticate(HttpServletRequest request, HttpServletResponse response) {
        String auth = getHeaderFromRequest(request, "Authorization");
        if (auth == null) {
            return null;
        }
        String username = parseUsernameFromAuthorizationHeader(auth);
        String password = parsePasswordFromAuthorizationHeader(auth);
        return authenticateUserByPassword(username, password);
    }
}

public APIAuthenticator implements Authenticator {
    public User authenticate(HttpServletRequest request, HttpServletResponse response) {
        String token = getHeaderFromRequest(request, "X-API-Token");
        if (token == null) {
            return null;
        }
        return authenticateUserByAPIToken(token);
    }
}
```

```java
public class UserContext implements AutoCloseable {

    static final ThreadLocal<User> current = new ThreadLocal<User>();

    public UserContext(User user) {
        current.set(user);
    }

    public static User getCurrentUser() {
        return current.get();
    }

    public void close() {
        current.remove();
    }
}
```
```java
public class GlobalFilter implements Filter {
    // 所有的Authenticator都在这里:
    Authenticator[] authenticators = initAuthenticators();

    // 每个页面都会执行的代码:
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        User user = null;
        for (Authenticator auth : this.authenticators) {
            user = auth.authenticate(request, response);
            if (user != null) {
                break;
            }
        }
        // 把User绑定到UserContext中
        UserContext.current.set(user);
        chain.doFilter(request, response);
        UserContext.current.remove(user);
    }
}
```
```
User user = UserContext.current.get();
```

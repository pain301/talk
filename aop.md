```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```
```
spring.aop.proxy-target-class=true
```
```java
// 定义为切面类
@Aspect
@Component
public class WebLogAspect {

    ThreadLocal<Long> startTime = new ThreadLocal<>();

    // 定义切入点
    @Pointcut("execution(public * com.pain.flame..*.*(..))")
    public void log(){}

    @Before("log()")
    public void doBefore(JoinPoint joinPoint) throws Throwable {
        startTime.set(System.currentTimeMillis());
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        logger.info("URL : " + request.getRequestURL().toString());
        logger.info("HTTP_METHOD : " + request.getMethod());
        logger.info("IP : " + request.getRemoteAddr());
        logger.info("CLASS_METHOD : " + joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());
        logger.info("ARGS : " + Arrays.toString(joinPoint.getArgs()));
    }

    @AfterReturning(returning = "ret", pointcut = "log()")
    public void doAfterReturning(Object ret) throws Throwable {
        logger.info("RESPONSE: " + ret);
        logger.info("TIME: " + (System.currentTimeMillis() - startTime.get()));
    }
}
```

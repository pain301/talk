默认的模板配置路径为：src/main/resources/templates
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```
```sh
# Enable template caching.
spring.thymeleaf.cache=true

# Check that the templates location exists.
spring.thymeleaf.check-template-location=true

# Content-Type value.
spring.thymeleaf.content-type=text/html

# Enable MVC Thymeleaf view resolution.
spring.thymeleaf.enabled=true

# Template encoding.
spring.thymeleaf.encoding=UTF-8

# Comma-separated list of view names that should be excluded from resolution.
spring.thymeleaf.excluded-view-names= 

# Template mode to be applied to templates. See also StandardTemplateModeHandlers.
spring.thymeleaf.mode=HTML5

# Prefix that gets prepended to view names when building a URL.
spring.thymeleaf.prefix=classpath:/templates/

# Suffix that gets appended to view names when building a URL.
spring.thymeleaf.suffix=.html

# Order of the template resolver in the chain.
spring.thymeleaf.template-resolver-order=

# Comma-separated list of view names that can be resolved.
spring.thymeleaf.view-names= 
```
```java
@Controller
public class HelloController {
    @RequestMapping("/")
    public String index(ModelMap map) {
        map.addAttribute("host", "127.0.0.1");
        // 返回模板文件的名称，对应 src/main/resources/templates/index.html
        return "index";  
    }
}
```

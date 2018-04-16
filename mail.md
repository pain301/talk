```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```
```
spring.mail.host=smtp.qq.com
spring.mail.username=pain
spring.mail.password=123
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```
```java
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class ApplicationTests {

  @Autowired
  private JavaMailSender mailSender;

  @Test
  public void sendSimpleMail() throws Exception {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom("pain@qq.com");
    message.setTo("page@qq.com");
    message.setSubject("subject");
    message.setText("content");
    mailSender.send(message);
  }

  @Test
  public void sendAttachmentsMail() throws Exception {
    MimeMessage mimeMessage = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
    helper.setFrom("pain@qq.com");
    helper.setTo("page@qq.com");
    helper.setSubject("subject");
    helper.setText("content");

    FileSystemResource file = new FileSystemResource(new File("bg.jpg"));
    helper.addAttachment("attachment-1.jpg", file);
    helper.addAttachment("attachment-2.jpg", file);

    mailSender.send(mimeMessage);
  }

  @Test
  public void sendInlineMail() throws Exception {
    MimeMessage mimeMessage = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
    helper.setFrom("pain@qq.com");
    helper.setTo("page@qq.com");
    helper.setSubject("subject");
    helper.setText("<html><body><img src=\"cid:tag\" ></body></html>", true);

    FileSystemResource file = new FileSystemResource(new File("bg.jpg"));
    helper.addInline("tag", file);
    mailSender.send(mimeMessage);
  }

  @Test
  public void sendTemplateMail() throws Exception {
    MimeMessage mimeMessage = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
    helper.setFrom("pain@qq.com");
    helper.setTo("page@qq.com");
    helper.setSubject("subject");

    Map<String, Object> model = new HashedMap();
    model.put("username", "page");
    String text = VelocityEngineUtils.mergeTemplateIntoString(
        velocityEngine, "template.vm", "UTF-8", model);
    helper.setText(text, true);
    mailSender.send(mimeMessage);
  }
}
```
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-velocity</artifactId>
</dependency>
```

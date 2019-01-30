Prevent cross-site scripting attacks by encoding HTML responses
org.springframework.web.util.HtmlUtils
防止出现非持久型 XSS 漏洞
Web 页面渲染的所有内容或者渲染的数据都必须来自于服务端
尽量不要从 URL，document.referrer，document.forms 等这种 DOM API 中获取数据直接渲染
尽量不要使用 eval, new Function()，document.write()，document.writeln()，window.setInterval()，window.setTimeout()，innerHTML，document.creteElement() 等可执行字符串的方法
对涉及 DOM 渲染的方法传入的字符串参数做 escape 转义
前端渲染的时候对任何的字段都需要做 escape 转义编码

防止持久型 XSS 漏洞
后端在入库前应该选择不相信任何前端数据，将所有的字段统一进行转义处理
后端在输出给前端数据统一进行转义处理
前端在渲染页面 DOM 的时候应该选择不相信任何后端数据，任何字段都需要做转义处理

为每个用户生成一个唯一的 cookie token，所有表单都包含同一个伪随机值，这种方案最简单，因为攻击者不能获得第三方的 cookie(理论上)，所以表单中的数据也就构造失败，但是由于用户的 cookie 很容易由于网站的 XSS 漏洞而被盗取，所以这个方案必须要在没有 XSS 的情况下才安全

每个 POST 请求使用验证码，这个方案算是比较完美的，但是需要用户多次输入验证码，用户体验比较差，所以不适合在业务中大量运用

渲染表单的时候，为每一个表单包含一个 csrfToken，提交表单的时候，带上 csrfToken，然后在后端做 csrfToken 验证

防御 CSRF 攻击主要有三种策略：验证 HTTP Referer 字段；在请求地址中添加 token 并验证；在 HTTP 头中自定义属性并验证

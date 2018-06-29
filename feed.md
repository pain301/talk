PUSH：valid user
PULL：invalid user
```sql
# feeds 临时表
SELECT id FROM feeds where uid in (following uid list) AND timeline > (last) ORDER BY id DESC LIMIT n;
```

```sh
# feed 长度：500
zadd feed_1001 1523955531 id1 1523955631 id2
hmset news_1001 id1 'news content' id2 'news content'
```
feed: id, user_id, timeline
news: id, user_id, content

数据的拆分，按照用户的 UID 拆分
按照时间拆分

发布 Feed 流程
1. Feed 消息先进入一个队列服务
2. 先从关注列表中读取到自己的粉丝列表，以及判断自己是否是大V
3. 将自己的 Feed 消息写入个人页 Timeline（发件箱）。如果是大V，写入流程到此就结束了
4. 如果是普通用户，还需要将自己的Feed消息写给自己的粉丝，如果有100个粉丝，那么就要写给100个用户，包括Feed内容和Feed ID

读取Feed流流程
1. 先去读取自己关注的大V列表
2. 去读取自己的收件箱，只需要一个GetRange读取一个范围即可，范围起始位置是上次读3. 取到的最新Feed的ID，结束位置可以使当前时间，也可以是MAX，建议是MAX值。由于之前使用了主键自增功能，所以这里可以使用GetRange读取。
3. 如果有关注的大V，则再次并发读取每一个大V的发件箱，如果关注了10个大V，那么则需要10次访问。
4. 合并2和3步的结果，然后按时间排序，返回给用户

## KAFKA

bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test
bin/kafka-topics.sh --list --zookeeper localhost:2181
bin/kafka-topics.sh --describe --zookeeper localhost:2181 --topic test
bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test --from-beginning

broker
```
broker.id=0
port=9092
log.dirs=/tmp/kafka-logs
zookeeper.connect=localhost:2181
zookeeper.connection.timeout.ms=1000000
```

# Timeout in ms for connecting to zookeeper
zookeeper.connection.timeout.ms=6000


session.timeout.ms -> defualt 30s
max.poll.interval.ms -> default 5min

enable.auto.commit
auto.commit.interval.ms

replica.lag.max.messages
replica.lag.time.max

replication.factor
min.insync.replicas

producer
```
props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
props.setProperty(ProducerConfig.ACKS_CONFIG, "all");
props.setProperty(ProducerConfig.RETRIES_CONFIG, "1");
```

each broker: (index file handle + data file handle) / every log segment


topic
n partitions
n consumers

b1: topicAp1-leader, topicAp2-follower, topicAp3-follower
b2: topicAp1-follower, topicAp2-leader, topicAp3-follower
b3: topicAp1-follower, topicAp2-follower, topicAp3-leader

ISR in sync replicas
replica.lag.max.messages 4   deprecated in 0.9
replica.lag.time.max.ms  500

max.poll.interval.ms
session.timeout.ms
auto.offset.reset
enable.auto.commit
auto.commit.interval.ms

last committed offset
init pos
cur pos
high watermark
log end offset

begin, committed, current, end


Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("group.id", "consumer-test");
props.setProperty("max.poll.records", 50);
props.put("key.deserializer", StringDeserializer.class.getName());
props.put("value.deserializer", StringDeserializer.class.getName());
props.setProperty("enable.auto.commit", "true");
KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props); 

consumer.subscribe(Arrays.asList(“foo”, “bar”));

try {
    while (running) {
        ConsumerRecords<String, String> records = consumer.poll(1000);
        for (ConsumerRecord<String, String> record : records)
            System.out.println(record.offset() + ": " + record.value());
    }
} finally {
    consumer.close();
}

try {
    while (true) {
        ConsumerRecords<String, String> records = consumer.poll(1000);
        for (ConsumerRecord<String, String> record : records)
            System.out.println(record.offset() + ": " +record.value());
    }
} catch (WakeupException e) {
    // ignore for shutdown
} finally {
    consumer.close();
}

consumer.wakeup()


public class ConsumerLoop implements Runnable {
    private final KafkaConsumer<String, String> consumer;
    private final List<String> topics;
    private final int id;

    public ConsumerLoop(int id,
                        String groupId,
                        List<String> topics) {
        this.id = id;
        this.topics = topics;
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", groupId);
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());
        this.consumer = new KafkaConsumer<>(props);
    }

    @Override
    public void run() {
        try {
            consumer.subscribe(topics);

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Long.MAX_VALUE);
                for (ConsumerRecord<String, String> record : records) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("partition", record.partition());
                    data.put("offset", record.offset());
                    data.put("value", record.value());
                    System.out.println(this.id + ": " + data);
                }
            }
        } catch (WakeupException e) {
            // ignore for shutdown 
        } finally {
            consumer.close();
        }
    }

    public void shutdown() {
        consumer.wakeup();
    }
}

public static void main(String[] args) {
    int numConsumers = 3;
    String groupId = "consumer-test-group"
    List<String> topics = Arrays.asList("consumer-test");
    ExecutorService executor = Executors.newFixedThreadPool(numConsumers);

    final List<ConsumerLoop> consumers = new ArrayList<>();
    for (int i = 0; i < numConsumers; i++) {
        ConsumerLoop consumer = new ConsumerLoop(i, groupId, topics);
        consumers.add(consumer);
        executor.submit(consumer);
    }

    Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
            for (ConsumerLoop consumer : consumers) {
                consumer.shutdown();
            }
            executor.shutdown();
            try {
                executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace;
            }
        }
    });
}

try {
    while (running) {
        ConsumerRecords<String, String> records = consumer.poll(1000);
        for (ConsumerRecord<String, String> record : records)
            System.out.println(record.offset() + ": " + record.value());

        try {
            consumer.commitSync();
        } catch (CommitFailedException e) {
            // application specific failure handling
        }
    }
} finally {
    consumer.close();
}

try {
    while (running) {
        ConsumerRecords<String, String> records = consumer.poll(1000);
        for (ConsumerRecord<String, String> record : records)
            System.out.println(record.offset() + ": " + record.value());

        consumer.commitAsync(new OffsetCommitCallback() {
            @Override
            public void onComplete(Map<TopicPartition, OffsetAndMetadata> offsets,
                                   Exception exception) {
                if (exception != null) {
                    // application specific failure handling
                }
            }
        });
    }
} finally {
    consumer.close();
}

automatic commit => at least once
at most once

bin/kafka-topics.sh --create --topic consumer-test --replication-factor 1 --partitions 3 --zookeeper localhost:2181
bin/kafka-verifiable-producer.sh --topic consumer-test --max-messages 200000 --broker-list localhost:9092

bin/kafka-consumer-groups.sh --new-consumer --describe --group consumer-test-group --bootstrap-server localhost:9092


======================

### RABBIT

Producer：消息生产者，即投递消息的程序
Broker：消息队列服务器实体
  Exchange：消息交换机，它指定消息按什么规则，路由到哪个队列
  Binding：绑定，它的作用就是把 Exchange 和 Queue 按照路由规则绑定起来
  Queue：消息队列载体，每个消息都会被投入到一个或多个队列
Consumer：消息消费者，即接受消息的程序

Binding
RoutingKey

Fanout Exchange
Direct Exchange
Topic Exchange

// 定义响应回调队列
String replyQueueName = channel.queueDeclare("", false, true, false, new HashMap<>()).getQueue();

// 设置回调队列到 Properties
AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
        .replyTo(replyQueueName)
        .build();
String request = "request";

// 发布请求
channel.basicPublish("", "rpc_queue", properties, request.getBytes());

exchange: crm_audit_log_exchange
queue: crm_audit_log_queue
routingKey: crm_audit_log
type: direct

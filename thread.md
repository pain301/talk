内置锁的重入机制在继承父类代码中同步操作时发挥作用
```java
public class A {
    public synchronized void function() {
    }
}

public class B extends A {
    public synchronized void funtcion() {
        super.function();
    }
}
```
加锁机制可以确保可见性、原子性和不可重排序性，但是volatile变量只能确保可见性和不可重排序性


Happens-Before 规则
程序顺序规则：一个线程中的每个操作，先于随后该线程中的任意后续操作执行（针对可见性而言）
监视器锁规则：对一个锁的解锁操作，先于随后对这个锁的获取操作执行
volatile变量规则：对一个volatile变量的写操作，先于对这个变量的读操作执行
传递性：如果A happens-before B，B happens-before C，那么A happens-before C
start规则：如果线程A执行线程B的start方法，那么线程A的ThreadB.start()先于线程B的任意操作执行
join规则：如果线程A执行线程B的join方法，那么线程B的任意操作先于线程A从TreadB.join()方法成功返回之前执行
中断规则：当线程A调用另一个线程B的interrupt方法时，必须在线程A检测到线程B被中断（抛出InterruptException，或者调用ThreadB.isInterrupted()）之前执行
终结器规则：一个对象的构造函数先于该对象的finalizer方法执行前完成


Java中提供Timer来执行延时任务和周期任务，但是Timer类有以下的缺陷：
Timer只会创建一个线程来执行任务，如果有一个TimerTask执行时间太长，就会影响到其他TimerTask的定时精度
Timer不会捕捉TimerTask未定义的异常，所以当有异常抛出到Timer中时，Timer就会崩溃，而且也无法恢复，就会影响到已经被调度但是没有执行的任务，造成“线程泄露”
建议使用ScheduledThreadPoolExecutor来代替Timer类
Callable支持任务有返回值，并支持异常的抛出。如果希望获得子线程的执行结果，那Callable将比Runnable更为合适

一次性提交一组任务可以使用CompletionService
ExecutorSerive的invokeAll()方法支持限时提交一组任务（任务的集合），并获得一个Future数组

```java
public class PrimeProducer extends Thread {
    private final BlockingQueue<BigInteger> queue;
    PrimeProducer(BlockingQueue<BigInteger> queue) {
        this.queue = queue;
    }

    public void run() {
        try {
            BigInteger p = BigInteger.ONE;
            //使用中断的方式来取消任务
            while (!Thread.currentThread().isInterrupted())
                //put方法会隐式检查并响应中断
                queue.put(p = p.nextProbablePrime());
        } catch (InterruptedException consumed) {
            /* 允许任务退出 */
        }
    }

    public void cancel() {
        interrupt();
    }
}
```
无界队列：newFixedThreadPool和newSingleThreadExecutor方法在默认情况下都是使用无界队列，当线程池中所有的任务都在忙碌时，达到的任务将会保存在队列中，如果任务达到的速率大于线程池处理任务的速率，任务队列就会无限地扩展。
有界队列：如ArrayBlockingQueue和有界的LinkedBlockingQueue，这是一种更为稳健的做法，可以防止任务队列无限扩展而耗尽资源，所以建议根据任务规模设置为进程池设置有界队列。
同步队列：为了避免任务的排队，可以使用同步队列SynchronousQueue,将任务从生产者直接提交给工作者（工作线程）。其实本质而言，同步队列不是一种队列，而是一种线程间进行移交的机制。当一个元素被的放入同步队列时，要求必须有一个线程（作为工作者）正在等待使用这个元素。如果线程池发现并没有线程在等待，且线程池大小没有达到最大时，便会新创建一个线程作为工作者去消费该任务。newCachedThreadPool方法便是使用同步队列，以提高效率

线程工厂
```java
public interface ThreadFactory {
    Thread newThread(Runnable r);
}

public class MyThreadFactory implements ThreadFactory {
    private final String poolName;

    public MyThreadFactory(String poolName) {
        this.poolName = poolName;
    }

    public Thread newThread(Runnable runnable) {
        return new MyAppThread(runnable, poolName);
    }
}
```
```java
public class MyAppThread extends Thread {
    public static final String DEFAULT_NAME = "MyAppThread";
    private static volatile boolean debugLifecycle = false;
    //线程编号标记位
    private static final AtomicInteger created = new AtomicInteger();
    //运行个数标记位
    private static final AtomicInteger alive = new AtomicInteger();
    private static final Logger log = Logger.getAnonymousLogger();

    public MyAppThread(Runnable r) {
        this(r, DEFAULT_NAME);
    }

    public MyAppThread(Runnable runnable, String name) {
        //新线程被创建，编号加一
        super(runnable, name + "-" + created.incrementAndGet());
        //定义如何处理未定义的异常处理器
        setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            public void uncaughtException(Thread t,
                                          Throwable e) {
                log.log(Level.SEVERE,
                        "UNCAUGHT in thread " + t.getName(), e);
            }
        });
    }

    public void run() {
        // 赋值Debug标志位；
        boolean debug = debugLifecycle;
        if (debug) log.log(Level.FINE, "Created " + getName());
        try {
            //有任务被执行，活动线程数加一
            alive.incrementAndGet();
            super.run();
        } finally {
            //线程执行完毕，活动线程数减一
            alive.decrementAndGet();
            if (debug) log.log(Level.FINE, "Exiting " + getName());
        }
    }

    public static int getThreadsCreated() {
        return created.get();
    }

    public static int getThreadsAlive() {
        return alive.get();
    }

    public static boolean getDebug() {
        return debugLifecycle;
    }

    public static void setDebug(boolean b) {
        debugLifecycle = b;
    }
}
```

```java
// INSTANCE对象初始化的时机并不是在单例类Singleton被加载的时候，而是在调用getInstance方法，使得静态内部类LazyHolder被加载的时候。因此这种实现方式是利用classloader的加载机制来实现懒加载，并保证构建单例的线程安全
public class Singleton {
    private static class LazyHolder {
        private static final Singleton INSTANCE = new Singleton();
    }
    private Singleton (){}
    public static Singleton getInstance() {
        return LazyHolder.INSTANCE;
    }
}
```

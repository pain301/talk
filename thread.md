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

同步容器的复合操作问题
同步容器类虽然对于单一操作是线程安全的，但是对于复合操作（即由多个操作组合而成，如迭代，跳转），就不一定能保证线程安全
```java
public class UnsafeVectorHelpers {
    // 需要额外的同步操作
    public static Object getLast(Vector list) {
        int lastIndex = list.size() - 1;
        return list.get(lastIndex);
    }
}
```
因为同步容器类没有解决复合操作的线程安全问题，所以在使用迭代器时，不能避免迭代器被修改。同步容器类的迭代器采用快速失败（fail-fast）的处理方法，即在容器迭代的过程中，发现容器被修改了，就抛出异常ConcurrentModificationException
除此之外，一些隐式调用迭代器的情况让同步容器的使用情况更为复杂

并发容器并不对整个容器上锁，故而允许多个线程同时访问容器，改进了同步容器因串行化而效率低的问题

在ConcurrentHashMap的实现中，其使用了16锁来分段保护容器，每个锁保护着散列表的1/16，其第N个散列桶的位置由第（N mod 16）个锁来保护。如果访问的元素不是由同一个锁来保护，则允许并发被访问。这样做虽然增加了维护和管理的开销，但是提高并发性。不过，ConcurrentHashMap中也存在对整个容器加锁的情况，比如容器要扩容，需要重新计算所有元素的散列值， 就需要获得全部的分段锁。

ConcurrentHashMap所提供的迭代器也不会抛出ConcurrentModificationException异常，所以不需要为其加锁。并发容器的迭代器具有弱一致性（Weakly Consistent）,容忍并发的修改，可以（但是不保证）将迭代器上的修改操作反映给容器。

需要注意的是，为了提高对元素访问的并发性，ConcurrentHashMap中对容器整体操作的语义被消弱，比如size和isEmpty等方法，其返回的结果都是估计值，可能是过期的

CopyOnWriteArrayList用于代替同步的List，其为“写时复制（Copy-on-Write）”容器，本质为事实不可变对象，一旦需要修改，就会创建一个新的容器副本并发布。容器的迭代器会保留一个指向底层基础数组的引用，这个数组是不变的，且其当前位置位于迭代器的起始位置。
由于每次修改CopyOnWriteArrayList都会有容器元素复制的开销，所以其更适合迭代操作远远多于修改操作的使用场景中

双端队列 Deque和BlockingDeque，即队列头尾都可以都可以插入和移除元素。双端队列适用于一种特殊的生产者-消费者模式——密取模式：即每个消费者都有一个双端队列，当自己队列中的元素被消费完之后，就可以秘密地从别的消费者队列的末端取出元素使用

闭锁一旦到达终止状态后，其状态就不会再被改变，CountDownLatch是闭锁的一种实现，其包括一个计数器，其被初始化为一个正整数，表示要等到事件数量。countDown方法表示一个事件已经放生了，await方法表示等到闭锁达到终止状态（拥塞方法，支持中断和超时）
```java
public class TestHarness {
    public long timeTasks(int nThreads, final Runnable task)
            throws InterruptedException {
        // 开始锁
        final CountDownLatch startGate = new CountDownLatch(1);
        // 结束锁
        final CountDownLatch endGate = new CountDownLatch(nThreads);

        for (int i = 0; i < nThreads; i++) {
            Thread t = new Thread() {
                public void run() {
                    try {
                        // 等待主线程初始化完毕
                        startGate.await();
                        try {
                            task.run();
                        } finally {
                            // 结束锁释放一个
                            endGate.countDown();
                        }
                    } catch (InterruptedException ignored) {
                    }
                }
            };
            t.start();
        }

        // 记录当前时间为开始时间
        long start = System.nanoTime();
        // 初始化完毕，开启开始锁，子线程可以运行
        startGate.countDown();
        // 等到个子线程运行完毕
        endGate.await();
        // 统计执行时间
        long end = System.nanoTime();
        return end - start;
    }
}
```

显式锁和同步代码块中的内置锁有着相同的互斥性和内存可见性。ReentrantLock是Lock的一种实现，提供对于线程的重入机制。和同步方法（Synchronized）相比，有着更强性能和灵活性
虽然同步方法的内置锁已经很强大和完备了，但是在功能上还有一定的局限性：不能实现非拥塞的锁操作。比如不能提供响应中断的获得锁操作，不能提供支持超时的获得锁操作等等。因此，在某些情况下需要使用更为灵活的加锁方式，也就是显式锁

使用tryLock方法可以用于实现轮询锁和定时锁
```java
public class DeadlockAvoidance {
    private static Random rnd = new Random();

    // 转账
    public boolean transferMoney(Account fromAcct, //转出账户
                                 Account toAcct, //转入账户
                                 DollarAmount amount, //金额
                                 long timeout, //超时时间
                                 TimeUnit unit) 
            throws InsufficientFundsException, InterruptedException {
        long fixedDelay = getFixedDelayComponentNanos(timeout, unit);
        long randMod = getRandomDelayModulusNanos(timeout, unit);
        long stopTime = System.nanoTime() + unit.toNanos(timeout);

        while (true) {
            // 尝试获得fromAcct的锁
            if (fromAcct.lock.tryLock()) {
                try {
                    // 尝试获得toAcct的锁
                    if (toAcct.lock.tryLock()) {
                        try {
                            if  (fromAcct.getBalance().compareTo(amount) < 0) //余额不足
                                throw new InsufficientFundsException();
                            else { // 余额满足，转账
                                fromAcct.debit(amount);
                                toAcct.credit(amount);
                                return true;
                            }
                        } finally { //释放toAcct锁
                            toAcct.lock.unlock();
                        }
                    }
                } finally { //释放fromAcct锁
                    fromAcct.lock.unlock();
                }
            }
            // 获得锁失败
            // 判断是否超时 如果超时则立刻失败
            if (System.nanoTime() < stopTime)
                return false;

            // 如果没有超时，随机睡眠一段时间
            NANOSECONDS.sleep(fixedDelay + rnd.nextLong() % randMod);
        }
    }


    class Account {
        //显示锁
        public Lock lock;

        void debit(DollarAmount d) {
        }

        void credit(DollarAmount d) {
        }

        DollarAmount getBalance() {
            return null;
        }
    }

    class InsufficientFundsException extends Exception {
    }
}
```
ReentrantLock的构造函数中提供两种锁的类型：

公平锁：线程将按照它们请求锁的顺序来获得锁；
非公平锁：允许插队，如果一个线程请求非公平锁的那个时刻，锁的状态正好为可用，则该线程将跳过所有等待中的线程获得该锁。
非公平锁在线程间竞争锁资源激烈的情况下，性能更高，这是由于：在恢复一个被挂起线程与该线程真正开始运行之间，存在着一个很严重的延迟，这是由于线程间上下文切换带来的。正是这个延迟，造成了公平锁在使用中出现CPU空闲。非公平锁正是将这个延迟带来的时间差利用起来，优先让正在运行的线程获得锁，避免线程的上下文切换。

如果每个线程获得锁的时间都很长，或者请求锁的竞争很稀疏或不频繁，则公平锁更为适合。

内置锁和显式锁都是默认使用非公平锁，但是显式锁可以设置公平锁，内置锁无法做到

无论是内置锁还是显式锁，都是一种独占锁，也是悲观锁


synchronized可以用于修饰类的实例方法、静态方法和代码块
synchronized实例方法保护的是当前实例对象，即this，this对象有一个锁和一个等待队列，锁只能被一个线程持有，其他试图获得同样锁的线程需要等待

执行synchronized实例方法的过程
尝试获得锁，如果能够获得锁，继续下一步，否则加入等待队列，阻塞并等待唤醒
执行实例方法体代码
释放锁，如果等待队列上有等待的线程，从中取一个并唤醒，如果有多个等待的线程，唤醒不保证公平性

当前线程不能获得锁的时候，它会加入等待队列等待，线程的状态会变为 BLOCKED

静态方法保护的是类对象，每个对象都有一个锁和一个等待队列，类对象也不例外

synchronized还可以用于包装代码块
synchronized括号里面的就是保护的对象

synchronized同步的对象可以是任意对象，任意对象都有一个锁和等待队列

理解synchronized
介绍了synchronized的基本用法和原理，我们再从下面几个角度来进一步理解一下synchronized：

可重入性
内存可见性
死锁

synchronized 是可重入的
可重入是通过记录锁的持有线程和持有数量来实现的

内存可见性，在释放锁时，所有写入都会写回内存，而获得锁后，都会从内存中读最新数据
加了volatile之后，Java会在操作对应变量时插入特殊的指令，保证读写到内存最新值，而非缓存的值

同步容器是通过给所有容器方法都加上synchronized来实现安全的

加了synchronized不是就绝对安全
复合操作，比如先检查再更新
伪同步，同步错对象
迭代，在遍历的同时容器发生了结构性变化，就会抛出该异常，同步容器并没有解决这个问题

同步容器的性能比较低的，当并发访问量比较大的时候性能很差

并发容器
CopyOnWriteArrayList
ConcurrentHashMap
ConcurrentLinkedQueue
ConcurrentSkipListSet



生产者/消费者协作
同时开始
等待结束
异步结果
集合点

wait/notify
public final void wait() throws InterruptedException
public final native void wait(long timeout) throws InterruptedException;
一个带时间参数，单位是毫秒，表示最多等待时间，参数为0表示无限期等待
一个不带时间参数，表示无限期等待，实际就是调用wait(0)
在等待期间都可以被中断，如果被中断，会抛出InterruptedException

除了用于锁的等待队列，每个对象还有另一个等待队列，表示条件队列，该队列用于线程间的协作。调用wait就会把当前线程放到条件队列上并阻塞

public final native void notify();
public final native void notifyAll();

notify 从条件队列中选一个线程，将其从队列中移除并唤醒，notifyAll 会移除条件队列中所有的线程并全部唤醒

wait/notify方法只能在synchronized代码块内被调用，如果调用wait/notify方法时，当前线程没有持有对象锁，会抛出异常java.lang.IllegalMonitorStateException

wait的具体过程
把当前线程放入条件等待队列，释放对象锁，阻塞等待，线程状态变为WAITING或TIMED_WAITING
等待时间到或被其他线程调用notify/notifyAll从条件队列中移除，这时，要重新竞争对象锁
如果能够获得锁，线程状态变为RUNNABLE，并从wait调用中返回
否则，该线程加入对象锁等待队列，线程状态变为BLOCKED，只有在获得锁后才会从wait调用中返回

线程从wait调用中返回后，不代表其等待的条件就一定成立了，它需要重新检查其等待的条件

调用notify会把在条件队列中等待的线程唤醒并从队列中移除，但它不会释放对象锁，只有在包含notify的synchronzied代码块执行完后，等待的线程才会从wait调用中返回


生产者/消费者模式
put和take都调用了wait，但它们的目的是不同的，或者说，它们等待的条件是不一样的，put等待的是队列不为满，而take等待的是队列不为空，但它们都会加入相同的条件等待队列。由于条件不同但又使用相同的等待队列，所以要调用notifyAll而不能调用notify，因为notify只能唤醒一个线程，如果唤醒的是同类线程就起不到协调的作用。

只能有一个条件等待队列，这是Java wait/notify机制的局限性，这使得对于等待条件的分析变得复杂，后续章节我们会介绍显式的锁和条件，它可以解决该问题

接口BlockingQueue和BlockingDeque
基于数组的实现类ArrayBlockingQueue
基于链表的实现类LinkedBlockingQueue和LinkedBlockingDeque
基于堆的实现类PriorityBlockingQueue



同时开始
static class FireFlag {
    private volatile boolean fired = false;

    public synchronized void waitForFire() throws InterruptedException {
        while (!fired) {
            wait();
        }
    }

    public synchronized void fire() {
        this.fired = true;
        notifyAll();
    }
}
子线程应该调用waitForFire()等待枪响，而主线程应该调用fire()发射比赛开始信号。
static class Racer extends Thread {
    FireFlag fireFlag;

    public Racer(FireFlag fireFlag) {
        this.fireFlag = fireFlag;
    }

    @Override
    public void run() {
        try {
            this.fireFlag.waitForFire();
            System.out.println("start run "
                    + Thread.currentThread().getName());
        } catch (InterruptedException e) {
        }
    }
}
public static void main(String[] args) throws InterruptedException {
    int num = 10;
    FireFlag fireFlag = new FireFlag();
    Thread[] racers = new Thread[num];
    for (int i = 0; i < num; i++) {
        racers[i] = new Racer(fireFlag);
        racers[i].start();
    }
    Thread.sleep(1000);
    fireFlag.fire();
}
等待结束
join方法让主线程等待子线程结束，join实际上就是调用了wait
while (isAlive()) {
    wait(0);
}
只要线程是活着的，isAlive()返回true，join就一直等待。当线程运行结束的时候，Java系统调用notifyAll来通知

使用协作对象
主线程与各个子线程协作的共享变量是一个数，这个数表示未完成的线程个数，初始值为子线程个数，主线程等待该值变为0，而每个子线程结束后都将该值减一，当减为0时调用notifyAll
public class MyLatch {
    private int count;

    public MyLatch(int count) {
        this.count = count;
    }

    public synchronized void await() throws InterruptedException {
        while (count > 0) {
            wait();
        }
    }

    public synchronized void countDown() {
        count--;
        if (count <= 0) {
            notifyAll();
        }
    }
}

异步结果
public interface Callable<V> {
    V call() throws Exception;
}
public interface MyFuture <V> {
    V get() throws Exception ;
}
public static void main(String[] args) {
    MyExecutor executor = new MyExecutor();
    // 子任务
    Callable<Integer> subTask = new Callable<Integer>() {

        @Override
        public Integer call() throws Exception {
            // ... 执行异步任务
            int millis = (int) (Math.random() * 1000);
            Thread.sleep(millis);
            return millis;
        }
    };
    // 异步调用，返回一个MyFuture对象
    MyFuture<Integer> future = executor.execute(subTask);
    // ... 执行其他操作
    try {
        // 获取异步调用的结果
        Integer result = future.get();
        System.out.println(result);
    } catch (Exception e) {
        e.printStackTrace();
    }
}

static class ExecuteThread<V> extends Thread {
    private V result = null;
    private Exception exception = null;
    private boolean done = false;
    private Callable<V> task;
    private Object lock;
    
    public ExecuteThread(Callable<V> task, Object lock) {
        this.task = task;
        this.lock = lock;
    }

    @Override
    public void run() {
        try {
            result = task.call();
        } catch (Exception e) {
            exception = e;
        } finally {
            synchronized (lock) {
                done = true;
                lock.notifyAll();
            }
        }
    }

    public V getResult() {
        return result;
    }

    public boolean isDone() {
        return done;
    }

    public Exception getException() {
        return exception;
    }
}

这个子线程执行实际的子任务，记录执行结果到result变量、异常到exception变量，执行结束后设置共享状态变量done为true并调用notifyAll以唤醒可能在等待结果的主线程。

MyExecutor的execute的方法的代码为：

public <V> MyFuture<V> execute(final Callable<V> task) {
    final Object lock = new Object();
    final ExecuteThread<V> thread = new ExecuteThread<>(task, lock);
    thread.start();

    MyFuture<V> future = new MyFuture<V>() {
        @Override
        public V get() throws Exception {
            synchronized (lock) {
                while (!thread.isDone()) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                    }
                }
                if (thread.getException() != null) {
                    throw thread.getException();
                }
                return thread.getResult();
            }
        }
    };
    return future;
}

集合点
public class AssemblePoint {
    private int n;

    public AssemblePoint(int n) {
        this.n = n;
    }

    public synchronized void await() throws InterruptedException {
        if (n > 0) {
            n--;
            if (n == 0) {
                notifyAll();
            } else {
                while (n != 0) {
                    wait();
                }
            }
        }
    }
}

取消/关闭的机制
停止一个线程的主要机制是中断，中断并不是强迫终止一个线程，它是一种协作机制，是给线程传递一个取消信号，但是由线程来决定如何以及何时退出
public boolean isInterrupted()
public void interrupt()
public static boolean interrupted() 

每个线程都有一个标志位，表示该线程是否被中断
isInterrupted：就是返回对应线程的中断标志位是否为true
interrupted：返回当前线程的中断标志位是否为true，但它还有一个重要的副作用，就是清空中断标志位
interrupt：表示中断对应的线程

线程的状态
RUNNABLE：线程在运行或具备运行条件只是在等待操作系统调度
WAITING/TIMED_WAITING：线程在等待某个条件或超时
BLOCKED：线程在等待锁，试图进入同步块
NEW/TERMINATED：线程还未启动或已结束


RUNNABLE
如果线程在运行中，且没有执行IO操作，interrupt()只是会设置线程的中断标志位，没有任何其它作用。线程应该在运行过程中合适的位置检查中断标志位，比如说，如果主体代码是一个循环，可以在循环开始处进行检查，如下所示
public class InterruptRunnableDemo extends Thread {
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            // ... 单次循环代码
        }
        System.out.println("done ");
    }

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new InterruptRunnableDemo();
        thread.start();
        Thread.sleep(1000);
        thread.interrupt();
    }
}


WAITING/TIMED_WAITING
线程执行如下方法会进入WAITING状态
public final void join() throws InterruptedException
public final void wait() throws InterruptedException

执行如下方法会进入TIMED_WAITING状态：
public final native void wait(long timeout) throws InterruptedException;
public static native void sleep(long millis) throws InterruptedException;
public final synchronized void join(long millis) throws InterruptedException

在这些状态时，对线程对象调用interrupt()会使得该线程抛出InterruptedException，抛出异常后，中断标志位会被清空，而不是被设置

InterruptedException是一个受检异常，线程必须进行处理

捕获到InterruptedException，通常表示希望结束该线程，线程大概有两种处理方式：
向上传递该异常，这使得该方法也变成了一个可中断的方法，需要调用者进行处理
有些情况，不能向上传递异常，比如Thread的run方法，它的声明是固定的，不能抛出任何受检异常，这时，应该捕获异常，进行合适的清理操作，清理后，一般应该调用Thread的interrupt方法设置中断标志位，使得其他代码有办法知道它发生了中断

public class InterruptWaitingDemo extends Thread {
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // 模拟任务代码
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // ... 清理操作
                // 重设中断标志位
                Thread.currentThread().interrupt();
            }
        }
        System.out.println(isInterrupted());
    }

    public static void main(String[] args) {
        InterruptWaitingDemo thread = new InterruptWaitingDemo();
        thread.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        thread.interrupt();
    }
}


BLOCKED
如果线程在等待锁，对线程对象调用interrupt()只是会设置线程的中断标志位，线程依然会处于BLOCKED状态，也就是说，interrupt()并不能使一个在等待锁的线程真正"中断"

public class InterruptSynchronizedDemo {
    private static Object lock = new Object();

    private static class A extends Thread {
        @Override
        public void run() {
            synchronized (lock) {
                while (!Thread.currentThread().isInterrupted()) {
                }
            }
            System.out.println("exit");
        }
    }

    public static void test() throws InterruptedException {
        synchronized (lock) {
            A a = new A();
            a.start();
            Thread.sleep(1000);

            a.interrupt();
            a.join();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        test();
    }
}

test方法在持有锁lock的情况下启动线程a，而线程a也去尝试获得锁lock，所以会进入锁等待队列，随后test调用线程a的interrupt方法并等待线程线程a结束，线程a不会结束，interrupt方法只会设置线程的中断标志，而并不会使它从锁等待队列中出来

public static void test() throws InterruptedException {
    synchronized (lock) {
        A a = new A();
        a.start();
        Thread.sleep(1000);

        a.interrupt();
    }
}

在使用synchronized关键字获取锁的过程中不响应中断请求，这是synchronized的局限性。如果这对程序是一个问题，应该使用显式锁

NEW/TERMINATE
如果线程尚未启动(NEW)，或者已经结束(TERMINATED)，则调用interrupt()对它没有任何效果，中断标志位也不会被设置

IO操作
如果线程在等待IO操作，尤其是网络IO，则会有一些特殊的处理

如果IO通道是可中断的，即实现了InterruptibleChannel接口，则线程的中断标志位会被设置，同时，线程会收到异常ClosedByInterruptException
如果线程阻塞于Selector调用，则线程的中断标志位会被设置，同时，阻塞的调用会立即返回

InputStream的read调用，该操作是不可中断的，如果流中没有数据，read会阻塞 (但线程状态依然是RUNNABLE)，且不响应interrupt()，与synchronized类似，调用interrupt()只会设置线程的中断标志，而不会真正中断

public class InterruptReadDemo {
    private static class A extends Thread {
        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try {
                    System.out.println(System.in.read());
                } catch (IOException e) {
                    e.printStackTrace();
                }    
            }
            System.out.println("exit");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        A t = new A();
        t.start();
        Thread.sleep(100);

        t.interrupt();
    }
}


线程t启动后调用System.in.read()从标准输入读入一个字符，不要输入任何字符，我们会看到，调用interrupt()不会中断read()，线程会一直运行。
不过，有一个办法可以中断read()调用，那就是调用流的close方法，我们将代码改为：

public class InterruptReadDemo {
    private static class A extends Thread {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    System.out.println(System.in.read());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("exit");
        }

        public void cancel() {
            try {
                System.in.close();
            } catch (IOException e) {
            }
            interrupt();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        A t = new A();
        t.start();
        Thread.sleep(100);

        t.cancel();
    }
}


我们给线程定义了一个cancel方法，在该方法中，调用了流的close方法，同时调用了interrupt方法，调用close方法后，read方法会返回，返回值为-1，表示流结束

对于以线程提供服务的程序模块而言，它应该封装取消/关闭操作，提供单独的取消/关闭方法给调用者，类似于InterruptReadDemo中演示的cancel方法，外部调用者应该调用这些方法而不是直接调用interrupt

AtomicBoolean：原子Boolean类型
AtomicInteger：原子Integer类型
AtomicLong：原子Long类型
AtomicReference：原子引用类型

针对Integer, Long和Reference类型，还有对应的数组类型：
AtomicIntegerArray
AtomicLongArray
AtomicReferenceArray

为了便于以原子方式更新对象中的字段，还有如下的类：
AtomicIntegerFieldUpdater
AtomicLongFieldUpdater
AtomicReferenceFieldUpdater

AtomicReference还有两个类似的类，在某些情况下更为易用：
AtomicMarkableReference
AtomicStampedReference

public final boolean compareAndSet(int expect, int update)
以原子方式实现了如下功能：如果当前值等于expect，则更新为update，否则不更新

synchronized是悲观的，它假定更新很可能冲突，所以先获取锁，得到锁后才更新。原子变量的更新逻辑是乐观的，它假定冲突比较少，但使用CAS更新，也就是进行冲突检测，如果确实冲突了，那也没关系，继续尝试就好了

synchronized代表一种阻塞式算法，得不到锁的时候，进入锁等待队列，等待其他线程唤醒，有上下文切换开销。原子变量的更新逻辑是非阻塞式的，更新冲突的时候，它就重试，不会阻塞，不会有上下文切换开销

public final boolean compareAndSet(int expect, int update) {
    return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
}
private static final Unsafe unsafe = Unsafe.getUnsafe();

实现锁
public class MyLock {
    private AtomicInteger status = new AtomicInteger(0);

    public void lock() {
        while (!status.compareAndSet(0, 1)) {
            Thread.yield();
        }
    }

    public void unlock() {
        status.compareAndSet(1, 0);
    }
}

AtomicBoolean可以用来在程序中表示一个标志位
AtomicReference用来以原子方式更新复杂类型，它有一个类型参数，使用时需要指定引用的类型

原子数组方便以原子的方式更新数组中的每个元素，我们以AtomicIntegerArray为例来简要介绍下

FieldUpdater方便以原子方式更新对象中的字段，字段不需要声明为原子变量，FieldUpdater是基于反射机制实现


ABA 使用AtomicStampedReference，在修改值的同时附加一个时间戳，只有值和时间戳都相同才进行修改

AtomicStampedReference在compareAndSet中要同时修改两个值，一个是引用，另一个是时间戳，内部AtomicStampedReference会将两个值组合为一个对象，修改的是一个值
public boolean compareAndSet(V   expectedReference,
                             V   newReference,
                             int expectedStamp,
                             int newStamp) {
    Pair<V> current = pair;
    return
        expectedReference == current.reference &&
        expectedStamp == current.stamp &&
        ((newReference == current.reference &&
          newStamp == current.stamp) ||
         casPair(current, Pair.of(newReference, newStamp)));
}


这个Pair是AtomicStampedReference的一个内部类，成员包括引用和时间戳，具体定义为：

private static class Pair<T> {
    final T reference;
    final int stamp;
    private Pair(T reference, int stamp) {
        this.reference = reference;
        this.stamp = stamp;
    }
    static <T> Pair<T> of(T reference, int stamp) {
        return new Pair<T>(reference, stamp);
    }
}
AtomicStampedReference将对引用值和时间戳的组合比较和修改转换为了对这个内部类Pair单个值的比较和修改。
AtomicMarkableReference是另一个AtomicReference的增强类，与AtomicStampedReference类似，它也是给引用关联了一个字段，只是这次是一个boolean类型的标志位，只有引用值和标志位都相同的情况下才进行修改。

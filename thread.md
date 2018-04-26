public static void park()
public static void parkNanos(long nanos)
public static void parkUntil(long deadline)
public static void unpark(Thread thread)
park使得当前线程放弃CPU，进入等待状态(WAITING)，操作系统不再对它进行调度，直到有其他线程对它调用了unpark，unpark需要指定一个线程，unpark会使之恢复可运行状态
public static void main(String[] args) throws InterruptedException {
    Thread t = new Thread (){
        public void run(){
            LockSupport.park();
            System.out.println("exit");
        }
    };
    t.start();    
    Thread.sleep(1000);
    LockSupport.unpark(t);
}
park不同于Thread.yield()，yield只是告诉操作系统可以先让其他线程运行，但自己依然是可运行状态，而park会放弃调度资格，使线程进入WAITING状态
需要说明的是，park是响应中断的，当有中断发生时，park会返回，线程的中断状态会被设置。另外，还需要说明一下，park可能会无缘无故的返回，程序应该重新检查park等待的条件是否满足
park有两个变体：
parkNanos：可以指定等待的最长时间，参数是相对于当前时间的纳秒数。
parkUntil：可以指定最长等到什么时候，参数是绝对时间，是相对于纪元时的毫秒数。
当等待超时的时候，它们也会返回。
这些park方法还有一些变体，可以指定一个对象，表示是由于该对象进行等待的，以便于调试，通常传递的值是this，这些方法有：
public static void park(Object blocker)
public static void parkNanos(Object blocker, long nanos)
public static void parkUntil(Object blocker, long deadline)

LockSupport有一个方法，可以返回一个线程的blocker对象：
public static Object getBlocker(Thread t)

与CAS方法一样，它们也调用了Unsafe类中的对应方法，Unsafe类最终调用了操作系统的API，从程序员的角度，我们可以认为LockSupport中的这些方法就是基本操作。

CompletionService也可以提交异步任务，它可以按任务完成顺序获取结果
public interface CompletionService<V> {
    Future<V> submit(Callable<V> task);
    Future<V> submit(Runnable task, V result);
    Future<V> take() throws InterruptedException;
    Future<V> poll();
    Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException;
}
take和poll方法，它们都是获取下一个完成任务的结果，take()会阻塞等待，poll()会立即返回，如果没有已完成的任务，返回null，带时间参数的poll方法会最多等待限定的时间

CompletionService的主要实现类是ExecutorCompletionService，它依赖于一个Executor完成实际的任务提交，而自己主要负责结果的排队和处理，它的构造方法有两个：
public ExecutorCompletionService(Executor executor)
public ExecutorCompletionService(Executor executor, BlockingQueue<Future<V>> completionQueue)
可以提供一个BlockingQueue参数，用作完成任务的队列，没有提供的话，ExecutorCompletionService内部会创建一个LinkedBlockingQueue

ExecutorCompletionService有一个额外的队列，每个任务完成之后，都会将代表结果的Future入队
FutureTask，任务完成后，不管是正常完成、异常结束、还是被取消，都会调用finishCompletion方法，而该方法会调用一个done方法
protected void done() { }
它的实现为空，但它是一个protected方法，子类可以重写该方法
在ExecutorCompletionService中，提交的任务类型不是一般的FutureTask，而是一个子类QueueingFuture
public Future<V> submit(Callable<V> task) {
    if (task == null) throw new NullPointerException();
    RunnableFuture<V> f = newTaskFor(task);
    executor.execute(new QueueingFuture(f));
    return f;
}
该子类重写了done方法，在任务完成时将结果加入到完成队列中
private class QueueingFuture extends FutureTask<Void> {
    QueueingFuture(RunnableFuture<V> task) {
        super(task, null);
        this.task = task;
    }
    protected void done() { completionQueue.add(task); }
    private final Future<V> task;
}

ExecutorCompletionService的take/poll方法就是从该队列获取结果
public Future<V> take() throws InterruptedException {
    return completionQueue.take();
}

AbstractExecutorService的invokeAny的实现，就利用了ExecutorCompletionService，提交任务后，通过take方法获取结果，获取到第一个有效结果后，取消所有其他任务，不过，它的具体实现有一些优化，比较复杂


定时任务的应用场景：
闹钟程序或任务提醒
监控系统，每隔一段时间采集下系统数据，对异常事件报警
统计系统，一般凌晨一定时间统计昨日的各种数据指标


实现定时任务：
Timer和TimerTask
ScheduledExecutorService

TimerTask表示一个定时任务，它是一个抽象类，实现了Runnable，具体的定时任务需要继承该类，实现run方法
Timer是一个具体类，它负责定时任务的调度和执行，它有如下主要方法：
//在指定绝对时间time运行任务task
public void schedule(TimerTask task, Date time)
//在当前时间延时delay毫秒后运行任务task
public void schedule(TimerTask task, long delay)
//固定延时重复执行，第一次计划执行时间为firstTime，后一次的计划执行时间为前一次"实际"执行时间加上period
public void schedule(TimerTask task, Date firstTime, long period)
//同样是固定延时重复执行，第一次执行时间为当前时间加上delay
public void schedule(TimerTask task, long delay, long period)
//固定频率重复执行，第一次计划执行时间为firstTime，后一次的计划执行时间为前一次"计划"执行时间加上period
public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period)
//同样是固定频率重复执行，第一次计划执行时间为当前时间加上delay
public void scheduleAtFixedRate(TimerTask task, long delay, long period)


需要注意固定延时(fixed-delay)与固定频率(fixed-rate)的区别，都是重复执行，但后一次任务执行相对的时间是不一样的，对于固定延时，它是基于上次任务的"实际"执行时间来算的，如果由于某种原因，上次任务延时了，则本次任务也会延时，而固定频率会尽量补够运行次数

另外，需要注意的是，如果第一次计划执行的时间firstTime是一个过去的时间，则任务会立即运行，对于固定延时的任务，下次任务会基于第一次执行时间计算，而对于固定频率的任务，则会从firstTime开始算，有可能加上period后还是一个过去时间，从而连续运行很多次，直到时间超过当前时间

```java
public class TimerFixedRate {

    static class LongRunningTask extends TimerTask {
        @Override
        public void run() {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
            System.out.println("long running finished");
        }
    }

    static class FixedRateTask extends TimerTask {

        @Override
        public void run() {
            System.out.println(System.currentTimeMillis());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Timer timer = new Timer();

        timer.schedule(new LongRunningTask(), 10);
        timer.scheduleAtFixedRate(new FixedRateTask(), 100, 1000);
    }
}
```
运行该程序，第二个任务同样只有在第一个任务运行结束后才会运行，会把之前没有运行的次数补过来，一下子运行5次

Timer内部主要由两部分组成，任务队列和Timer线程。任务队列是一个基于堆实现的优先级队列，按照下次执行的时间排优先级。Timer线程负责执行所有的定时任务，需要强调的是，一个Timer对象只有一个Timer线程
Timer线程主体是一个循环，从队列中拿任务，如果队列中有任务且计划执行时间小于等于当前时间，就执行它，如果队列中没有任务或第一个任务延时还没到，就睡眠。如果睡眠过程中队列上添加了新任务且新任务是第一个任务，Timer线程会被唤醒，重新进行检查

在执行任务之前，Timer线程判断任务是否为周期任务，如果是，就设置下次执行的时间并添加到优先级队列中，对于固定延时的任务，下次执行时间为当前时间加上period，对于固定频率的任务，下次执行时间为上次计划执行时间加上period
下次任务的计划是在执行当前任务之前就做出了的，对于固定延时的任务，延时相对的是任务执行前的当前时间，而不是任务执行后
另一方面，对于固定频率的任务，它总是基于最先的计划计划的，所以，很有可能会出现前面例子中一下子执行很多次任务的情况

一个Timer对象只有一个Timer线程，这意味着，定时任务不能耗时太长，更不能是无限循环

关于Timer线程，在执行任何一个任务的run方法时，一旦run抛出异常，Timer线程就会退出，从而所有定时任务都会被取消
如果希望各个定时任务不互相干扰，一定要在run方法内捕获所有异常

背后只有一个线程在运行
固定频率的任务被延迟后，可能会立即执行多次，将次数补够
固定延时任务的延时相对的是任务执行前的时间
不要在定时任务中使用无限循环
一个定时任务的未处理异常会导致所有定时任务被取消

Java并发包引入了ScheduledExecutorService，它是一个接口
public interface ScheduledExecutorService extends ExecutorService {
    //单次执行，在指定延时delay后运行command
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);
    //单次执行，在指定延时delay后运行callable
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);
    //固定频率重复执行
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);
    //固定延时重复执行
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);
}

它们的返回类型都是ScheduledFuture，它是一个接口，扩展了Future和Delayed，没有定义额外方法。对于固定频率的任务，第一次执行时间为initialDelay后，第二次为initialDelay+period，第三次initialDelay+2*period，依次类推。不过，对于固定延时的任务，它是从任务执行后开始算的，第一次为initialDelay后，第二次为第一次任务执行结束后再加上delay

ScheduledExecutorService的主要实现类是ScheduledThreadPoolExecutor，它是线程池ThreadPoolExecutor的子类，是基于线程池实现的，它的主要构造方法是：
public ScheduledThreadPoolExecutor(int corePoolSize) 
还有构造方法可以接受参数ThreadFactory和RejectedExecutionHandler
它的任务队列是一个无界的优先级队列，所以最大线程数对它没有作用，即使corePoolSize设为0，它也会至少运行一个线程

工厂类Executors也提供了一些方便的方法，以方便创建ScheduledThreadPoolExecutor
//单线程的定时任务执行服务
public static ScheduledExecutorService newSingleThreadScheduledExecutor()
public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) 
//多线程的定时任务执行服务
public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize)
public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory)

由于可以有多个线程执行定时任务，一般任务就不会被某个长时间运行的任务所延迟了
另外，与Timer不同，单个定时任务的异常不会再导致整个定时任务被取消了，即使背后只有一个线程执行任务
ScheduledThreadPoolExecutor的实现思路与Timer基本是类似的，都有一个基于堆的优先级队列，保存待执行的定时任务，它的主要不同是：
它的背后是线程池，可以有多个线程执行任务
它在任务执行后再设置下次执行的时间，对于固定延时的任务更为合理
任务执行线程会捕获任务执行过程中的所有异常，一个定时任务的异常不会影响其他定时任务，但发生异常的任务也不再被重新调度，即使它是一个重复任务


读写锁ReentrantReadWriteLock
信号量Semaphore
倒计时门栓CountDownLatch
循环栅栏CyclicBarrier
都是基于AQS实现的，在一些特定的同步协作场景中，相比使用最基本的wait/notify，显示锁/条件，它们更为方便，效率更高

可重入读写锁ReentrantReadWriteLock。
多个线程的读操作完全可以并行，在读多写少的场景中，让读操作并行可以明显提高性能
public interface ReadWriteLock {
    Lock readLock();
    Lock writeLock();
}
通过一个ReadWriteLock产生两个锁，一个读锁，一个写锁。读操作使用读锁，写操作使用写锁

只有一个线程可以进行写操作，在获取写锁时，只有没有任何线程持有任何锁才可以获取到，在持有写锁时，其他任何线程都获取不到任何锁。在没有其他线程持有写锁的情况下，多个线程可以获取和持有读锁

ReentrantReadWriteLock是可重入的读写锁
public ReentrantLock()
public ReentrantLock(boolean fair)

内部使用同一个整数变量表示锁的状态，16位给读锁用，16位给写锁用，使用一个变量便于进行CAS操作，锁的等待队列其实也只有一个
写锁的获取，就是确保当前没有其他线程持有任何锁，否则就等待。写锁释放后，也就是将等待队列中的第一个线程唤醒，唤醒的可能是等待读锁的，也可能是等待写锁的
读锁的获取不太一样，首先，只要写锁没有被持有，就可以获取到读锁，此外，在获取到读锁后，它会检查等待队列，逐个唤醒最前面的等待读锁的线程，直到第一个等待写锁的线程。如果有其他线程持有写锁，获取读锁会等待。读锁释放后，检查读锁和写锁数是否都变为了0，如果是，唤醒等待队列中的下一个线程

信号量Semaphore可以限制对资源的并发访问数
public Semaphore(int permits)
public Semaphore(int permits, boolean fair)
permits表示许可数量
Semaphore的方法与锁是类似的，主要的方法有两类，获取许可和释放许可
//阻塞获取许可
public void acquire() throws InterruptedException
//阻塞获取许可，不响应中断
public void acquireUninterruptibly()
//批量获取多个许可
public void acquire(int permits) throws InterruptedException
public void acquireUninterruptibly(int permits)
//尝试获取
public boolean tryAcquire()
//限定等待时间获取
public boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException
//释放许可
public void release()

如果我们将permits的值设为1，你可能会认为它就变成了一般的锁，不过，它与一般的锁是不同的。一般锁只能由持有锁的线程释放，而Semaphore表示的只是一个许可数，任意线程都可以调用其release方法。主要的锁实现类ReentrantLock是可重入的，而Semaphore不是，每一次的acquire调用都会消耗一个许可
信号量也是基于AQS实现的，permits表示共享的锁个数，acquire方法就是检查锁个数是否大于0，大于则减一，获取成功，否则就等待，release就是将锁个数加一，唤醒第一个等待的线程

倒计时门栓CountDownLatch
相当于是一个门栓，一开始是关闭的，所有希望通过该门的线程都需要等待，然后开始倒计时，倒计时变为0后，门栓打开，等待的所有线程都可以通过，它是一次性的，打开后就不能再关上了

CountDownLatch里有一个计数
public CountDownLatch(int count)
多个线程可以基于这个计数进行协作
public void await() throws InterruptedException
public boolean await(long timeout, TimeUnit unit) throws InterruptedException
public void countDown() 

await()检查计数是否为0，如果大于0，就等待，await()可以被中断，也可以设置最长等待时间。countDown检查计数，如果已经为0，直接返回，否则减少计数，如果新的计数变为0，则唤醒所有等待的线程

门栓的两种应用场景，一种是同时开始，另一种是主从协作
同时开始，计数初始为1，运动员线程调用await，主线程调用countDown
```java
public class RacerWithCountDownLatch {
    static class Racer extends Thread {
        CountDownLatch latch;

        public Racer(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                this.latch.await();
                System.out.println(getName() 
                        + " start run "+System.currentTimeMillis());
            } catch (InterruptedException e) {
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int num = 10;
        CountDownLatch latch = new CountDownLatch(1);
        Thread[] racers = new Thread[num];
        for (int i = 0; i < num; i++) {
            racers[i] = new Racer(latch);
            racers[i].start();
        }
        Thread.sleep(1000);
        latch.countDown();
    }
}
```
主从协作模式中，主线程依赖工作线程的结果，需要等待工作线程结束，计数初始值为工作线程的个数，工作线程结束后调用countDown，主线程调用await进行等待
```java
public class MasterWorkerDemo {
    static class Worker extends Thread {
        CountDownLatch latch;

        public Worker(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                // simulate working on task
                Thread.sleep((int) (Math.random() * 1000));

                // simulate exception
                if (Math.random() < 0.02) {
                    throw new RuntimeException("bad luck");
                }
            } catch (InterruptedException e) {
            } finally {
                this.latch.countDown();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int workerNum = 100;
        CountDownLatch latch = new CountDownLatch(workerNum);
        Worker[] workers = new Worker[workerNum];
        for (int i = 0; i < workerNum; i++) {
            workers[i] = new Worker(latch);
            workers[i].start();
        }
        latch.await();
        System.out.println("collect worker results");
    }
}
```
countDown的调用应该放到finally语句中，确保在工作线程发生异常的情况下也会被调用，使主线程能够从await调用中返回

循环栅栏CyclicBarrier
相当于是一个栅栏，所有线程在到达该栅栏后都需要等待其他线程
CyclicBarrier特别适用于并行迭代计算，每个线程负责一部分计算，然后在栅栏处等待其他线程完成，所有线程到齐后，交换数据和计算结果，再进行下一次迭代
它有一个数字，表示的是参与的线程个数
public CyclicBarrier(int parties)

它还有一个构造方法，接受一个Runnable参数
public CyclicBarrier(int parties, Runnable barrierAction)

这个参数表示栅栏动作，当所有线程到达栅栏后，在所有线程执行下一步动作前，运行参数中的动作，这个动作由最后一个到达栅栏的线程执行
public int await() throws InterruptedException, BrokenBarrierException
public int await(long timeout, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException
await在等待其他线程到达栅栏，调用await后，表示自己已经到达，如果自己是最后一个到达的，就执行可选的命令，执行后，唤醒所有等待的线程，然后重置内部的同步计数，以循环使用

await可以被中断，可以限定最长等待时间，中断或超时后会抛出异常。需要说明的是异常BrokenBarrierException，它表示栅栏被破坏了，在CyclicBarrier中，参与的线程是互相影响的，只要其中一个线程在调用await时被中断了，或者超时了，栅栏就会被破坏，此外，如果栅栏动作抛出了异常，栅栏也会被破坏，被破坏后，所有在调用await的线程就会退出，抛出BrokenBarrierException

```java
public class CyclicBarrierDemo {
    static class Tourist extends Thread {
        CyclicBarrier barrier;

        public Tourist(CyclicBarrier barrier) {
            this.barrier = barrier;
        }

        @Override
        public void run() {
            try {
                // 模拟先各自独立运行
                Thread.sleep((int) (Math.random() * 1000));

                // 集合点A
                barrier.await();

                System.out.println(this.getName() + " arrived A "
                        + System.currentTimeMillis());

                // 集合后模拟再各自独立运行
                Thread.sleep((int) (Math.random() * 1000));

                // 集合点B
                barrier.await();
                System.out.println(this.getName() + " arrived B "
                        + System.currentTimeMillis());
            } catch (InterruptedException e) {
            } catch (BrokenBarrierException e) {
            }
        }
    }

    public static void main(String[] args) {
        int num = 3;
        Tourist[] threads = new Tourist[num];
        CyclicBarrier barrier = new CyclicBarrier(num, new Runnable() {

            @Override
            public void run() {
                System.out.println("all arrived " + System.currentTimeMillis()
                        + " executed by " + Thread.currentThread().getName());
            }
        });
        for (int i = 0; i < num; i++) {
            threads[i] = new Tourist(barrier);
            threads[i].start();
        }
    }
}
```
CountDownLatch的参与线程是有不同角色的，有的负责倒计时，有的在等待倒计时变为0，负责倒计时和等待倒计时的线程都可以有多个，它用于不同角色线程间的同步
CyclicBarrier的参与线程角色是一样的，用于同一角色线程间的协调一致
CountDownLatch是一次性的，而CyclicBarrier是可以重复利用的


每个对象都有一把锁和两个等待队列，一个是锁等待队列，放的是等待获取锁的线程，另一个是条件等待队列，放的是等待条件的线程，wait将自己加入条件等待队列，notify从条件等待队列上移除一个线程并唤醒，notifyAll移除所有线程并唤醒。
显式条件与显式锁配合使用，与wait/notify相比，可以支持多个条件队列，代码更为易读，效率更高，使用时注意不要将signal/signalAll误写为notify/notifyAll。
而并发容器是专为并发而设计的，线程安全、并发度更高、性能更高、迭代不会抛出ConcurrentModificationException、很多容器以原子方式支持一些复合操作。

反射不一样，它是在运行时，而非编译时，动态获取类型的信息，比如接口信息、成员信息、方法信息、构造方法信息等，根据这些动态获取到的信息创建对象、访问/修改成员、调用方法等

获取Class对象
每个已加载的类在内存都有一份类信息，每个对象都有指向它所属类信息的引用。Java中，类信息对应的类就是java.lang.Class，所有类的根父类Object有一个方法，可以获取对象的Class对象：
public final native Class<?> getClass()
Class是一个泛型类，有一个类型参数，getClass()并不知道具体的类型，所以返回Class<?>。
获取Class对象不一定需要实例对象，如果在写程序时就知道类名，可以使用<类名>.class获取Class对象，比如：
Class<Date> cls = Date.class;
接口也有Class对象，且这种方式对于接口也是适用的
Class<Comparable> cls = Comparable.class;

基本类型没有getClass方法，但也都有对应的Class对象，类型参数为对应的包装类型
Class<Integer> intCls = int.class;
Class<Byte> byteCls = byte.class;
Class<Character> charCls = char.class;
Class<Double> doubleCls = double.class;
void作为特殊的返回类型，也有对应的Class：
Class<Void> voidCls = void.class;

对于数组，每种类型都有对应数组类型的Class对象，每个维度都有一个，即一维数组有一个，二维数组有一个不同的
String[] strArr = new String[10];
int[][] twoDimArr = new int[3][2];
int[] oneDimArr = new int[10];
Class<? extends String[]> strArrCls = strArr.getClass();
Class<? extends int[][]> twoDimArrCls = twoDimArr.getClass();
Class<? extends int[]> oneDimArrCls = oneDimArr.getClass();

枚举类型也有对应的Class
enum Size {
    SMALL, MEDIUM, BIG
}
Class<Size> cls = Size.class;
Class有一个静态方法forName，可以根据类名直接加载Class，获取Class对象
forName可能抛出异常ClassNotFoundException。

Class有如下方法，可以获取与名称有关的信息：
public String getName()
public String getSimpleName()
public String getCanonicalName()
public Package getPackage()
getSimpleName不带包信息，getName返回的是Java内部使用的真正的名字，getCanonicalName返回的名字更为友好，getPackage返回的是包信息

需要说明的是数组类型的getName返回值，它使用前缀[表示数组，有几个[表示是几维数组，数组的类型用一个字符表示，I表示int，L表示类或接口，其他类型与字符的对应关系为: boolean(Z), byte(B), char(C), double(D), float(F), long(J), short(S)

类中定义的静态和实例变量都被称为字段，用类Field表示
//返回所有的public字段，包括其父类的，如果没有字段，返回空数组
public Field[] getFields() 
//返回本类声明的所有字段，包括非public的，但不包括父类的
public Field[] getDeclaredFields() 
//返回本类或父类中指定名称的public字段，找不到抛出异常NoSuchFieldException
public Field getField(String name)
//返回本类中声明的指定名称的字段，找不到抛出异常NoSuchFieldException
public Field getDeclaredField(String name)

通过Field访问和操作指定对象中该字段的值
//获取字段的名称
public String getName() 
//判断当前程序是否有该字段的访问权限
public boolean isAccessible()
//flag设为true表示忽略Java的访问检查机制，以允许读写非public的字段
public void setAccessible(boolean flag)
//获取指定对象obj中该字段的值
public Object get(Object obj)
//将指定对象obj中该字段的值设为value
public void set(Object obj, Object value)

在get/set方法中，对于静态变量，obj被忽略，可以为null，如果字段值为基本类型，get/set会自动在基本类型与对应的包装类型间进行转换，对于private字段，直接调用get/set会抛出非法访问异常IllegalAccessException，应该先调用setAccessible(true)以关闭Java的检查机制。

//返回字段的修饰符
public int getModifiers()
//返回字段的类型
public Class<?> getType()
//以基本类型操作字段
public void setBoolean(Object obj, boolean z)
public boolean getBoolean(Object obj)
public void setDouble(Object obj, double d)
public double getDouble(Object obj)

//查询字段的注解信息
public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
public Annotation[] getDeclaredAnnotations()

getModifiers返回的是一个int，可以通过Modifier类的静态方法进行解读
public static final int MAX_NAME_LEN = 255;
可以这样查看该字段的修饰符：
Field f = Student.class.getField("MAX_NAME_LEN");
int mod = f.getModifiers();
System.out.println(Modifier.toString(mod));
System.out.println("isPublic: " + Modifier.isPublic(mod));
System.out.println("isStatic: " + Modifier.isStatic(mod));
System.out.println("isFinal: " + Modifier.isFinal(mod));
System.out.println("isVolatile: " + Modifier.isVolatile(mod));

方法信息
类中定义的静态和实例方法都被称为方法，用类Method表示
//返回所有的public方法，包括其父类的，如果没有方法，返回空数组
public Method[] getMethods()
//返回本类声明的所有方法，包括非public的，但不包括父类的
public Method[] getDeclaredMethods() 
//返回本类或父类中指定名称和参数类型的public方法，找不到抛出异常NoSuchMethodException
public Method getMethod(String name, Class<?>... parameterTypes)
//返回本类中声明的指定名称和参数类型的方法，找不到抛出异常NoSuchMethodException
public Method getDeclaredMethod(String name, Class<?>... parameterTypes)

可以获取方法的信息，也可以通过Method调用对象的方法
//获取方法的名称
public String getName() 
//flag设为true表示忽略Java的访问检查机制，以允许调用非public的方法
public void setAccessible(boolean flag)
//在指定对象obj上调用Method代表的方法，传递的参数列表为args
public Object invoke(Object obj, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException

对invoke方法，如果Method为静态方法，obj被忽略，可以为null，args可以为null，也可以为一个空的数组，方法调用的返回值被包装为Object返回，如果实际方法调用抛出异常，异常被包装为InvocationTargetException重新抛出，可以通过getCause方法得到原异常

Method还有很多方法，可以获取方法的修饰符、参数、返回值、注解等信息，比如：
//获取方法的修饰符，返回值可通过Modifier类进行解读
public int getModifiers()
//获取方法的参数类型
public Class<?>[] getParameterTypes()
//获取方法的返回值类型
public Class<?> getReturnType()
//获取方法声明抛出的异常类型
public Class<?>[] getExceptionTypes()
//获取注解信息
public Annotation[] getDeclaredAnnotations()
public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
//获取方法参数的注解信息
public Annotation[][] getParameterAnnotations() 

创建对象和构造方法
public T newInstance() throws InstantiationException, IllegalAccessException
调用类的默认构造方法(即无参public构造方法)，如果类没有该构造方法，会抛出异常InstantiationException

//获取所有的public构造方法，返回值可能为长度为0的空数组
public Constructor<?>[] getConstructors() 
//获取所有的构造方法，包括非public的
public Constructor<?>[] getDeclaredConstructors() 
//获取指定参数类型的public构造方法，没找到抛出异常NoSuchMethodException
public Constructor<T> getConstructor(Class<?>... parameterTypes)
//获取指定参数类型的构造方法，包括非public的，没找到抛出异常NoSuchMethodException
public Constructor<T> getDeclaredConstructor(Class<?>... parameterTypes) 

类Constructor表示构造方法，通过它可以创建对象
public T newInstance(Object ... initargs) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException

获取关于构造方法的很多信息
//获取参数的类型信息
public Class<?>[] getParameterTypes()
//构造方法的修饰符，返回值可通过Modifier类进行解读
public int getModifiers()
//构造方法的注解信息
public Annotation[] getDeclaredAnnotations() 
public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
//构造方法中参数的注解信息
public Annotation[][] getParameterAnnotations() 

类型检查和转换
instanceof关键字，它可以用来判断变量指向的实际对象类型，instanceof后面的类型是在代码中确定的，如果要检查的类型是动态的，可以使用Class类的如下方法：
public native boolean isInstance(Object obj)

if(list instanceof ArrayList){
    System.out.println("array list");
}

Class cls = Class.forName("java.util.ArrayList");
if(cls.isInstance(list)){
    System.out.println("array list");
}

进行强制类型转换
List list = ..
if(list instanceof ArrayList){
    ArrayList arrList = (ArrayList)list;
}

强制转换到的类型是在写代码时就知道的，如果是动态的，可以使用Class的如下方法：
public T cast(Object obj)

public static <T> T toType(Object obj, Class<T> cls){
    return cls.cast(obj);
}

判断Class之间的关系
// 检查参数类型cls能否赋给当前Class类型的变量
public native boolean isAssignableFrom(Class<?> cls);

Class代表的类型既可以是普通的类、也可以是内部类，还可以是基本类型、数组等
//是否是数组
public native boolean isArray();  
//是否是基本类型
public native boolean isPrimitive();
//是否是接口
public native boolean isInterface();
//是否是枚举
public boolean isEnum() 
//是否是注解
public boolean isAnnotation()
//是否是匿名内部类
public boolean isAnonymousClass()
//是否是成员类
public boolean isMemberClass() 
//是否是本地类
public boolean isLocalClass() 

Class还有很多方法，可以获取类的声明信息，如修饰符、父类、实现的接口、注解等
//获取修饰符，返回值可通过Modifier类进行解读
public native int getModifiers()
//获取父类，如果为Object，父类为null
public native Class<? super T> getSuperclass()
//对于类，为自己声明实现的所有接口，对于接口，为直接扩展的接口，不包括通过父类间接继承来的
public native Class<?>[] getInterfaces(); 
//自己声明的注解
public Annotation[] getDeclaredAnnotations()
//所有的注解，包括继承得到的
public Annotation[] getAnnotations()
//获取或检查指定类型的注解，包括继承得到的
public <A extends Annotation> A getAnnotation(Class<A> annotationClass)
public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass)

关于内部类，Class有一些专门的方法
//获取所有的public的内部类和接口，包括从父类继承得到的
public Class<?>[] getClasses()
//获取自己声明的所有的内部类和接口
public Class<?>[] getDeclaredClasses() 
//如果当前Class为内部类，获取声明该类的最外部的Class对象
public Class<?> getDeclaringClass() 
//如果当前Class为内部类，获取直接包含该类的类
public Class<?> getEnclosingClass()
//如果当前Class为本地类或匿名内部类，返回包含它的方法
public Method getEnclosingMethod()

类的加载
Class有两个静态方法，可以根据类名加载类：
public static Class<?> forName(String className)
public static Class<?> forName(String name, boolean initialize, ClassLoader loader)

ClassLoader表示类加载器，后面章节我们会进一步介绍，initialize表示加载后，是否执行类的初始化代码(如static语句块)。第一个方法中没有传这些参数，相当于调用：
Class.forName(className, true, currentLoader)
currentLoader表示加载当前类的ClassLoader 

根据原始类型的字符串构造Class对象
public static Class<?> forName(String className) throws ClassNotFoundException{
    if("int".equals(className)){
        return int.class;
    }
    //其他基本类型...
    return Class.forName(className);
}

对于数组类型，有一个专门的方法，可以获取它的元素类型
public native Class<?> getComponentType()
String[] arr = new String[]{};
System.out.println(arr.getClass().getComponentType());

java.lang.reflect包中有一个针对数组的专门的类Array（注意不是java.util中的Arrays)，提供了对于数组的一些反射支持，以便于统一处理多种类型的数组，主要方法有：

//创建指定元素类型、指定长度的数组，
public static Object newInstance(Class<?> componentType, int length)
//创建多维数组
public static Object newInstance(Class<?> componentType, int... dimensions)
//获取数组array指定的索引位置index处的值
public static native Object get(Object array, int index)
//修改数组array指定的索引位置index处的值为value
public static native void set(Object array, int index, Object value)
//返回数组的长度
public static native int getLength(Object array)

需要注意的是，在Array类中，数组是用Object而非Object[]表示的，这是为了方便处理多种类型的数组，int[]，String[]都不能与Object[]相互转换，但可以与Object相互转换
int[] intArr = (int[])Array.newInstance(int.class, 10);
String[] strArr = (String[])Array.newInstance(String.class, 10);

除了以Object类型操作数组元素外，Array也支持以各种基本类型操作数组元素
public static native double getDouble(Object array, int index)
public static native void setDouble(Object array, int index, double d)
public static native void setLong(Object array, int index, long l)
public static native long getLong(Object array, int index)

枚举类型也有一个专门方法，可以获取所有的枚举常量：
public T[] getEnumConstants()

Class有如下方法，可以获取类的泛型参数信息：
public TypeVariable<Class<T>>[] getTypeParameters()
Field有如下方法：
public Type getGenericType()

Method有如下方法：
public Type getGenericReturnType()
public Type[] getGenericParameterTypes()
public Type[] getGenericExceptionTypes()

Constructor有如下方法：
public Type[] getGenericParameterTypes() 
Type是一个接口，Class实现了Type，Type的其他子接口还有：
TypeVariable：类型参数，可以有上界，比如：T extends Number
ParameterizedType：参数化的类型，有原始类型和具体的类型参数，比如：List<String> 
WildcardType：通配符类型，比如：?, ? extends Number, ? super Integer

反射虽然是灵活的，但一般情况下，并不是我们优先建议的，主要原因是：
反射更容易出现运行时错误，使用显式的类和接口，编译器能帮我们做类型检查，减少错误，但使用反射，类型是运行时才知道的，编译器无能为力
反射的性能要低一些，在访问字段、调用方法前，反射先要查找对应的Field/Method，性能要慢一些
如果能用接口实现同样的灵活性，就不要使用反射

内置注解
@Override修饰一个方法，表示该方法不是当前类首先声明的，而是在某个父类或实现的接口中声明的，当前类"重写"了该方法
@Deprecated可以修饰的范围很广，包括类、方法、字段、参数等，它表示对应的代码已经过时了
@SuppressWarnings表示压制Java的编译警告，它有一个必填参数，表示压制哪种类型的警告

现代Java开发经常利用某种框架管理对象的生命周期及其依赖关系，这个框架一般称为DI(Dependency Injection)容器，DI是指依赖注入，流行的框架有Spring、Guice等，在使用这些框架时，程序员一般不通过new创建对象，而是由容器管理对象的创建，对于依赖的服务，也不需要自己管理，而是使用注解表达依赖关系

@Override的定义
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Override {
}

@Target表示注解的目标，@Override的目标是方法(ElementType.METHOD)，ElementType是一个枚举，其他可选值有：

TYPE：表示类、接口（包括注解），或者枚举声明
FIELD：字段，包括枚举常量
METHOD：方法
PARAMETER：方法中的参数
CONSTRUCTOR：构造方法
LOCAL_VARIABLE：本地变量
ANNOTATION_TYPE：注解类型
PACKAGE：包

目标可以有多个，用{}表示，比如@SuppressWarnings的@Target就有多个
@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE})
@Retention(RetentionPolicy.SOURCE)
public @interface SuppressWarnings {
    String[] value();
}

如果没有声明@Target，默认为适用于所有类型
@Retention是一个枚举，有三个取值：
SOURCE：只在源代码中保留，编译器将代码编译为字节码文件后就会丢掉
CLASS：保留到字节码文件中，但Java虚拟机将class文件加载到内存时不一定会在内存中保留
RUNTIME：一直保留到运行时
如果没有声明@Retention，默认为CLASS。
@Override和@SuppressWarnings都是给编译器用的，所以@Retention都是RetentionPolicy.SOURCE

//获取所有的注解
public Annotation[] getAnnotations()
//获取所有本元素上直接声明的注解，忽略inherited来的
public Annotation[] getDeclaredAnnotations()
//获取指定类型的注解，没有返回null
public <A extends Annotation> A getAnnotation(Class<A> annotationClass) 
//判断是否有指定类型的注解
public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass)

动态代理可以在运行时动态创建一个类，实现一个或多个接口，在不修改原有类的基础上动态为通过该类获取的对象添加方法、修改行为

动态代理有两种实现方式，一种是Java SDK提供的，另外一种是第三方库如cglib提供的
Java SDK动态代理
静态代理中，代理类是直接定义在代码中的，在动态代理中，代理类是动态生成的
Proxy.getProxyClass需要两个参数，一个是ClassLoader，另一个是接口数组，它会动态生成一个类，类名以$Proxy开头，后跟一个数字
$Proxy0的定义，可以配置java的一个属性得到
java -Dsun.misc.ProxyGenerator.saveGeneratedFiles=true shuo.laoma.dynamic.c86.SimpleJDKDynamicProxyDemo
以上命令会把动态生成的代理类$Proxy0保存到文件$Proxy0.class中，通过JD-GUI反编译器得到源码

Java SDK动态代理的局限在于，它只能为接口创建代理，返回的代理对象也只能转换到某个接口类型
cglib 通过继承实现的，动态创建了一个类，但这个类的父类是被代理的类，代理类重写了父类的所有public非final方法，改为调用Callback中的相关方法

类加载器ClassLoader就是加载其他类的类，它负责将字节码文件加载到内存，创建Class对象

通过创建自定义的ClassLoader，可以实现一些强大灵活的功能
热部署，在不重启Java程序的情况下，动态替换类的实现
应用的模块化和相互隔离，不同的ClassLoader可以加载相同的类但互相隔离、互不影响
从不同地方灵活加载，通过自定义的ClassLoader，可以从共享的Web服务器、数据库、缓存服务器等其他地方加载字节码文件

每个Class对象都有一个方法，可以获取实际加载它的ClassLoader
public ClassLoader getClassLoader()

ClassLoader有一个方法，可以获取它的父ClassLoader
public final ClassLoader getParent()

ClassLoader有一个静态方法，可以获取默认的系统类加载器
public static ClassLoader getSystemClassLoader()

ClassLoader中有一个主要方法，用于加载类
public Class<?> loadClass(String name) throws ClassNotFoundException

由于委派机制，Class的getClassLoader()方法返回的不一定是调用loadClass的ClassLoader

ClassLoader vs Class.forName
public static Class<?> forName(String className)
public static Class<?> forName(String name, boolean initialize, ClassLoader loader)
第一个方法使用系统类加载器加载
第二个指定ClassLoader，参数initialize表示，加载后，是否执行类的初始化代码(如static语句块)，没有指定默认为true
ClassLoader的loadClass不会执行类的初始化代码

使用自己的逻辑寻找class文件字节码，找到后使用defineClass转换为Class对象
protected final Class<?> defineClass(String name, byte[] b, int off, int len)

一个复杂的程序，内部可能按模块组织，不同模块可能使用同一个类，但使用的是不同版本，如果使用同一个类加载器，它们是无法共存的，不同模块使用不同的类加载器就可以实现隔离
使用同一个ClassLoader，类只会被加载一次，加载后，即使class文件已经变了，再次加载，得到的也还是原来的Class对象，创建一个新的ClassLoader再加载Class，得到的Class对象就是新的，从而实现动态更新

使用文件的最后修改时间来跟踪文件是否发生了变化，当文件修改后，调用reloadHelloService()来重新加载

CopyOnWriteArrayList实现了List接口，它的用法与其他List如ArrayList基本是一样的，它的区别是
它是线程安全的，可以被多个线程并发访问
它的迭代器不支持修改操作，但也不会抛出ConcurrentModificationException
它以原子方式支持一些复合操作

基于synchronized的同步容器的几个问题。迭代时，需要对整个列表对象加锁，否则会抛出ConcurrentModificationException，CopyOnWriteArrayList没有这个问题，迭代时不需要加锁

CopyOnWriteArrayList的迭代器不支持修改操作，也不支持一些依赖迭代器修改方法的操作，比如Collections的sort方法
 
基于synchronized的同步容器的另一个问题是复合操作，比如先检查再更新，也需要调用方加锁，而CopyOnWriteArrayList直接支持两个原子方法

CopyOnWriteArrayList的内部也是一个数组，但这个数组是以原子方式被整体更新的。每次修改操作，都会新建一个数组，复制原数组的内容到新数组，在新数组上进行需要的修改，然后以原子方式设置内部的数组引用，这就是写时拷贝。

所有的读操作，都是先拿到当前引用的数组，然后直接访问该数组，在读的过程中，可能内部的数组引用已经被修改了，但不会影响读操作，它依旧访问原数组内容。

在CopyOnWriteArrayList中，读不需要锁，可以并行，读和写也可以并行，但多个线程不能同时写，每个写操作都需要先获取锁，CopyOnWriteArrayList内部使用ReentrantLock
transient final ReentrantLock lock = new ReentrantLock();

```java
public boolean add(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        Object[] newElements = Arrays.copyOf(elements, len + 1);
        newElements[len] = e;
        setArray(newElements);
        return true;
    } finally {
        lock.unlock();
    }
}
```

CopyOnWriteArrayList不适用于数组很大，且修改频繁的场景。它是以优化读操作为目标的，读不需要同步，性能很高，但在优化读的同时就牺牲了写的性能

CopyOnWriteArraySet实现了Set接口，不包含重复元素，使用比较简单，内部通过CopyOnWriteArrayList实现的

由于CopyOnWriteArraySet是基于CopyOnWriteArrayList实现的，与之前介绍过的Set的实现类如HashSet/TreeSet相比，性能比较低，不适用于元素个数特别多的集合。如果元素个数比较多，可以考虑ConcurrentHashMap或ConcurrentSkipListSet

ConcurrentHashMap与HashMap类似，适用于不要求排序的场景，ConcurrentSkipListSet与TreeSet类似，适用于要求排序的场景。Java并发包中没有与HashSet对应的并发容器，但可以很容易的基于ConcurrentHashMap构建一个，利用Collections.newSetFromMap方法即可

ConcurrentHashMap
并发安全
直接支持一些原子复合操作
支持高并发、读操作完全并行、写操作支持一定程度的并行

与同步容器Collections.synchronizedMap相比，迭代不用加锁，不会抛出ConcurrentModificationException

同步容器有几个问题：
每个方法都需要同步，支持的并发度比较低
对于迭代和复合操作，需要调用方加锁

除了Map接口，ConcurrentHashMap还实现了一个接口ConcurrentMap，接口定义了一些条件更新操作
```java
public interface ConcurrentMap<K, V> extends Map<K, V> {
    //条件更新，如果Map中没有key，设置key为value，

    //返回原来key对应的值，如果没有，返回null

    V putIfAbsent(K key, V value);
    //条件删除，如果Map中有key，且对应的值为value，

    //则删除，如果删除了，返回true，否则false
    boolean remove(Object key, Object value);
    //条件替换，如果Map中有key，且对应的值为oldValue，

    //则替换为newValue，如果替换了，返回ture，否则false
    boolean replace(K key, V oldValue, V newValue);
    //条件替换，如果Map中有key，则替换值为value，

    //返回原来key对应的值，如果原来没有，返回null
    V replace(K key, V value);
}
```
如果使用同步容器，调用方必须加锁，而ConcurrentMap将它们实现为了原子操作

高并发
分段锁
读不需要锁
同步容器使用synchronized，所有方法，竞争同一个锁，而ConcurrentHashMap采用分段锁技术，将数据分为多个段，而每个段有一个独立的锁，每一个段相当于一个独立的哈希表，分段的依据也是哈希值，无论是保存键值对还是根据键查找，都先根据键的哈希值映射到段，再在段对应的哈希表上进行操作

采用分段锁，可以大大提高并发度，多个段之间可以并行读写。默认情况下，段是16个，不过，这个数字可以通过构造方法进行设置
public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel)
concurrencyLevel表示估计的并行更新的线程个数，ConcurrentHashMap会将该数转换为2的整数次幂，比如14转换为16，25转换为32

在迭代过程中，如果另一个线程对容器进行了修改，迭代会继续，不会抛出异常

弱一致性
ConcurrentHashMap的迭代器创建后，就会按照哈希表结构遍历每个元素，但在遍历过程中，内部元素可能会发生变化，如果变化发生在已遍历过的部分，迭代器就不会反映出来，而如果变化发生在未遍历过的部分，迭代器就会发现并反映出来，这就是弱一致性

ConcurrentSkipListSet也是基于ConcurrentSkipListMap实现的
ConcurrentSkipListMap是基于SkipList实现的，SkipList称为跳跃表或跳表，是一种数据结构，并发版本采用跳表而不是树是因为跳表更易于实现高效并发算法

ConcurrentSkipListMap
没有使用锁，所有操作都是无阻塞的，所有操作都可以并行，包括写，多个线程可以同时写
与ConcurrentHashMap类似，迭代器不会抛出ConcurrentModificationException，是弱一致的，迭代可能反映最新修改也可能不反映，一些方法如putAll, clear不是原子的
与ConcurrentHashMap类似，同样实现了ConcurrentMap接口，直接支持一些原子复合操作
与TreeMap一样，可排序，默认按键自然有序，可以传递比较器自定义排序，实现了SortedMap和NavigableMap接口

需要说明一下的是它的size方法，与大多数容器实现不同，这个方法不是常量操作，它需要遍历所有元素，复杂度为O(N)，而且遍历结束后，元素个数可能已经变了，一般而言，在并发应用中，这个方法用处不大

基本实现原理
跳表是基于链表的，在链表的基础上加了多层索引结构

3, 6, 7, 9, 12, 17, 19, 21, 25, 26
对Map来说，这些值可以视为键。ConcurrentSkipListMap会构造跳表结构
最下面一层，就是最基本的单向链表，这个链表是有序的
为了快速查找，跳表有多层索引结构，高层的索引节点一定同时是低层的索引节点

查找元素总是从最高层开始，将待查值与下一个索引节点的值进行比较，如果大于索引节点，就向右移动，继续比较，如果小于，则向下移动到下一层进行比较
查找的性能与二叉树类似，复杂度是O(log(N))

对于索引更新，随机计算一个数，表示为该元素最高建几层索引，一层的概率为1/2，二层为1/4，三层为1/8，依次类推。然后从最高层到最低层，在每一层，为该元素建立索引节点，建的过程也是先查找位置，再插入。

对于删除元素，ConcurrentSkipListMap不是一下子真的进行删除，为了避免并发冲突，有一个复杂的标记过程，在内部遍历元素的过程中会真正删除

对于常见的操作，如get/put/remove/containsKey，ConcurrentSkipListMap的复杂度都是O(log(N))

如果不需要并发，可以使用另一种更为高效的结构，数据和所有层的索引放到一个节点中
对于一个元素，只有一个节点，只是每个节点的索引个数可能不同，在新建一个节点时，使用随机算法决定它的索引个数，平均而言，1/2的元素有两个索引，1/4的元素有三个索引


无锁非阻塞并发队列：ConcurrentLinkedQueue和ConcurrentLinkedDeque
普通阻塞队列：基于数组的ArrayBlockingQueue，基于链表的LinkedBlockingQueue和LinkedBlockingDeque

优先级阻塞队列：PriorityBlockingQueue
延时阻塞队列：DelayQueue
其他阻塞队列：SynchronousQueue和LinkedTransferQueue

这些队列迭代都不会抛出ConcurrentModificationException，都是弱一致的

有两个无锁非阻塞队列：ConcurrentLinkedQueue和ConcurrentLinkedDeque，它们适用于多个线程并发使用一个队列的场合，都是基于链表实现的，都没有限制大小，是无界的，与ConcurrentSkipListMap类似，它们的size方法不是一个常量运算，不过这个方法在并发应用中用处也不大

ConcurrentLinkedQueue实现了Queue接口，表示一个先进先出的队列，从尾部入队，从头部出队，内部是一个单向链表。ConcurrentLinkedDeque实现了Deque接口，表示一个双端队列，在两端都可以入队和出队，内部是一个双向链表

普通阻塞队列
//入队，如果队列满，等待直到队列有空间
void put(E e) throws InterruptedException;
//出队，如果队列空，等待直到队列不为空，返回头部元素
E take() throws InterruptedException;
//入队，如果队列满，最多等待指定的时间，如果超时还是满，返回false
boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException;
//出队，如果队列空，最多等待指定的时间，如果超时还是空，返回null
E poll(long timeout, TimeUnit unit) throws InterruptedException;

ArrayBlockingQueue和LinkedBlockingQueue都是实现了Queue接口，表示先进先出的队列，尾部进，头部出，而LinkedBlockingDeque实现了Deque接口，是一个双端队列

ArrayBlockingQueue是基于循环数组实现的，有界，创建时需要指定大小，且在运行过程中不会改变，这与我们在容器类中介绍的ArrayDeque是不同的，ArrayDeque也是基于循环数组实现的，但是是无界的，会自动扩展。

LinkedBlockingQueue是基于单向链表实现的，在创建时可以指定最大长度，也可以不指定，默认是无限的，节点都是动态创建的。LinkedBlockingDeque与LinkedBlockingQueue一样，最大长度也是在创建时可选的，默认无限，不过，它是基于双向链表实现的
内部，它们都是使用显式锁ReentrantLock和显式条件Condition实现的

普通阻塞队列是先进先出的，而优先级队列是按优先级出队的，优先级高的先出，我们在容器类中介绍过优先级队列PriorityQueue及其背后的数据结构堆

PriorityBlockingQueue是PriorityQueue的并发版本，与PriorityQueue一样，它没有大小限制，是无界的，内部的数组大小会动态扩展，要求元素要么实现Comparable接口，要么创建PriorityBlockingQueue时提供一个Comparator对象
与PriorityQueue的区别是，PriorityBlockingQueue实现了BlockingQueue接口，在队列为空时，take方法会阻塞等待
另外，PriorityBlockingQueue是线程安全的，它的基本实现原理与PriorityQueue是一样的，也是基于堆，但它使用了一个锁ReentrantLock保护所有访问，使用了一个条件协调阻塞等待。
延时阻塞队列DelayQueue是一种特殊的优先级队列，它也是无界的，它要求每个元素都实现Delayed接口
Delayed扩展了Comparable接口，也就是说，DelayQueue的每个元素都是可比较的，它有一个额外方法getDelay返回一个给定时间单位unit的整数，表示再延迟多长时间，如果小于等于0，表示不再延迟
DelayQueue也是优先级队列，它按元素的延时时间出队，它的特殊之处在于，只有当元素的延时过期之后才能被从队列中拿走，也就是说，take方法总是返回第一个过期的元素，如果没有，则阻塞等待

内部，DelayQueue是基于PriorityQueue实现的，它使用一个锁ReentrantLock保护所有访问，使用一个条件available表示头部是否有元素，当头部元素的延时未到时，take操作会根据延时计算需睡眠的时间，然后睡眠，如果在此过程中有新的元素入队，且成为头部元素，则阻塞睡眠的线程会被提前唤醒然后重新检查

SynchronousQueue与一般的队列不同，它不算一种真正的队列，它没有存储元素的空间，存储一个元素的空间都没有。它的入队操作要等待另一个线程的出队操作，反之亦然。如果没有其他线程在等待从队列中接收元素，put操作就会等待。take操作需要等待其他线程往队列中放元素，如果没有，也会等待。SynchronousQueue适用于两个线程之间直接传递信息、事件或任务

LinkedTransferQueue实现了TransferQueue接口，TransferQueue是BlockingQueue的子接口，但增加了一些额外功能，生产者在往队列中放元素时，可以等待消费者接收后再返回，适用于一些消息传递类型的应用中。TransferQueue的接口定义为：

public interface TransferQueue<E> extends BlockingQueue<E> {
    //如果有消费者在等待(执行take或限时的poll)，直接转给消费者，
    //返回true，否则返回false，不入队
    boolean tryTransfer(E e);
    //如果有消费者在等待，直接转给消费者，
    //否则入队，阻塞等待直到被消费者接收后再返回
    void transfer(E e) throws InterruptedException;
    //如果有消费者在等待，直接转给消费者，返回true
    //否则入队，阻塞等待限定的时间，如果最后被消费者接收，返回true
    boolean tryTransfer(E e, long timeout, TimeUnit unit)
        throws InterruptedException;
    //是否有消费者在等待
    boolean hasWaitingConsumer();
    //等待的消费者个数
    int getWaitingConsumerCount();
}

LinkedTransferQueue是基于链表实现的、无界的TransferQueue

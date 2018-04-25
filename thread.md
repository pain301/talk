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


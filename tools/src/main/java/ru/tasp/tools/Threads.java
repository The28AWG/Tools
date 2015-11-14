package ru.tasp.tools;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.LinkedHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import ru.tasp.tools.logger.L;

/**
 * Created by the28awg on 26.10.15.
 */
public class Threads {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE = 1;
    private Handler uiHandler;
    private Thread uiThread;
    private UncaughtExceptionHandler exceptionHandler;
    private ThreadFactory factory;
    private RejectedExecutionHandler handler;
    private ThreadPoolExecutor executor = null;
    private BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(128);

    private Threads() {
        uiHandler = new Handler(Looper.getMainLooper());
        uiThread = Looper.getMainLooper().getThread();
        exceptionHandler = new UncaughtExceptionHandler();
        factory = new ThreadFactory().wrapRunnable(true).pattern("Background", true).
                daemon(false).exceptionHandler(exceptionHandler).finishConfig();
        handler = new RejectedExecutionHandler();
        executor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
                TimeUnit.SECONDS, queue, factory, handler);
    }

    public static Threads get() {
        return Holder.THREADS;
    }

    public UncaughtExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    public ThreadFactory getFactory() {
        return factory;
    }

    public RejectedExecutionHandler getHandler() {
        return handler;
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    public BlockingQueue<Runnable> getQueue() {
        return queue;
    }

    public void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != uiThread) {
            uiHandler.post(action);
        } else {
            action.run();
        }
    }

    public static class ThreadFactory implements java.util.concurrent.ThreadFactory {
        protected final AccessControlContext acc;
        protected final AtomicLong counter = new AtomicLong();
        private long stackSize;
        private String pattern;
        private ClassLoader ccl;
        private ThreadGroup group;
        private Thread.UncaughtExceptionHandler exceptionHandler;
        private boolean daemon;
        private boolean configured;
        private boolean wrapRunnable;

        public ThreadFactory() {
            final Thread t = Thread.currentThread();
            ClassLoader loader;
            AccessControlContext acc = null;
            try {
                loader = t.getContextClassLoader();
                if (System.getSecurityManager() != null) {
                    acc = AccessController.getContext();
                    acc.checkPermission(new RuntimePermission("setContextClassLoader"));
                }
            } catch (SecurityException _skip) {
                //no permission
                loader = null;
                acc = null;
            }

            this.ccl = loader;
            this.acc = acc;
            this.daemon = true;//Executors have it false by default

            this.wrapRunnable = true;//by default wrap if acc is present (+SecurityManager)

            //default pattern - caller className
            StackTraceElement[] stack = new Exception().getStackTrace();
            pattern(stack.length > 1 ? getOuterClassName(stack[1].getClassName()) : "ThreadFactory", true);
        }

        private static String getOuterClassName(String className) {
            int idx = className.lastIndexOf('.') + 1;
            className = className.substring(idx);//remove package
            idx = className.indexOf('$');
            if (idx <= 0) {
                return className;//handle classes starting w/ $
            }
            return className.substring(0, idx);//assume inner class

        }

        public ThreadFactory finishConfig() {
            configured = true;
            counter.addAndGet(0);//write fence "w/o" volatile
            return this;
        }

        public long getCreatedThreadsCount() {
            return counter.get();
        }

        protected void assertConfigurable() {
            if (configured)
                throw new IllegalStateException("already configured");
        }

        @Override
        public Thread newThread(Runnable r) {
            configured = true;
            final Thread t = new Thread(group, wrapRunnable(r), composeName(r), stackSize);
            t.setDaemon(daemon);
            t.setUncaughtExceptionHandler(exceptionHandler);//securityException only if in the main group, shall be safe here
            //funny moment Thread.getUncaughtExceptionHandler() has a race.. badz (can throw NPE)

            applyCCL(t);
            return t;
        }

        private void applyCCL(final Thread t) {
            if (ccl != null) {//use factory creator ACC for setContextClassLoader
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        t.setContextClassLoader(ccl);
                        return null;
                    }
                }, acc);
            }
        }

        private Runnable wrapRunnable(final Runnable r) {
            if (acc == null || !wrapRunnable) {
                return r;
            }
            Runnable result = new Runnable() {
                public void run() {
                    AccessController.doPrivileged(new PrivilegedAction<Object>() {
                        @Override
                        public Object run() {
                            android.os.Process.setThreadPriority(Process.myTid(), Process.THREAD_PRIORITY_BACKGROUND);
                            r.run();
                            return null;
                        }
                    }, acc);
                }
            };
            return result;
        }


        protected String composeName(Runnable r) {
            return String.format(pattern, counter.incrementAndGet(), System.currentTimeMillis());
        }


        //standard setters allowing chaining, feel free to add normal setXXX
        public ThreadFactory pattern(String patten, boolean appendFormat) {
            assertConfigurable();
            if (appendFormat) {
                patten += ": %d @ %tF %<tT";//counter + creation time
            }
            this.pattern = patten;
            return this;
        }


        public ThreadFactory daemon(boolean daemon) {
            assertConfigurable();
            this.daemon = daemon;
            return this;
        }

        public ThreadFactory stackSize(long stackSize) {
            assertConfigurable();
            this.stackSize = stackSize;
            return this;
        }


        public ThreadFactory threadGroup(ThreadGroup group) {
            assertConfigurable();
            this.group = group;
            return this;
        }

        public ThreadFactory exceptionHandler(Thread.UncaughtExceptionHandler exceptionHandler) {
            assertConfigurable();
            this.exceptionHandler = exceptionHandler;
            return this;
        }

        public ThreadFactory wrapRunnable(boolean wrapRunnable) {
            assertConfigurable();
            this.wrapRunnable = wrapRunnable;
            return this;
        }

        public ThreadFactory ccl(ClassLoader ccl) {
            assertConfigurable();
            this.ccl = ccl;
            return this;
        }
    }

    public static class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        private LinkedHashMap<String, LinkedBlockingQueue<Throwable>> throwable = new LinkedHashMap<>();

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            L.error(ex, ex.getMessage());
            if (!throwable.containsKey(thread.toString())) {
                throwable.put(thread.toString(), new LinkedBlockingQueue<Throwable>());
            }
            throwable.get(thread.toString()).add(ex);
        }

        public LinkedHashMap<String, LinkedBlockingQueue<Throwable>> getThrowable() {
            return throwable;
        }
    }

    public static class RejectedExecutionHandler implements java.util.concurrent.RejectedExecutionHandler {
        private AtomicLong counter = new AtomicLong(0);

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            counter.incrementAndGet();
        }

        public long getCounter() {
            return counter.get();
        }
    }

    private static class Holder {
        private static final Threads THREADS = new Threads();
    }

    public static class Task<V> extends FutureTask<V> {
        private Handler handler;

        public Task(Callable<V> callable) {
            super(callable);
            try {
                handler = new Handler(Looper.myLooper());
            } catch (NullPointerException e) {
                handler = new Handler(Looper.getMainLooper());
            }
        }

        public Task(Callable<V> callable, Looper looper) {
            super(callable);
            handler = new Handler(looper);
        }

        @Override
        protected void done() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        done(get());
                    } catch (InterruptedException e) {
                        L.error(e, "Ooops!");
                    } catch (ExecutionException e) {
                        L.error(e, "Ooops!");
                    }
                }
            });
        }

        public void done(V result) {}
    }
}

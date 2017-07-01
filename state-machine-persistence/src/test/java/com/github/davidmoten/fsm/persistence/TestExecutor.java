package com.github.davidmoten.fsm.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class TestExecutor implements ScheduledExecutorService {

    long time = 0;
    private AtomicReference<List<ScheduledRunnable>> list = new AtomicReference<>(Collections.emptyList());
    private AtomicInteger wip = new AtomicInteger();

    public void advance(long duration, TimeUnit unit) {
        time += unit.toMillis(duration);
        drain();
    }

    public void drain() {
        if (wip.getAndIncrement() == 0) {
            int missed = 1;
            while (true) {
                while (true) {
                    List<ScheduledRunnable> x = list.get();
                    List<ScheduledRunnable> remove = new ArrayList<>();
                    for (ScheduledRunnable r : x) {
                        if (r.time <= time) {
                            remove.add(r);
                        }
                    }
                    List<ScheduledRunnable> y = new ArrayList<>(x);
                    y.removeAll(remove);
                    if (list.compareAndSet(x, y)) {
                        for (ScheduledRunnable s : remove) {
                            s.runnable.run();
                        }
                        break;
                    }
                }
                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    return;
                }
            }
        }
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTerminated() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<?> submit(Runnable task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void execute(Runnable command) {
        command.run();
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        ScheduledRunnable s = new ScheduledRunnable(command, time + unit.toMillis(delay));
        while (true) {
            List<ScheduledRunnable> x = list.get();
            List<ScheduledRunnable> y = new ArrayList<ScheduledRunnable>(x);
            y.add(s);
            if (list.compareAndSet(x, y)) {
                break;
            }
        }
        return null;
    }

    private static final class ScheduledRunnable {
        final Runnable runnable;
        final long time;

        ScheduledRunnable(Runnable runnable, long time) {
            this.runnable = runnable;
            this.time = time;
        }
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

}

package com.github.terminatornl.tiquality.profiling.standalone;

import com.github.terminatornl.tiquality.Tiquality;
import com.github.terminatornl.tiquality.api.TiqualityException;
import com.github.terminatornl.tiquality.interfaces.Tracker;
import com.github.terminatornl.tiquality.profiling.*;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SA_SilentProfiler implements Runnable {

    private final Tracker tracker;
    private final long durationInMs;
    private long startTimeNanos;
    private ProfilingKey key;
    private ProfilePrinter printer;
    private boolean isProfiling;

    public SA_SilentProfiler(Tracker tracker, long durationInMs, ProfilePrinter printer) {
        this.tracker = tracker;
        this.durationInMs = durationInMs;
        this.printer = printer;
    }

    public boolean isProfiling() {
        return isProfiling;
    }

    public static SortedSet<AnalyzedComponent> analyzeComponents(TickLogger logger, ProfilePrinter printer) throws InterruptedException {
        TreeMap<ReferencedTickable.ReferenceId, TickTime> times = logger.getTimes();
        Iterator<Map.Entry<ReferencedTickable.ReferenceId, TickTime>> referenceIterator = times.entrySet().iterator();
        SortedSet<AnalyzedComponent> finishedAnalyzers = Collections.synchronizedSortedSet(new TreeSet<>());

        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(16, 64, 500L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        Tiquality.SCHEDULER.scheduleWait(new Runnable() {
            @Override
            public void run() {
                while (referenceIterator.hasNext()) {
                    Map.Entry<ReferencedTickable.ReferenceId, TickTime> entry = referenceIterator.next();
                    referenceIterator.remove(); /* Lots of data can be in here, Free up some sweet sweet RAM. */

                    threadPool.submit(new AnalyzedComponent.Analyzer(entry.getKey().convert(), entry.getValue(), finishedAnalyzers));
                }
            }
        });

        threadPool.shutdown();

        while (threadPool.awaitTermination(5, TimeUnit.SECONDS) == false) {
            long completed = threadPool.getCompletedTaskCount();
            long total = threadPool.getTaskCount();

            printer.onProgressUpdate(total, completed);
        }

        return finishedAnalyzers;
    }

    public void start() throws TiqualityException {
        key = tracker.startProfiler(System.currentTimeMillis() + durationInMs);
        startTimeNanos = System.nanoTime();
        new Thread(this, "Silent Tiquality Profiler [" + tracker.getIdentifier() + "]").start();
    }

    @Override
    public void run() {
        isProfiling = true;
        try {
            Thread.sleep(durationInMs);
        } catch (InterruptedException e) {
            Tiquality.LOGGER.warn("Failed to sleep for " + durationInMs + " ms. Profiling aborted.");
            e.printStackTrace();
            return;
        }

        final TickLogger logger;
        final long endTimeNanos = System.nanoTime();
        try {
            logger = tracker.stopProfiler(key);
        } catch (TiqualityException.TrackerWasNotProfilingException | TiqualityException.InvalidKeyException e) {
            Tiquality.LOGGER.warn("Tried to stop profiler, but an exception occurred. This probably indicates a collision. Nothing fatal, but this should be looked in to. This means we do not have any results, however.");
            e.printStackTrace();
            return;
        }

        SortedSet<AnalyzedComponent> components;
        try {
            components = analyzeComponents(logger, printer);
        } catch (InterruptedException e) {
            return;
        }

        SA_ProfileReport report = new SA_ProfileReport(tracker, startTimeNanos, endTimeNanos, logger, components);
        printer.onProfileFinish(report);
        isProfiling = false;
    }

    public interface ProfilePrinter  {
        /**
         * Returned report
         *
         * @param report the report
         */
        void onProfileFinish(SA_ProfileReport report);
        void onProgressUpdate(long total, long completed);
    }

}

package com.github.terminatornl.tiquality.profiling.standalone;

import com.github.terminatornl.tiquality.profiling.AnalyzedComponent;
import com.github.terminatornl.tiquality.profiling.TickLogger;
import com.github.terminatornl.tiquality.profiling.TickTime;
import com.github.terminatornl.tiquality.profiling.interfaces.IAnalyzedComponent;
import com.github.terminatornl.tiquality.profiling.interfaces.ITickTime;
import com.github.terminatornl.tiquality.util.Constants;

import java.util.*;

import static com.github.terminatornl.tiquality.util.Utils.TWO_DECIMAL_FORMATTER;

public class SA_ProfileReport  {

    private final long startTimeNanos;
    private final long endTimeNanos;
    private final TreeSet<AnalyzedComponent> analyzedComponents = new TreeSet<>();
    private final TreeMap<String, ITickTime> classTimes = new TreeMap<>();
    private double serverTPS;
    private double trackerTPS;
    private int serverTicks;
    private int trackerTicks;
    private long grantedNanos;
    private long totalNanosUsed = 0L;
    private NavigableSet<Map.Entry<String, ITickTime>> classTimesSorted = null;

    public SA_ProfileReport(long startTimeNanos, long endTimeNanos, TickLogger logger, Collection<AnalyzedComponent> analyzedComponents) {
        long totalTimeNanos = endTimeNanos - startTimeNanos;
        this.startTimeNanos = startTimeNanos;
        this.endTimeNanos = endTimeNanos;
        this.serverTicks = logger.getServerTicks();
        this.trackerTicks = logger.getTrackerTicks();

        double idealTicks = totalTimeNanos / Constants.NS_IN_TICK_DOUBLE;
        this.serverTPS = this.serverTicks / idealTicks * 20D;
        this.trackerTPS = this.trackerTicks / idealTicks * 20D;


        this.grantedNanos = logger.getGrantedNanos();
        this.analyzedComponents.addAll(analyzedComponents);
        for (AnalyzedComponent component : this.analyzedComponents) {
            /*
                Total nanoseconds used
             */
            totalNanosUsed += component.getTimes().getNanosConsumed();

            /*
                Class times
             */
            TickTime time = (TickTime) classTimes.get(component.getReferencedClass());
            if (time == null) {
                classTimes.put(component.getReferencedClass(), new TickTime(component.getTimes()));
            } else {
                time.add(component.getTimes());
            }
        }
    }

    public double getServerTPS() {
        return serverTPS;
    }

    public double getTrackerTPS() {
        return trackerTPS;
    }

    public int getServerTicks() {
        return serverTicks;
    }

    public int getTrackerTicks() {
        return trackerTicks;
    }

    public String getTrackerImpactPercentage(ITickTime time) {
        double factor = (double) time.getNanosConsumed() / (double) this.grantedNanos;
        return TWO_DECIMAL_FORMATTER.format(Math.round(factor * 10000D) / 100D);
    }

    public String getServerImpactPercentage(ITickTime time) {
        double nanosPassedOnServer = (Constants.NS_IN_TICK_DOUBLE * (double) serverTicks * serverTPS / 20D);

        double factor = (double) time.getNanosConsumed() / nanosPassedOnServer;
        return TWO_DECIMAL_FORMATTER.format(Math.round(factor * 10000D) / 100D);
    }

    public double getMuPerTick(ITickTime time) {
        return ((double) time.getNanosConsumed() / 1000) / (double) trackerTicks;
    }

    public double getCallsPerTick(ITickTime time) {
        return ((double) time.getCalls()) / (double) trackerTicks;
    }

    public NavigableSet<IAnalyzedComponent> getAnalyzedComponents() {
        return Collections.unmodifiableNavigableSet((TreeSet<IAnalyzedComponent>)(Object)analyzedComponents);
    }

    public NavigableMap<String, ITickTime> getClassTimes() {
        return Collections.unmodifiableNavigableMap(classTimes);
    }

    public NavigableSet<Map.Entry<String, ITickTime>> getClassTimesSorted() {
        if (classTimesSorted != null) {
            return classTimesSorted;
        }
        TreeSet<Map.Entry<String, ITickTime>> set = new TreeSet<>(Comparator.comparing(Map.Entry::getValue));
        set.addAll(classTimes.entrySet());
        classTimesSorted = Collections.unmodifiableNavigableSet(set);
        return classTimesSorted;
    }

    public long getTotalNanosUsed() {
        return totalNanosUsed;
    }

    public long getStartTimeNanos() {
        return startTimeNanos;
    }

    public long getEndTimeNanos() {
        return endTimeNanos;
    }
}

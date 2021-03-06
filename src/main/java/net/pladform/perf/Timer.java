package net.pladform.perf;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.pladform.perf.TimerStats.*;

/**
 * @author Dan Barrese
 */
public class Timer {

    private static final Instant jvmStart = Instant.now();
    private static final Map<String, Map<String, ThreadTicker>> timers = new HashMap<>();
    private static Priority priority = Priority.LOW;
    private static boolean enabled = true;

    @SuppressWarnings("unused")
    public enum Priority {
        LOW, MEDIUM, HIGH
    }

    @SuppressWarnings("unused")
    public static void setPriority(Priority priority) {
        Timer.priority = priority;
    }

    @SuppressWarnings("unused")
    public static void disable() {
        enabled = false;
    }

    public static void start(String name) {
        if (enabled) {
            getThreadedAvg(name).start();
        }
    }

    public static void start(String name, Priority priority) {
        if (enabled && priority.ordinal() >= Timer.priority.ordinal()) {
            start(name);
        }
    }

    public static void stop(String name) {
        if (enabled) {
            timers.get(name).get(threaded(name)).stop();
        }
    }

    public static void stop(String name, Priority priority) {
        if (enabled && priority.ordinal() >= Timer.priority.ordinal()) {
            stop(name);
        }
    }

    public static TickerStats getTickerStats(String name) {
        TickerStats tickerStats = new TickerStats();
        tickerStats.setThreadTicker(new ThreadTicker());
        tickerStats.setName(name);
        for (ThreadTicker ticker : timers.get(name).values()) {
            tickerStats.getThreadTicker().merge(ticker);
        }
        return tickerStats;
    }

    public static TimerStats getStats() {
        long jvmRunTime = Duration.between(jvmStart, Instant.now()).toMillis();
        LongRef totalMillisForAllTimers = new LongRef(0L);
        List<TickerStats> allTickStats = timers.keySet().stream()
                .map(name -> {
                    TickerStats tickerStats = getTickerStats(name);
                    totalMillisForAllTimers.incNum(tickerStats.getThreadTicker().getSum());
                    tickerStats.setTotalMillisForAllTimers(totalMillisForAllTimers);
                    return tickerStats;
                })
                .sorted()
                .collect(Collectors.toList());
        return new TimerStats(totalMillisForAllTimers.getNum(), jvmRunTime, allTickStats);
    }

    // ------------------------------
    // private methods
    // ------------------------------

    private static ThreadTicker getThreadedAvg(String name) {
        if (!timers.containsKey(name)) {
            synchronized (Timer.class) {
                timers.putIfAbsent(name, new HashMap<>());
            }
        }
        Map<String, ThreadTicker> avgsPerThread = timers.get(name);
        String threadedName = threaded(name);
        if (!avgsPerThread.containsKey(threadedName)) {
            synchronized (Timer.class) {
                avgsPerThread.putIfAbsent(threadedName, new ThreadTicker());
            }
        }
        return avgsPerThread.get(threadedName);
    }

    private static String threaded(String name) {
        return name + Thread.currentThread().getName();
    }

}

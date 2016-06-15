package net.pladform.perf;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Dan Barrese
 */
@SuppressWarnings("ALL")
public class TimerStats implements Serializable {

    private static long serialVersionUID = 1L;
    private long totalMillisForAllTimers;
    private long jvmRunTime;
    private List<TickStats> tickStats;

    public TimerStats(long totalMillisForAllTimers, long jvmRunTime, List<TickStats> tickStats) {
        this.totalMillisForAllTimers = totalMillisForAllTimers;
        this.jvmRunTime = jvmRunTime;
        this.tickStats = tickStats;
    }

    @Override
    public String toString() {
        return String.format("All Timers: %d ms -- JVM run time: %d ms%s",
                totalMillisForAllTimers,
                jvmRunTime,
                System.lineSeparator() + tickStats.stream()
                        .map(TickStats::toString)
                        .collect(Collectors.joining(System.lineSeparator())));
    }

    // ---------------------------------------
    // public inner classes
    // ---------------------------------------

    public static class TickStats implements Serializable, Comparable<TickStats> {
        private static long serialVersionUID = 1L;
        private static final DecimalFormat df = new DecimalFormat("0.00");
        private String name;
        private ThreadTicker threadTicker;
        private LongRef totalMillisForAllTimers;

        public ThreadTicker getThreadTicker() { return threadTicker; }
        public void setThreadTicker(ThreadTicker threadTicker) { this.threadTicker = threadTicker; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public LongRef getTotalMillisForAllTimers() { return totalMillisForAllTimers; }
        public void setTotalMillisForAllTimers(LongRef totalMillisForAllTimers) { this.totalMillisForAllTimers = totalMillisForAllTimers; }

        @Override
        public int compareTo(TickStats o) {
            return Double.compare(o.threadTicker.sum, this.threadTicker.sum);
        }

        @Override
        public String toString() {
            return String.format(
                    "Timer (%s) -- AVG %s ms -- TOTAL %d ms -- %s",
                    name,
                    df.format(threadTicker.getAverage()),
                    threadTicker.sum,
                    NumberFormat.getPercentInstance().format(threadTicker.sum / (double) totalMillisForAllTimers.num));
        }
    }

    public static class ThreadTicker implements Serializable {
        private static long serialVersionUID = 1L;
        private long sum;
        private long count;
        private transient Instant start;

        public ThreadTicker() {
        }

        public long getSum() { return sum + getNextIfStoppedNow(); }
        public void setSum(long sum) { this.sum = sum; }

        public long getCount() { return count + (start == null ? 0 : 1); }
        public void setCount(long count) { this.count = count; }

        public void merge(ThreadTicker other) {
            sum += other.sum;
            count += other.count;
        }

        public void stop() {
            if (start == null) {
                throw new IllegalStateException("cannot stop timer because it's not started yet");
            }
            long next = Duration.between(start, Instant.now()).toMillis();
            add(next);
            start = null;
        }

        public void start() {
            if (start != null) {
                throw new IllegalStateException("cannot start timer because it's alread started");
            }
            start = Instant.now();
        }

        public double getAverage() {
            return sum / (double) count;
        }

        private void add(long next) {
            sum += next;
            ++count;
            if (sum < next) {
                throw new IllegalStateException("numeric overflow");
            }
        }

        private long getNextIfStoppedNow() {
            return start == null ? 0L : Duration.between(start, Instant.now()).toMillis();
        }
    }

    public static class LongRef implements Serializable {
        private static long serialVersionUID = 1L;
        private long num;

        public LongRef(long num) {
            this.num = num;
        }

        public long getNum() { return num; }
        public void incNum(long inc) { num += inc; }
    }

}

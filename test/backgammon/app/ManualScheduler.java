package backgammon.app;

import java.util.PriorityQueue;

/**
 * A scheduler for tests: time only moves when the test says so.
 *
 * <p>Tasks wait in a queue ordered by when they are due. {@link #advance(int)}
 * moves the clock forward and runs every task that falls due along the way,
 * including tasks those tasks schedule, in due order. No threads and no
 * sleeping, so controller checks run instantly and deterministically.</p>
 */
final class ManualScheduler implements Scheduler {

    /** A task and the time it is due. */
    private static final class Pending implements Comparable<Pending> {

        /** When the task is due, in scheduler milliseconds. */
        private final long due;

        /** Tie-breaker so tasks due together run in the order scheduled. */
        private final long sequence;

        /** What to run. */
        private final Runnable task;

        /**
         * Creates a pending task.
         *
         * @param due      when it is due
         * @param sequence its place in scheduling order
         * @param task     what to run
         */
        Pending(final long due, final long sequence, final Runnable task) {
            this.due = due;
            this.sequence = sequence;
            this.task = task;
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(final Pending other) {
            final int byTime = Long.compare(due, other.due);
            return byTime != 0 ? byTime : Long.compare(sequence, other.sequence);
        }
    }

    /** Tasks not yet run, soonest first. */
    private final PriorityQueue<Pending> queue = new PriorityQueue<>();

    /** The current time, in milliseconds since the scheduler was made. */
    private long now;

    /** How many tasks have ever been scheduled, for ordering ties. */
    private long scheduled;

    /** {@inheritDoc} */
    @Override
    public void schedule(final int delayMillis, final Runnable task) {
        queue.add(new Pending(now + delayMillis, scheduled++, task));
    }

    /**
     * Moves time forward, running every task that falls due.
     *
     * @param millis how far to move the clock
     */
    void advance(final int millis) {
        final long target = now + millis;
        while (!queue.isEmpty() && queue.peek().due <= target) {
            final Pending next = queue.poll();
            now = next.due;
            next.task.run();
        }
        now = target;
    }
}

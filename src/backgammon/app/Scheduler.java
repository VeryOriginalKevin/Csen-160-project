package backgammon.app;

/**
 * Runs a task once after a delay, on the same thread as the controller.
 *
 * <p>The controller needs timed events &mdash; the trivia countdown, the pause
 * that reveals the correct answer &mdash; but should not be tied to a real clock,
 * or tests would have to sleep. In the running game {@link SwingScheduler}
 * fires tasks on Swing's event dispatch thread; tests supply a scheduler they
 * advance by hand.</p>
 */
public interface Scheduler {

    /**
     * Arranges for a task to run once, after a delay.
     *
     * <p>There is no way to cancel a task. Callers that may no longer want it
     * when it fires check, inside the task, whether it is still relevant.</p>
     *
     * @param delayMillis how long to wait, in milliseconds
     * @param task        what to run; it runs on the controller's thread
     */
    void schedule(int delayMillis, Runnable task);
}

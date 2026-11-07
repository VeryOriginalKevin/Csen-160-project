package backgammon.app;

import javax.swing.Timer;

/**
 * The scheduler the running game uses: a one-shot Swing timer per task.
 *
 * <p>Swing timers fire on the event dispatch thread, which is the thread every
 * button press and click also arrives on, so the controller never has to deal
 * with two threads touching its state.</p>
 */
public final class SwingScheduler implements Scheduler {

    /** Creates the scheduler. */
    public SwingScheduler() {
        // Stateless: each call makes its own timer.
    }

    /** {@inheritDoc} */
    @Override
    public void schedule(final int delayMillis, final Runnable task) {
        final Timer timer = new Timer(delayMillis, event -> task.run());
        // Fire once, then stop; the timer is garbage once it has fired.
        timer.setRepeats(false);
        timer.start();
    }
}

package backgammon.app;

/**
 * Told whenever anything the screen shows may have changed.
 *
 * <p>The views register one of these with the {@link GameController} and redraw
 * from the controller's getters when it fires. A single coarse notification is
 * plenty: the whole window repaints in well under a frame.</p>
 */
public interface GameListener {

    /** Called on the controller's thread after its state changes. */
    void gameChanged();
}

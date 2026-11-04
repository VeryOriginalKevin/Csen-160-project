/**
 * Swing views and the Java2D painting of the board, checkers, and dice.
 *
 * <p>Every game graphic is drawn procedurally from the geometry and palette in
 * {@link backgammon.ui.Theme}, so the application ships no image files and
 * scales cleanly to any window size.</p>
 *
 * <p>Views read from the model and report user intent to
 * {@link backgammon.app}; they never mutate game state directly.</p>
 */
package backgammon.ui;

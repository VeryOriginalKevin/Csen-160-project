/**
 * Application wiring and turn orchestration.
 *
 * <p>This package owns the state machine that sequences a turn through its
 * phases &mdash; trivia contest, dice draft, then each player's movement &mdash;
 * and connects {@link backgammon.model} to {@link backgammon.ui}. It is the only
 * package that depends on both.</p>
 */
package backgammon.app;

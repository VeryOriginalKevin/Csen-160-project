/**
 * Turn orchestration: the controller that sequences a round.
 *
 * <p>{@link backgammon.app.GameController} owns the state machine that walks
 * each round through its {@link backgammon.app.TurnPhase phases} &mdash; trivia
 * contest, dice draft, then each player's movement &mdash; and decides which
 * input is accepted when. It depends on {@link backgammon.model} and
 * {@link backgammon.trivia}; the views in {@link backgammon.ui} call into it and
 * listen to it, but it never refers to them.</p>
 */
package backgammon.app;

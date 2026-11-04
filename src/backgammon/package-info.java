/**
 * Root package for the dice-draft backgammon variant built for CSEN 160.
 *
 * <p>The application is a single-window, local-multiplayer game. Each turn
 * begins with a trivia contest in which both players answer simultaneously; the
 * winner takes the first move of the turn and hand-picks two of the four rolled
 * dice, while the loser moves second with whatever remains.</p>
 *
 * <p>Package layout:</p>
 * <ul>
 *   <li>{@link backgammon.app} &mdash; application wiring and turn orchestration.</li>
 *   <li>{@link backgammon.model} &mdash; board, dice, moves, and rules; no UI.</li>
 *   <li>{@link backgammon.trivia} &mdash; questions from opentdb.com, and the contest.</li>
 *   <li>{@link backgammon.ui} &mdash; Swing views and Java2D painting.</li>
 * </ul>
 */
package backgammon;

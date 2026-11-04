/**
 * Game state and rules: the board, the four-die draft, and move legality.
 *
 * <p>Nothing in this package may import Swing, AWT, or any networking class.
 * Keeping the rules free of the user interface is what makes them unit testable
 * without starting the application.</p>
 *
 * <p>Points are numbered 1&ndash;24 once for the whole board;
 * {@link backgammon.model.PlayerColor#getDirection()} defines which way each
 * side travels through that numbering.</p>
 */
package backgammon.model;

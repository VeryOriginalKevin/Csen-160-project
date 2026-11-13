# Triviagammon

A local two-player backgammon variant where trivia skill decides turn order.
Each round, 4 dice are rolled and both players race to answer a trivia
question (White: keys 1-4, Black: keys 7-0). The winner picks 2 of the 4
dice and moves first; the loser moves with the other 2.

Plain Java + Swing, no external dependencies, no build tool, no image assets
(the board is drawn live with Java2D).

## How to play

1. **Pick a topic and difficulty** on the title screen, then click Start.
2. **Trivia round.** Four dice are rolled and a question appears with four
   answers. Both players answer at the same time on a shared keyboard:
   - White presses **1-4**
   - Black presses **7-0**

   Whoever answers correctly first wins the round; a wrong answer hands the
   win to the other player. No answer in 15 seconds picks a winner at random.
3. **Draft.** The winner clicks 2 of the 4 dice in the tray to keep for
   themselves; the loser gets the other 2.
4. **Move.** The winner moves first with their two dice, then the loser
   moves with the remaining two. Click a checker (movable ones get a gold
   ring), then click a highlighted point to move it there. Rolling a pair
   gives four moves instead of two, as in standard backgammon.
5. Repeat until one side bears off all 15 checkers.

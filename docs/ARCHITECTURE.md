# Architecture

Plain JDK (tested on 15), Swing, no external dependencies. Trivia is fetched with the JDK's
built-in `java.net.http.HttpClient`; the JSON response is read by a small
hand-written parser in `backgammon.trivia`.

## Layout

```
src/backgammon/
  Main.java        entry point
  app/             GameController: the turn state machine and input rules
  model/           board, dice, moves, rules -- no UI, no networking
  trivia/          opentdb.com client, question model, the contest
  ui/              Swing panels and Java2D painting
test/backgammon/   unit tests, mirroring the src packages
docs/              this file; docs/api is generated javadoc (git-ignored)
```

There is no build tool: plain `javac` and `java`, run from the project root.

```
javac -d out src/backgammon/*.java src/backgammon/app/*.java src/backgammon/model/*.java src/backgammon/trivia/*.java src/backgammon/ui/*.java
java -cp out backgammon.Main
```

The dependency rule, one direction only:

```
model   <-  trivia  <-  app  <-  ui  <-  Main
```

`model` imports nothing from the project, and never Swing, AWT, or networking,
so the rules are testable without opening a window. `app` drives `model` and
`trivia` but never refers to `ui`. `ui` reads `model` objects to draw them and
calls `app` for every action; it never changes game state itself. `Main` only
opens the window; `app.GameSession` assembles each game when Start is pressed.

## A turn

1. Four dice are rolled. A question is shown and both players' keys are live:
   White answers with 1-4, Black with 7-0. Both on the
   keyboard, so neither is slowed by a mouse.
2. First correct answer wins the contest; a wrong answer concedes it. After 15
   seconds with no answer the winner is drawn at random. The correct answer is
   shown for 2 seconds.
3. The winner clicks two of the four dice. Only the winner has input.
4. The winner moves, then the loser moves with the two dice left behind. A
   matching pair gives four moves, as in normal backgammon.

`backgammon.app.TurnPhase` enumerates those steps, and `GameController`
ignores any input that does not fit the current phase. Outside step 1, exactly
one player is in control.

### Movement rules (`model.RuleBook`)

Standard backgammon movement, hitting, bar entry and bearing off, with one
deliberate relaxation: any legal move may be made in any order, with no
obligation to pick moves that use both dice. Checkers on the bar must enter
before anything else moves. A player's turn ends when their dice are spent or
no legal move remains; in the second case a "no legal moves" notice shows for a
moment first. The game ends the instant a side bears off its fifteenth checker.

## Model and app classes

```
model/
  PlayerColor     WHITE/BLACK, direction of travel, home board, bar/off points
  Board           checker counts on points, bar, trays; carries out moves
  Move            (from, to, die); from may be BAR, to may be OFF
  Die, DiceRoll   one die; the four dice of a round
  DiceDraft       the winner's two picks and the loser's two leftovers
  DicePool        die values a player has left (a pair becomes four)
  RuleBook        which single moves are legal
  MovementTurn    one player spending their pool, move by move
  Game            board + dice + winner
app/
  GameController  the round state machine; the only class ui calls
  TurnPhase       the five phases
  Scheduler       "run this after N ms": Swing timer in the game, manual in tests
  GameListener    "state changed, redraw"
  GameSession     one game: its question deck + controller; end() stops both
```

Timed events (countdown, reveal pause, blocked pause) go through `Scheduler`
so the controller checks can drive time by hand. A scheduled task is ignored if
a new round or game has started since it was scheduled, so stale timers are
harmless without needing cancellation.

## Visuals

There are no image files. The board's twenty-four points are triangles, the
thirty checkers are circles, and the four dice are rounded rectangles with pip
dots -- all drawn with Java2D from the geometry in `ui` and the palette in
`backgammon.ui.Theme`. This keeps the game resolution-independent, keeps binary
assets out of git, and means retuning the look is a one-file edit.

```
ui/
  MainWindow      the frame; shows TitleScreen, then a GameScreen per game
  TitleScreen     game name, topic and difficulty choices, Start button
  ChoiceGroup     a grid of ChoiceChips: "Random" or any mix of options
  GameScreen      board left, side panel right; binds answer keys
  BoardGeometry   where every point/checker/tray is for a given size;
                  shared by painting and click hit-testing
  BoardPanel      paints the board; clicks -> controller.clickLocation
  DiceTray        the four dice; clicks during the draft -> controller.pickDie
  TriviaPanel     question, countdown, four AnswerRows
  StatusPanel     "whose turn, what to do" headline
  AnswerKeys      the 1-4 / 7-0 key layout, used for bindings and labels
```

Moving: click a checker (movable stacks have a faint gold ring), then a
highlighted point or the tray. When entering from the bar is the only option,
the bar is selected automatically.

## Trivia

```
JsonValue / JsonParser   minimal JSON reader, so there are no dependencies
TriviaQuestion           one question, answers already shuffled, immutable
Difficulty               the API's easy/medium/hard tier
TriviaCategory           the API's 24 topics and their ids
TriviaSettings           topics + difficulties chosen; empty set = any
TriviaSource             interface: somewhere questions come from
  OpenTriviaDatabase     live: HTTP to opentdb.com
  OfflineTriviaSource    bundled: 16 questions, cannot fail
QuestionDeck             one supply per chosen topic x difficulty; mixes them
TriviaContest            the two-player race; decides who moves first
ContestResult            who won the contest, and why
```

Three decisions worth knowing:

**Questions are fetched in the background, one supply per choice.** The API
allows one request per IP every five seconds and takes one topic and one
difficulty per request. So `QuestionDeck` keeps a separate supply (its own
`OpenTriviaDatabase` and buffer of up to 10) for every combination of the chosen
topics and difficulties, refills the emptiest one in the background, and picks
a random stocked supply for each question. That way every question is
independently from any chosen topic. `next()` never blocks: if every supply is
empty it serves a bundled question. The pacing clock in `OpenTriviaDatabase` is
static, because the limit counts per IP address rather than per object.

**A game waits for its first live question.** Bundled questions cover few
topics, so `GameSession` shows "Fetching questions..." until the first live
batch arrives (usually under a second) and only starts with bundled questions
after 10 s with no answer from the API, i.e. no internet.

**Answers are requested URL-encoded** (`encode=url3986`) rather than in the API's
default HTML-escaped form, so text is recovered with `URLDecoder` instead of
needing an HTML entity decoder. Only `type=multiple` is requested, so every
question has exactly four answers and each player's panel always needs four
buttons.

**The contest ends on the first answer submitted.** A correct answer wins it; an
incorrect one hands it to the other player. Every later press is ignored, which
is what makes the winner immediately the only player in control. No tie is
possible: Swing delivers presses one at a time on the event dispatch thread, so
one is always first, however close together they land.

## Checks

Each check class under `test/` is a plain `main` method, compiled and run
directly:

```
javac -d out -sourcepath src test/backgammon/model/*.java test/backgammon/app/*.java test/backgammon/trivia/*.java
java -cp out backgammon.model.ModelChecks
java -cp out backgammon.app.ControllerChecks
java -cp out backgammon.trivia.TriviaChecks
```

`ModelChecks` covers the rules; `ControllerChecks` covers the phase sequence and
also plays twenty whole games with random answers and moves to prove a game
always reaches its end.

No test framework, to keep the no-dependency promise; a check class prints only
its failures and exits non-zero if any failed. Network checks skip rather than fail
when opentdb.com is unreachable, since the game is meant to work offline.

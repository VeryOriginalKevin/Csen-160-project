# Handover

Last updated 2026-11-04. Read `README.md` for the game rules and
`docs/ARCHITECTURE.md` for how the code is organised; this file is just the
state of play.

## Getting it running

```
tools/build.ps1     compile src/ to out/classes
tools/run.ps1       build, then run backgammon.Main
tools/test.ps1      build, compile test/, run the checks
tools/javadoc.ps1   generate docs/api
```

Java 17, Swing, no external dependencies. There is no Maven or Gradle, on
purpose — the scripts call `javac` directly.

One gotcha: the `java` on PATH on the original dev machine is a 1.8 JRE with no
compiler, and it cannot run classes compiled for 17. The scripts therefore look
a JDK up themselves (`tools/Jdk.ps1`): `JAVA_HOME` first, then `~/.jdks`, then
IntelliJ's bundled runtime. Set `JAVA_HOME` to a JDK 17+ install and everything
works regardless.

## What is done

**The trivia half is finished and tested.** 11 classes in
`src/backgammon/trivia/`: a hand-written JSON reader, the live opentdb.com
client, 16 bundled offline questions, the background question buffer, and the
two-player contest that decides who moves first.

`tools/test.ps1` runs 78 checks over that package and passes, including a live
fetch. Everything compiles clean under `-Xlint:all` and the javadoc generates
without warnings.

## What is not done

**Nothing is committed yet.** The whole tree is still untracked.

**The board model is empty.** `src/backgammon/model/` holds only `PlayerColor`
and `Die`. Still needed: the board and its 24 points, the bar and trays,
checkers, the four-die roll and the draft of two, move legality, hitting and
entering from the bar, and bearing off.

**There is no UI at all.** `src/backgammon/ui/` holds only `Theme`, which is the
palette and fonts every graphic will be drawn from. `Main` prints a line and
exits; it does not open a window.

**`src/backgammon/app/` holds only `TurnPhase`**, the five-phase turn enum. The
controller that drives those phases does not exist, so nothing connects the
model to the UI yet.

## Decisions already made

- **No image files.** The board's points are triangles, checkers are circles,
  dice are rounded rectangles with pips — all drawn with Java2D from `Theme`.
  Resolution-independent, no binary assets in git, retuning the look is a
  one-file edit.
- **Batches of 20 questions, fetched in the background.** The API allows one
  request per IP every five seconds, so the pacing clock in
  `OpenTriviaDatabase` is `static` — per address, not per object. Getting this
  wrong produced an HTTP 429 during testing.
- **Multiple-choice questions only** (`type=multiple`), so every question has
  exactly four answers and each player's panel always needs four buttons.
- **Checks are plain `main` methods, not JUnit**, to keep the no-dependency
  promise. Swapping in JUnit means jars in a `lib/` and a classpath change in
  `tools/build.ps1` and `tools/test.ps1`.

## Open questions

- **Nobody answers the question.** The README does not cover this, and the turn
  has to start somehow, so `TriviaContest.resolveByTimeout` draws the winner at
  random. Reasonable alternatives: whoever lost the previous contest, or whoever
  is behind on the board. Worth confirming with the team.
- **There is no time limit on a question.** `resolveByTimeout` exists but nothing
  calls it, because no duration was ever specified. A countdown needs one.
- **Doubling cube.** Not mentioned in the README and not built. Presumably out of
  scope, since the dice draft already replaces the usual turn order.

## Where to start

The trivia package is ready to put on screen, so either direction works:

1. **The board model** — `Board`, `Move`, and legality checking in
   `src/backgammon/model/`. Pure logic, testable without a window, and
   `tools/test.ps1` is already set up to run new check classes (register them in
   the `$checkClasses` list).
2. **The trivia panel** — a Swing panel showing a `TriviaQuestion` with two sets
   of four buttons, calling `TriviaContest.submitAnswer`. This makes something
   visible and exercises the finished half.

Doing the model first is the safer order: the UI needs something to render, and
the model is where the rules risk hides.

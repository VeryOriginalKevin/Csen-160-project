# Architecture

Java 17, Swing, no external dependencies. Trivia is fetched with the JDK's
built-in `java.net.http.HttpClient`; the JSON response is read by a small
hand-written parser in `backgammon.trivia`.

## Layout

```
src/backgammon/
  Main.java        entry point
  app/             turn state machine, wiring between model and ui
  model/           board, dice, moves, rules -- no UI, no networking
  trivia/          opentdb.com client, question model, the contest
  ui/              Swing panels and Java2D painting
test/backgammon/   unit tests, mirroring the src packages
tools/             build.ps1, run.ps1, javadoc.ps1
docs/              this file; docs/api is generated javadoc (git-ignored)
```

The dependency rule: `ui` and `model` never import each other, and `model` never
imports Swing, AWT, or networking. `app` is the only package that may import
both. That rule is what makes the rules testable without opening a window.

## A turn

1. Four dice are rolled. A question is shown, both players' buttons live.
2. First correct answer wins the contest; a wrong answer concedes it. The winner
   moves first this turn.
3. The winner drafts two of the four dice. Only the winner has input.
4. The winner moves, then the loser moves with the two dice left behind.

`backgammon.app.TurnPhase` enumerates those steps, and the current phase decides
which inputs the UI accepts. Outside step 1, exactly one player is in control.

## Visuals

There are no image files. The board's twenty-four points are triangles, the
thirty checkers are circles, and the four dice are rounded rectangles with pip
dots -- all drawn with Java2D from the geometry in `ui` and the palette in
`backgammon.ui.Theme`. This keeps the game resolution-independent, keeps binary
assets out of git, and means retuning the look is a one-file edit.

## Trivia

```
JsonValue / JsonParser   minimal JSON reader, so there are no dependencies
TriviaQuestion           one question, answers already shuffled, immutable
Difficulty               the API's easy/medium/hard tier
TriviaSource             interface: somewhere questions come from
  OpenTriviaDatabase     live: HTTP to opentdb.com
  OfflineTriviaSource    bundled: 16 questions, cannot fail
QuestionDeck             buffers live questions, falls back to bundled ones
TriviaContest            the two-player race; decides who moves first
ContestResult            who won the contest, and why
```

Three decisions worth knowing:

**Questions are requested in batches of 20, in the background.** The API allows
one request per IP every five seconds, and a turn must not wait on the network.
`QuestionDeck.next()` returns immediately and never returns null: if the buffer
is empty it serves a bundled question instead. One request covers twenty turns.
The pacing clock in `OpenTriviaDatabase` is static, because the limit counts per
IP address rather than per object.

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

`tools/test.ps1` compiles `test/` and runs each check class's `main`. No test
framework, to keep the no-dependency promise; a check class prints only its
failures and exits non-zero if any failed. Network checks skip rather than fail
when opentdb.com is unreachable, since the game is meant to work offline.

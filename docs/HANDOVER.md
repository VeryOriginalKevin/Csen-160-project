# Handover

Last updated 2026-11-07. Read `CLAUDE.md` for the game rules and
`docs/ARCHITECTURE.md` for how the code is organised; this file is just the
state of play.

## Getting it running

No build tool, no scripts: plain `javac` and `java` from the project root.

```
# compile and run the game
javac -d out src/backgammon/*.java src/backgammon/app/*.java src/backgammon/model/*.java src/backgammon/trivia/*.java src/backgammon/ui/*.java
java -cp out backgammon.Main

# compile and run the checks (-sourcepath pulls in the src classes they use)
javac -d out -sourcepath src test/backgammon/model/*.java test/backgammon/app/*.java test/backgammon/trivia/*.java
java -cp out backgammon.model.ModelChecks
java -cp out backgammon.app.ControllerChecks
java -cp out backgammon.trivia.TriviaChecks
```

Swing and the JDK's `java.net.http` only; no external dependencies. Compiles
clean under `-Xlint:all` on JDK 15. `out/` is git-ignored.

## What is done

**The whole game is playable end to end.** Trivia contest (keys 1-4 vs 7-0,
15 s countdown), answer reveal, dice draft by mouse, both players' movement
with full backgammon rules (hitting, bar entry, bearing off, doubles = four
moves), automatic turn end when blocked, game over banner. A title screen
lets players pick any mix of trivia topics and difficulties, or Random for
either; New game returns to it.

Checks: `ModelChecks` 50, `ControllerChecks` 69 (including 20 complete random
games), `TriviaChecks` 89. All pass.

## Decisions already made

- **Input.** Trivia: White keys 1-4, Black keys 7-0, both
  on the keyboard so the race is fair. Everything else is the mouse, used by
  whichever single player is in control.
- **Relaxed move obligation.** Any legal move in any order; no rule forcing
  both dice to be used. Bar checkers must enter first. A turn ends when the
  dice are spent or nothing legal remains.
- **Doubles.** A drafted matching pair gives four moves.
- **No answer in 15 s.** The winner is drawn at random.
- **Players are White and Black,** matching their checkers.
- **Trivia choices.** The API takes one topic and one difficulty per request,
  so the deck keeps one supply per chosen combination and draws each question
  from a random stocked supply. Supplies fill 5 s apart (the API's rate
  limit), so with several topics the first round or two may come from the
  first topic fetched. If a narrow choice has too few questions, smaller
  batches are tried; a combination that fails 3 times is dropped.
- **Start waits for live questions** (a "Fetching questions..." screen, up to
  10 s), so the first question follows the players' choice.
- **Dependency direction.** `model <- trivia <- app <- ui <- Main`. `ui` reads
  model objects to draw them but changes state only through `GameController`.
- **No image files.** Everything is drawn with Java2D from `Theme`.
- **Checks are plain `main` methods, not JUnit,** to keep the no-dependency
  promise.

## Open questions / possible next steps

- **Doubling cube and gammons.** Not built; the game ends with a simple win.
- **Offline play uses 16 bundled questions.** They are only used with no
  internet. They never repeat until all eligible ones have been shown (memory
  lasts across New game), but they cover few topics, so offline play may not
  follow the chosen topics, and a long offline session will cycle. Adding more
  to `OfflineTriviaSource` is the fix if that matters.
- **No sound or animation.** Checkers jump rather than slide.
- **A move needs two clicks even when only one destination exists.** A
  one-click shortcut would be a small change in `GameController.clickLocation`.

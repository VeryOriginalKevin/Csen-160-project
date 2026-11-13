package backgammon;

import backgammon.app.ControllerTest;
import backgammon.model.ModelTest;
import backgammon.trivia.TriviaTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Runs every test class in one go. In IntelliJ, right-click this class (or
 * open it and press the run button) to run the whole suite at once.
 */
@Suite
@SelectClasses({ModelTest.class, ControllerTest.class, TriviaTest.class})
public class AllTests {
}

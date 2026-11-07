package backgammon.ui;

import backgammon.app.GameController;
import backgammon.model.PlayerColor;
import backgammon.trivia.TriviaQuestion;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shows the round's trivia question, the time left, and the four answers.
 *
 * <p>While the question is open, every answer is shown neutrally with both
 * players' keys beside it. Once it is decided, the correct answer turns green,
 * a wrong pick turns red, and the rest fade. The question stays on screen for
 * the rest of the round so players can still see what was asked.</p>
 */
public final class TriviaPanel extends JPanel {

    /** Serialisation version; Swing components are serialisable. */
    private static final long serialVersionUID = 1L;

    /** Width the question text wraps at. */
    private static final int QUESTION_WIDTH = 330;

    /** Vertical space between the answer rows. */
    private static final int ROW_SPACING = 6;

    /** The game whose question is shown. */
    private final GameController controller;

    /** Category and difficulty, above the question. */
    private final JLabel meta;

    /** The question text. */
    private final JLabel prompt;

    /** Time left to answer. */
    private final CountdownBar countdown;

    /** One row per answer. */
    private final List<AnswerRow> rows;

    /**
     * Builds the panel.
     *
     * @param controller the game whose question to show
     */
    public TriviaPanel(final GameController controller) {
        this.controller = controller;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        meta = new JLabel();
        meta.setFont(Theme.FONT_STATUS);
        meta.setForeground(Theme.TEXT_SECONDARY);
        addRow(meta);
        add(Box.createVerticalStrut(4));

        prompt = new JLabel();
        prompt.setFont(Theme.FONT_QUESTION);
        prompt.setForeground(Theme.TEXT_PRIMARY);
        addRow(prompt);
        add(Box.createVerticalStrut(10));

        countdown = new CountdownBar();
        addRow(countdown);
        add(Box.createVerticalStrut(12));

        rows = new ArrayList<>();
        for (int i = 0; i < AnswerKeys.count(); i++) {
            final AnswerRow row = new AnswerRow(i);
            rows.add(row);
            addRow(row);
            add(Box.createVerticalStrut(ROW_SPACING));
        }
        setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
    }

    /** Redraws the panel from the controller's current state. */
    public void refresh() {
        final TriviaQuestion question = controller.getQuestion();
        meta.setText(question.getCategory() + "  \u00B7  "
                + question.getDifficulty().name().toLowerCase(Locale.ROOT));
        prompt.setText(Html.wrapped(question.getPrompt(), QUESTION_WIDTH));

        final boolean open = controller.isQuestionOpen();
        // An emptied bar rather than a hidden one, so nothing below it jumps.
        countdown.setFraction(open ? controller.getTimeRemainingFraction() : 0.0);

        final List<String> answers = question.getAnswers();
        for (int i = 0; i < rows.size(); i++) {
            final AnswerRow row = rows.get(i);
            // Questions are always four-answer multiple choice, but a shorter
            // one should hide spare rows rather than crash.
            row.setVisible(i < answers.size());
            if (i >= answers.size()) {
                continue;
            }
            final PlayerColor pressedBy = i == controller.getAnsweredIndex()
                    ? controller.getAnsweredBy().orElse(null)
                    : null;
            row.show(answers.get(i), lookFor(question, i, open), pressedBy);
        }
        revalidate();
        repaint();
    }

    /**
     * Decides how one answer row should look.
     *
     * @param question the question
     * @param index    the answer's position
     * @param open     whether players may still answer
     * @return the row's look
     */
    private AnswerRow.Look lookFor(final TriviaQuestion question, final int index, final boolean open) {
        if (open) {
            return AnswerRow.Look.IDLE;
        }
        if (question.isCorrect(index)) {
            return AnswerRow.Look.CORRECT;
        }
        if (index == controller.getAnsweredIndex()) {
            return AnswerRow.Look.WRONG;
        }
        return AnswerRow.Look.FADED;
    }

    /**
     * Adds a child left-aligned and stretched to the panel's width.
     *
     * @param component the child to add
     */
    private void addRow(final JComponent component) {
        component.setAlignmentX(LEFT_ALIGNMENT);
        add(component);
    }
}

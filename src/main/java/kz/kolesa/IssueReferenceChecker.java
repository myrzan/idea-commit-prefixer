package kz.kolesa;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.UIUtil;
import kz.kolesa.git.GitBranches;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.concurrent.atomic.AtomicBoolean;

/** Перед коммитом проверяет, что в сообщении упомянута текущая ветка. */
public class IssueReferenceChecker extends CheckinHandler {

    private static final String CHECKER_STATE_KEY = "COMMIT_MESSAGE_ISSUE_CHECKER_STATE_KEY";

    private final CheckinProjectPanel panel;

    public IssueReferenceChecker(@NotNull CheckinProjectPanel panel) {
        this.panel = panel;
    }

    public static boolean isCheckMessageEnabled() {
        return PropertiesComponent.getInstance().getBoolean(CHECKER_STATE_KEY, true);
    }

    @Override
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        JBCheckBox checkBox = new JBCheckBox("Check reference to issue in message");

        return new RefreshableOnComponent() {
            @Override
            public JComponent getComponent() {
                JPanel root = new JPanel(new BorderLayout());
                root.add(checkBox, BorderLayout.WEST);
                return root;
            }

            @Override
            public void saveState() {
                PropertiesComponent.getInstance().setValue(CHECKER_STATE_KEY, checkBox.isSelected(), true);
            }

            @Override
            public void restoreState() {
                checkBox.setSelected(isCheckMessageEnabled());
            }
        };
    }

    @Override
    public ReturnResult beforeCheckin() {
        if (!isCheckMessageEnabled()) {
            return super.beforeCheckin();
        }

        Project project = panel.getProject();
        String branchName = GitBranches.currentBranch(project, panel.getRoots());

        // На main/master/develop кнопка префикс не ставит — значит и требовать
        // его здесь нельзя, иначе диалог вылезал на каждый коммит в master.
        // Раньше сюда же прилетал null и сообщение искало подстроку "null ".
        if (CommitMessages.shouldSkipPrefix(branchName)) {
            return ReturnResult.COMMIT;
        }
        if (CommitMessages.mentionsBranch(panel.getCommitMessage(), branchName)) {
            return ReturnResult.COMMIT;
        }

        return askUser(project, branchName) ? ReturnResult.COMMIT : ReturnResult.CANCEL;
    }

    /**
     * В немодальном коммите {@code beforeCheckin()} вызывается не из EDT,
     * поэтому диалог показываем через invokeAndWait.
     */
    private static boolean askUser(Project project, @NotNull String branchName) {
        AtomicBoolean confirmed = new AtomicBoolean(false);
        ApplicationManager.getApplication().invokeAndWait(() -> {
            int answer = Messages.showYesNoDialog(
                    project,
                    "Commit message doesn't contain reference to the issue \"" + branchName + "\"."
                            + "\nAre you sure you want to commit as is?",
                    "Missing Issue Reference",
                    UIUtil.getErrorIcon());
            confirmed.set(answer == Messages.YES);
        });
        return confirmed.get();
    }
}

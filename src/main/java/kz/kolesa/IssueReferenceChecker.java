package kz.kolesa;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.issueLinks.IssueLinkHtmlRenderer;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

public class IssueReferenceChecker extends CheckinHandler {
    private static final String CHECKER_STATE_KEY = "COMMIT_MESSAGE_ISSUE_CHECKER_STATE_KEY";
    private final CheckinProjectPanel panel;

    public IssueReferenceChecker(CheckinProjectPanel panel) {
        this.panel = panel;
    }

    @Override
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        final JCheckBox checkBox = new JCheckBox("Check reference to issue in message");

        return new RefreshableOnComponent() {
            @Override
            public JComponent getComponent() {
                JPanel root = new JPanel(new BorderLayout());
                root.add(checkBox, BorderLayout.WEST);
                return root;
            }

            @Override
            public void refresh() {
                // Метод оставлен пустым, так как не требует обработки
            }

            @Override
            public void saveState() {
                PropertiesComponent.getInstance().setValue(CHECKER_STATE_KEY, checkBox.isSelected());
            }

            @Override
            public void restoreState() {
                checkBox.setSelected(IssueReferenceChecker.isCheckMessageEnabled());
            }
        };
    }

    public static boolean isCheckMessageEnabled() {
        return PropertiesComponent.getInstance().getBoolean(CHECKER_STATE_KEY, true);
    }

    @Override
    public ReturnResult beforeCheckin() {
        if (!isCheckMessageEnabled()) {
            return super.beforeCheckin();
        }

        Project project = panel.getProject();
        String branchName = KolesaCommitMessageAction.getBranchName(project);
        boolean shouldCommit = findReferenceInMessage(branchName, project);

        return shouldCommit ? ReturnResult.COMMIT : ReturnResult.CANCEL;
    }

    private boolean findReferenceInMessage(String branchName, Project project) {
        String commitMessage = panel.getCommitMessage();

        if (commitMessage.contains(branchName + " ")) {
            return true;
        }

        String message = "Commit message doesn't contain reference to the issue \"" + branchName + "\".\nAre you sure you want to commit as is?";
        String html = IssueLinkHtmlRenderer.formatTextIntoHtml(project, message);

        int yesNo = Messages.showYesNoDialog(html, "Missing Issue Reference", UIUtil.getErrorIcon());

        return yesNo == Messages.YES;
    }
}

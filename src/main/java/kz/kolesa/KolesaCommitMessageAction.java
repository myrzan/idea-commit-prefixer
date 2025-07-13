package kz.kolesa;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vcs.ui.Refreshable;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KolesaCommitMessageAction extends AnAction implements DumbAware {
    @Contract("null -> !null")
    public static @Nullable String getBranchName(@Nullable Project project) {
        if (project == null) {
            return "";
        }

        VcsRoot[] vcsRoots = ProjectLevelVcsManager.getInstance(project).getAllVcsRoots();

        for (VcsRoot vcsRoot : vcsRoots) {
            File gitDir = new File(vcsRoot.getPath().getPath(), ".git/HEAD");
            if (gitDir.exists()) {
                try {
                    String content = new String(java.nio.file.Files.readAllBytes(gitDir.toPath())).trim();
                    if (content.startsWith("ref: refs/heads/")) {
                        return content.replace("ref: refs/heads/", "").trim();
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return null;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        CheckinProjectPanel checkinPanel = getCheckinPanel(e);
        if (checkinPanel == null) {
            return;
        }

        String branchName = getBranchName(e.getProject());
        if (branchName == null || branchName.isEmpty()) {
            return;
        }

        // Не трогаем стандартные ветки
        if (branchName.equals("main") || branchName.equals("master") || branchName.equals("develop")) {
            return;
        }

        String currentMessage = checkinPanel.getCommitMessage();

        // Если имя ветки уже встречается в сообщении (в любом месте), не делаем ничего
        if (currentMessage.contains(branchName)) {
            return;
        }

        // Заменяем старую ветку или добавляем новую
        String updatedMessage = replaceExistingBranch(currentMessage, branchName);

        // Обновляем коммит только если текст изменился
        if (!updatedMessage.equals(currentMessage)) {
            checkinPanel.setCommitMessage(updatedMessage);
        }
    }

    /**
     * Метод добавляет имя ветки в начало сообщения, если его не было.
     */
    private String replaceExistingBranch(String commitMessage, String newBranch) {
        if (commitMessage == null || commitMessage.trim().isEmpty()) {
            return newBranch + " ";
        }

        return newBranch + " " + commitMessage;
    }

    @Nullable
    private static CheckinProjectPanel getCheckinPanel(@Nullable AnActionEvent e) {
        if (e == null) {
            return null;
        }
        Refreshable panel = Refreshable.PANEL_KEY.getData(e.getDataContext());
        if (panel instanceof CheckinProjectPanel) {
            return (CheckinProjectPanel) panel;
        }
        return null;
    }
}

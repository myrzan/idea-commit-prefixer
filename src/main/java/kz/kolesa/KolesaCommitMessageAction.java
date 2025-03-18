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
        String currentMessage = checkinPanel.getCommitMessage();

        // Проверяем, есть ли уже правильное название ветки в начале
        if (currentMessage.startsWith(branchName + " ")) {
            return; // Уже правильный коммит, ничего не делаем
        }

        // Заменяем старую ветку или добавляем новую
        String updatedMessage = replaceExistingBranch(currentMessage, branchName);

        // Обновляем коммит только если текст изменился
        if (!updatedMessage.equals(currentMessage)) {
            checkinPanel.setCommitMessage(updatedMessage);
        }
    }

    /**
     * Метод заменяет старую ветку на новую или добавляет её, если ветки не было.
     */
    private String replaceExistingBranch(String commitMessage, String newBranch) {
        if (commitMessage == null || commitMessage.trim().isEmpty()) {
            return newBranch + " "; // Если коммит пустой, просто вставляем новую ветку
        }

        // Регулярка для поиска имени ветки в начале строки (например, "KL-123 ")
        String branchPattern = "^[A-Z]+-\\d+\\s+";
        Pattern pattern = Pattern.compile(branchPattern);
        Matcher matcher = pattern.matcher(commitMessage);

        if (matcher.find()) {
            // Если в начале уже есть ветка, заменяем её на новую
            return newBranch + " " + commitMessage.substring(matcher.end());
        } else {
            // Если ветки не было, добавляем в начало
            return newBranch + " " + commitMessage;
        }
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

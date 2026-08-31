package kz.kolesa;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.ui.Refreshable;
import kz.kolesa.git.GitBranches;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Кнопка в панели коммита: вставляет имя текущей ветки в начало сообщения. */
public class KolesaCommitMessageAction extends AnAction implements DumbAware {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // Обязателен начиная с 2022.3: без него платформа пишет ошибку в лог,
        // а с 2024.x перестаёт обновлять состояние действия.
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null && messageTarget(e) != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        CheckinProjectPanel panel = checkinPanel(e);
        String branchName = GitBranches.currentBranch(project, panel == null ? null : panel.getRoots());
        if (CommitMessages.shouldSkipPrefix(branchName)) {
            return;
        }

        String currentMessage = currentMessage(e, panel);
        String updatedMessage = CommitMessages.withBranchPrefix(currentMessage, branchName);
        if (updatedMessage.equals(currentMessage)) {
            return;
        }

        CommitMessageI target = messageTarget(e);
        if (target != null) {
            target.setCommitMessage(updatedMessage);
        }
    }

    private static @Nullable String currentMessage(@NotNull AnActionEvent e, @Nullable CheckinProjectPanel panel) {
        if (panel != null) {
            return panel.getCommitMessage();
        }
        Document document = e.getData(VcsDataKeys.COMMIT_MESSAGE_DOCUMENT);
        return document == null ? null : document.getText();
    }

    /**
     * В немодальном окне коммита (по умолчанию во всех IDE с 2020.3)
     * {@code Refreshable.PANEL_KEY} может отсутствовать — тогда работаем через
     * {@link VcsDataKeys#COMMIT_MESSAGE_CONTROL}. Без этого кнопка там молча
     * ничего не делала.
     */
    private static @Nullable CommitMessageI messageTarget(@NotNull AnActionEvent e) {
        CheckinProjectPanel panel = checkinPanel(e);
        if (panel != null) {
            return panel;
        }
        return e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);
    }

    private static @Nullable CheckinProjectPanel checkinPanel(@NotNull AnActionEvent e) {
        Refreshable panel = e.getData(Refreshable.PANEL_KEY);
        return panel instanceof CheckinProjectPanel ? (CheckinProjectPanel) panel : null;
    }
}

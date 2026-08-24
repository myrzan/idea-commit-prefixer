package kz.kolesa;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Правила подстановки имени ветки в сообщение коммита.
 * <p>
 * Вынесены из {@code KolesaCommitMessageAction}, потому что ровно те же
 * проверки нужны и {@link IssueReferenceChecker}: раньше он ругался на ветках
 * {@code main}/{@code master}/{@code develop}, куда кнопка префикс осознанно
 * не ставит.
 */
public final class CommitMessages {

    private static final Set<String> STANDARD_BRANCHES = Set.of("main", "master", "develop");

    private CommitMessages() {
    }

    /** Префикс не ставим: стандартная ветка, detached HEAD или проект без git. */
    public static boolean shouldSkipPrefix(@Nullable String branchName) {
        // Сравнение точное: имена веток в git регистрозависимы, ветка Master —
        // это не master.
        return branchName == null
                || branchName.isBlank()
                || STANDARD_BRANCHES.contains(branchName);
    }

    /** Уже упомянута ли ветка в сообщении. */
    public static boolean mentionsBranch(@Nullable String commitMessage, @NotNull String branchName) {
        return commitMessage != null && commitMessage.contains(branchName);
    }

    /**
     * Сообщение с именем ветки в начале. Идемпотентно: повторный вызов ничего
     * не меняет.
     */
    public static @NotNull String withBranchPrefix(@Nullable String commitMessage, @NotNull String branchName) {
        if (commitMessage == null || commitMessage.isBlank()) {
            return branchName + " ";
        }
        if (mentionsBranch(commitMessage, branchName)) {
            return commitMessage;
        }
        return branchName + " " + commitMessage;
    }
}

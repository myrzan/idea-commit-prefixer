package kz.kolesa;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * Ключ задачи в стиле Jira: {@code KL-123}, {@code ABC-4567}. Ищется в
     * любом месте токена — ветка обычно выглядит как {@code feature/KL-123-fix}.
     * <p>
     * Только верхний регистр и минимум две буквы в коде проекта: иначе
     * устаревшим префиксом считался бы любой {@code utf-8 ...} в начале
     * сообщения.
     * <p>
     * Это единственная регулярка в классе, и она — константа. Имя ветки
     * паттерном не становится никогда: {@code release/1.2+rc} — легальная
     * ветка, но невалидный regex.
     */
    private static final Pattern ISSUE_KEY = Pattern.compile("[A-Z][A-Z0-9]+-\\d+");

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

    /**
     * Уже упомянута ли ветка в сообщении — сама по себе или своим ключом
     * задачи. Ключа достаточно: на ветке {@code feature/KL-123-fix} сообщение
     * {@code "KL-123 fix"} ссылается на ту же задачу, и требовать полное имя
     * ветки здесь нельзя — кнопка такое сообщение тоже не трогает.
     */
    public static boolean mentionsBranch(@Nullable String commitMessage, @NotNull String branchName) {
        if (commitMessage == null) {
            return false;
        }
        if (commitMessage.contains(branchName)) {
            return true;
        }
        String branchIssue = issueKey(branchName);
        return branchIssue != null && commitMessage.contains(branchIssue);
    }

    /**
     * Сообщение с именем ветки в начале.
     * <p>
     * Если сообщение уже начинается с префикса-задачи ({@code KL-100 ...}), он
     * не дублируется: при совпадении задачи с текущей веткой сообщение
     * остаётся как есть, при расхождении устаревший префикс заменяется на
     * текущую ветку. Идемпотентно.
     */
    public static @NotNull String withBranchPrefix(@Nullable String commitMessage, @NotNull String branchName) {
        if (commitMessage == null || commitMessage.isBlank()) {
            return branchName + " ";
        }

        int start = firstNonWhitespace(commitMessage);
        int end = tokenEnd(commitMessage, start);
        String leadingToken = commitMessage.substring(start, end);

        if (issueKey(leadingToken) != null) {
            if (leadingToken.equals(branchName) || sameIssue(leadingToken, branchName)) {
                return commitMessage;
            }
            // Работаем срезами, а не replaceFirst: имя ветки — не регулярка.
            String rest = commitMessage.substring(end);
            return rest.isBlank() ? branchName + " " : branchName + rest;
        }

        if (mentionsBranch(commitMessage, branchName)) {
            return commitMessage;
        }
        return branchName + " " + commitMessage;
    }

    /** Ссылаются ли обе строки на одну и ту же задачу. */
    private static boolean sameIssue(@NotNull String first, @NotNull String second) {
        String firstKey = issueKey(first);
        return firstKey != null && firstKey.equals(issueKey(second));
    }

    private static @Nullable String issueKey(@NotNull String text) {
        Matcher matcher = ISSUE_KEY.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private static int firstNonWhitespace(@NotNull String text) {
        int index = 0;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private static int tokenEnd(@NotNull String text, int from) {
        int index = from;
        while (index < text.length() && !Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }
}

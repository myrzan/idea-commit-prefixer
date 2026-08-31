package kz.kolesa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommitMessagesTest {

    @Test
    void skipsStandardBranches() {
        assertTrue(CommitMessages.shouldSkipPrefix("main"));
        assertTrue(CommitMessages.shouldSkipPrefix("master"));
        assertTrue(CommitMessages.shouldSkipPrefix("develop"));
        assertFalse(CommitMessages.shouldSkipPrefix("Master"));
        assertFalse(CommitMessages.shouldSkipPrefix("KL-123"));
        assertFalse(CommitMessages.shouldSkipPrefix("feature/KL-123-fix"));
    }

    /** Detached HEAD и проекты без git отдают null — префикс ставить не из чего. */
    @Test
    void skipsMissingBranch() {
        assertTrue(CommitMessages.shouldSkipPrefix(null));
        assertTrue(CommitMessages.shouldSkipPrefix("  "));
    }

    @Test
    void prependsBranchName() {
        assertEquals("KL-123 fix search", CommitMessages.withBranchPrefix("fix search", "KL-123"));
    }

    @Test
    void fillsEmptyMessage() {
        assertEquals("KL-123 ", CommitMessages.withBranchPrefix("", "KL-123"));
        assertEquals("KL-123 ", CommitMessages.withBranchPrefix(null, "KL-123"));
        assertEquals("KL-123 ", CommitMessages.withBranchPrefix("   ", "KL-123"));
    }

    @Test
    void isIdempotent() {
        String once = CommitMessages.withBranchPrefix("fix search", "KL-123");
        assertEquals(once, CommitMessages.withBranchPrefix(once, "KL-123"));

        String branchStyle = CommitMessages.withBranchPrefix("fix search", "feature/KL-123-fix");
        assertEquals("feature/KL-123-fix fix search", branchStyle);
        assertEquals(branchStyle, CommitMessages.withBranchPrefix(branchStyle, "feature/KL-123-fix"));
    }

    @Test
    void keepsMessageThatAlreadyMentionsBranchAnywhere() {
        assertEquals("fix search, see KL-123",
                CommitMessages.withBranchPrefix("fix search, see KL-123", "KL-123"));
    }

    /** Главный кейс: в поле остался префикс от предыдущей ветки. */
    @Test
    void replacesStaleIssuePrefix() {
        assertEquals("KL-200 fix search",
                CommitMessages.withBranchPrefix("KL-100 fix search", "KL-200"));
        assertEquals("KL-200 fix search",
                CommitMessages.withBranchPrefix("ABC-1 fix search", "KL-200"));
        assertEquals("feature/KL-200-new fix search",
                CommitMessages.withBranchPrefix("feature/KL-100-old fix search", "feature/KL-200-new"));
        assertEquals("KL-200 fix search",
                CommitMessages.withBranchPrefix("feature/KL-100-old fix search", "KL-200"));
    }

    /** Префикс от той же задачи — сообщение не трогаем, даже в другой форме. */
    @Test
    void keepsPrefixOfSameIssue() {
        assertEquals("KL-123 fix search",
                CommitMessages.withBranchPrefix("KL-123 fix search", "KL-123"));
        assertEquals("KL-123 fix search",
                CommitMessages.withBranchPrefix("KL-123 fix search", "feature/KL-123-fix"));
        assertEquals("feature/KL-123-fix fix search",
                CommitMessages.withBranchPrefix("feature/KL-123-fix fix search", "KL-123"));
    }

    /** Замена префикса тоже идемпотентна. */
    @Test
    void replacementIsIdempotent() {
        String once = CommitMessages.withBranchPrefix("KL-100 fix search", "KL-200");
        assertEquals(once, CommitMessages.withBranchPrefix(once, "KL-200"));
    }

    @Test
    void replacesPrefixOnlyMessage() {
        assertEquals("KL-200 ", CommitMessages.withBranchPrefix("KL-100", "KL-200"));
        assertEquals("KL-200 ", CommitMessages.withBranchPrefix("KL-100   ", "KL-200"));
        assertEquals("KL-200", CommitMessages.withBranchPrefix("KL-200", "KL-200"));
    }

    @Test
    void ignoresLeadingWhitespaceBeforeStalePrefix() {
        assertEquals("KL-200 fix search",
                CommitMessages.withBranchPrefix("  KL-100 fix search", "KL-200"));
    }

    /** Обычное слово в начале — это не префикс, его нельзя затирать. */
    @Test
    void doesNotTouchOrdinaryFirstWord() {
        assertEquals("KL-200 fix search", CommitMessages.withBranchPrefix("fix search", "KL-200"));
        assertEquals("KL-200 utf-8 encoding", CommitMessages.withBranchPrefix("utf-8 encoding", "KL-200"));
        assertEquals("KL-200 e-2 fix", CommitMessages.withBranchPrefix("e-2 fix", "KL-200"));
    }

    /** Ветка без ключа задачи: заменять нечем, но и устаревший префикс не остаётся. */
    @Test
    void handlesBranchWithoutIssueKey() {
        assertEquals("release/1.2+rc fix search",
                CommitMessages.withBranchPrefix("fix search", "release/1.2+rc"));
        assertEquals("release/1.2+rc fix search",
                CommitMessages.withBranchPrefix("KL-100 fix search", "release/1.2+rc"));
        assertEquals("release/1.2+rc fix search",
                CommitMessages.withBranchPrefix("release/1.2+rc fix search", "release/1.2+rc"));
    }

    @Test
    void detectsMention() {
        assertTrue(CommitMessages.mentionsBranch("KL-123 fix", "KL-123"));
        assertFalse(CommitMessages.mentionsBranch("fix", "KL-123"));
        assertFalse(CommitMessages.mentionsBranch(null, "KL-123"));
        assertFalse(CommitMessages.mentionsBranch("KL-100 fix", "KL-123"));
    }

    /** Ключа задачи достаточно — иначе checker ругался бы на то, что кнопка не правит. */
    @Test
    void detectsMentionByIssueKeyOfBranch() {
        assertTrue(CommitMessages.mentionsBranch("KL-123 fix", "feature/KL-123-fix"));
        assertFalse(CommitMessages.mentionsBranch("KL-100 fix", "feature/KL-123-fix"));
    }
}

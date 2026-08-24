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
    }

    @Test
    void keepsMessageThatAlreadyMentionsBranchAnywhere() {
        assertEquals("fix search, see KL-123",
                CommitMessages.withBranchPrefix("fix search, see KL-123", "KL-123"));
    }

    @Test
    void detectsMention() {
        assertTrue(CommitMessages.mentionsBranch("KL-123 fix", "KL-123"));
        assertFalse(CommitMessages.mentionsBranch("fix", "KL-123"));
        assertFalse(CommitMessages.mentionsBranch(null, "KL-123"));
    }
}

package kz.kolesa.git;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Текущая ветка через API бандла Git4Idea.
 * <p>
 * Раньше имя ветки читалось прямо с диска из {@code <vcs-root>/.git/HEAD}.
 * Так не работали git worktree и submodule (там {@code .git} — файл, а не
 * каталог), rebase/detached HEAD отдавали мусор, а обращение к файловой
 * системе шло из EDT.
 */
public final class GitBranches {

    private GitBranches() {
    }

    /** Ветка корня, попавшего в коммит; при неоднозначности — первый git-репозиторий проекта. */
    public static @Nullable String currentBranch(@Nullable Project project,
                                                 @Nullable Collection<VirtualFile> roots) {
        if (project == null || project.isDisposed()) {
            return null;
        }

        List<GitRepository> repositories = GitRepositoryManager.getInstance(project).getRepositories();
        if (repositories.isEmpty()) {
            return null;
        }

        if (roots != null && !roots.isEmpty()) {
            for (GitRepository repository : repositories) {
                if (containsRoot(roots, repository.getRoot())) {
                    // null в detached HEAD и во время rebase — тогда префикс не трогаем.
                    return repository.getCurrentBranchName();
                }
            }
        }
        return repositories.get(0).getCurrentBranchName();
    }

    private static boolean containsRoot(@NotNull Collection<VirtualFile> roots, @NotNull VirtualFile repoRoot) {
        for (VirtualFile root : roots) {
            if (root != null && (root.equals(repoRoot) || VfsUtilCore.isAncestor(repoRoot, root, false))) {
                return true;
            }
        }
        return false;
    }
}

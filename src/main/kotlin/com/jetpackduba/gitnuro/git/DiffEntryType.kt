package com.jetpackduba.gitnuro.git

import com.jetpackduba.gitnuro.extensions.filePath
import com.jetpackduba.gitnuro.extensions.toStatusType
import com.jetpackduba.gitnuro.git.workspace.StatusEntry
import com.jetpackduba.gitnuro.git.workspace.StatusType
import org.eclipse.jgit.diff.DiffEntry

sealed class DiffEntryType {
    class CommitDiff(val diffEntry: DiffEntry) : DiffEntryType() {
        override val filePath: String
            get() = diffEntry.filePath

        override val statusType: StatusType
            get() = diffEntry.toStatusType()
    }

    sealed class UncommitedDiff(val statusEntry: StatusEntry) : DiffEntryType() {
        override val filePath: String
            get() = statusEntry.filePath

        override val statusType: StatusType
            get() = statusEntry.statusType
    }

    sealed class UnstagedDiff(statusEntry: StatusEntry) : UncommitedDiff(statusEntry)
    sealed class StagedDiff(statusEntry: StatusEntry) : UncommitedDiff(statusEntry)

    /**
     * State used to represent staged changes when the repository state is not [org.eclipse.jgit.lib.RepositoryState.SAFE]
     */
    class UnsafeStagedDiff(statusEntry: StatusEntry) : StagedDiff(statusEntry)

    /**
     * State used to represent unstaged changes when the repository state is not [org.eclipse.jgit.lib.RepositoryState.SAFE]
     */
    class UnsafeUnstagedDiff(statusEntry: StatusEntry) : UnstagedDiff(statusEntry)

    class SafeStagedDiff(statusEntry: StatusEntry) : StagedDiff(statusEntry)
    class SafeUnstagedDiff(statusEntry: StatusEntry) : UnstagedDiff(statusEntry)

    abstract val filePath: String
    abstract val statusType: StatusType

}

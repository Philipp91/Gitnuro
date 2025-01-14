package com.jetpackduba.gitnuro.git.diff

import com.jetpackduba.gitnuro.extensions.lineAt
import com.jetpackduba.gitnuro.git.EntryContent
import com.jetpackduba.gitnuro.git.RawFileManager
import org.eclipse.jgit.diff.*
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.patch.FileHeader
import org.eclipse.jgit.patch.FileHeader.PatchType
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InvalidObjectException
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts
import kotlin.math.max
import kotlin.math.min

private const val CONTEXT_LINES = 3

/**
 * Generator of [Hunk] lists from [DiffEntry]
 */
class HunkDiffGenerator @Inject constructor(
    private val rawFileManager: RawFileManager,
) {
    fun format(
        repository: Repository,
        diffEntry: DiffEntry,
        oldTreeIterator: AbstractTreeIterator?,
        newTreeIterator: AbstractTreeIterator?,
    ): DiffResult {
        val outputStream = ByteArrayOutputStream() // Dummy output stream used for the diff formatter
        return outputStream.use {
            val diffFormatter = DiffFormatter(outputStream).apply {
                setRepository(repository)
            }

            if (oldTreeIterator != null && newTreeIterator != null) {
                diffFormatter.scan(oldTreeIterator, newTreeIterator)
            }

            val fileHeader = diffFormatter.toFileHeader(diffEntry)

            val rawOld = rawFileManager.getRawContent(
                repository,
                DiffEntry.Side.OLD,
                diffEntry,
                oldTreeIterator,
                newTreeIterator
            )
            val rawNew = rawFileManager.getRawContent(
                repository,
                DiffEntry.Side.NEW,
                diffEntry,
                oldTreeIterator,
                newTreeIterator
            )

            if (rawOld == EntryContent.InvalidObjectBlob || rawNew == EntryContent.InvalidObjectBlob)
                throw InvalidObjectException("Invalid object in diff format")

            var diffResult: DiffResult = DiffResult.Text(diffEntry, emptyList())

            // If we can, generate text diff (if one of the files has never been a binary file)
            val hasGeneratedTextDiff = canGenerateTextDiff(rawOld, rawNew) { oldRawText, newRawText ->
                diffResult = DiffResult.Text(diffEntry, format(fileHeader, oldRawText, newRawText))
            }

            if (!hasGeneratedTextDiff) {
                diffResult = DiffResult.NonText(diffEntry, rawOld, rawNew)
            }

            return@use diffResult
        }
    }

    @OptIn(ExperimentalContracts::class)
    private fun canGenerateTextDiff(
        rawOld: EntryContent,
        rawNew: EntryContent,
        onText: (oldRawText: RawText, newRawText: RawText) -> Unit
    ): Boolean {

        val rawOldText = when (rawOld) {
            is EntryContent.Text -> rawOld.rawText
            EntryContent.Missing -> RawText.EMPTY_TEXT
            else -> null
        }

        val newOldText = when (rawNew) {
            is EntryContent.Text -> rawNew.rawText
            EntryContent.Missing -> RawText.EMPTY_TEXT
            else -> null
        }

        return if (rawOldText != null && newOldText != null) {
            onText(rawOldText, newOldText)
            true
        } else
            false
    }

    /**
     * Given a [FileHeader] and the both [RawText], generate a [List] of [Hunk]
     */
    private fun format(head: FileHeader, oldRawText: RawText, newRawText: RawText): List<Hunk> {
        return if (head.patchType == PatchType.UNIFIED)
            format(head.toEditList(), oldRawText, newRawText)
        else
            emptyList()
    }

    private fun format(edits: EditList, oldRawText: RawText, newRawText: RawText): List<Hunk> {
        var curIdx = 0
        val hunksList = mutableListOf<Hunk>()
        while (curIdx < edits.size) {
            var curEdit = edits[curIdx]
            val endIdx = findCombinedEnd(edits, curIdx)
            val endEdit = edits[endIdx]
            var oldCurrentLine = max(0, curEdit.beginA - CONTEXT_LINES)
            var newCurrentLine = max(0, curEdit.beginB - CONTEXT_LINES)
            val oldEndLine = min(oldRawText.size(), endEdit.endA + CONTEXT_LINES)
            val newEndLine = min(newRawText.size(), endEdit.endB + CONTEXT_LINES)

            val headerText = createHunkHeader(oldCurrentLine, oldEndLine, newCurrentLine, newEndLine)
            val lines = mutableListOf<Line>()

            while (oldCurrentLine < oldEndLine || newCurrentLine < newEndLine) {
                if (oldCurrentLine < curEdit.beginA || endIdx + 1 < curIdx) {
                    var lineText = oldRawText.lineAt(oldCurrentLine)

                    if (
                        oldCurrentLine < oldRawText.size() - 1 || // If it's not the last
                        (oldCurrentLine == oldRawText.size() - 1 && !oldRawText.isMissingNewlineAtEnd) // Or is the last and contains new line at the end
                    ) {
                        lineText += oldRawText.lineDelimiter.orEmpty()
                    }

                    lines.add(Line(lineText, oldCurrentLine, newCurrentLine, LineType.CONTEXT))

                    oldCurrentLine++
                    newCurrentLine++
                } else if (oldCurrentLine < curEdit.endA) {
                    var lineText = oldRawText.lineAt(oldCurrentLine)

                    if (
                        oldCurrentLine < oldRawText.size() - 1 || // If it's not the last
                        (oldCurrentLine == oldRawText.size() - 1 && !oldRawText.isMissingNewlineAtEnd) // Or is the last and contains new line at the end
                    ) {
                        lineText += oldRawText.lineDelimiter.orEmpty()
                    }

                    lines.add(Line(lineText, oldCurrentLine, newCurrentLine, LineType.REMOVED))

                    oldCurrentLine++
                } else if (newCurrentLine < curEdit.endB) {
                    var lineText = newRawText.lineAt(newCurrentLine)

                    if (
                        newCurrentLine < newRawText.size() - 1 || // If it's not the last
                        (newCurrentLine == newRawText.size() - 1 && !newRawText.isMissingNewlineAtEnd) // Or is the last and contains new line at the end
                    ) {
                        lineText += newRawText.lineDelimiter.orEmpty()
                    }

                    lines.add(Line(lineText, oldCurrentLine, newCurrentLine, LineType.ADDED))

                    newCurrentLine++
                }

                if (end(curEdit, oldCurrentLine, newCurrentLine) && ++curIdx < edits.size) curEdit = edits[curIdx]
            }

            hunksList.add(Hunk(headerText, lines))
        }

        return hunksList
    }

    /**
     * Generates the hunk's header string like in git diff
     */
    @Throws(IOException::class)
    private fun createHunkHeader(
        oldStartLine: Int,
        oldEndLine: Int,
        newStartLine: Int,
        newEndLine: Int
    ): String {
        val prefix = "@@"
        val contentRemoved = createRange('-', oldStartLine + 1, oldEndLine - oldStartLine)
        val contentAdded = createRange('+', newStartLine + 1, newEndLine - newStartLine)
        val suffix = " @@"

        return prefix + contentRemoved + contentAdded + suffix
    }

    private fun createRange(symbol: Char, begin: Int, linesCount: Int): String {
        return when (linesCount) {
            0 -> " $symbol${begin - 1},0"
            1 -> " $symbol$begin" // If the range is exactly one line, produce only the number.
            else -> " $symbol$begin,$linesCount"
        }
    }

    private fun findCombinedEnd(edits: List<Edit>, i: Int): Int {
        var end = i + 1
        while (end < edits.size
            && (combineA(edits, end) || combineB(edits, end))
        ) end++
        return end - 1
    }

    private fun combineA(e: List<Edit>, i: Int): Boolean {
        return e[i].beginA - e[i - 1].endA <= 2 * CONTEXT_LINES
    }

    private fun combineB(e: List<Edit>, i: Int): Boolean {
        return e[i].beginB - e[i - 1].endB <= 2 * CONTEXT_LINES
    }

    private fun end(edit: Edit, a: Int, b: Int): Boolean {
        return edit.endA <= a && edit.endB <= b
    }
}

sealed class DiffResult(
    val diffEntry: DiffEntry,
) {
    class Text(
        diffEntry: DiffEntry,
        val hunks: List<Hunk>
    ) : DiffResult(diffEntry)

    class TextSplit(
        diffEntry: DiffEntry,
        val hunks: List<SplitHunk>
    ) : DiffResult(diffEntry)

    class NonText(
        diffEntry: DiffEntry,
        val oldBinaryContent: EntryContent,
        val newBinaryContent: EntryContent,
    ) : DiffResult(diffEntry)
}
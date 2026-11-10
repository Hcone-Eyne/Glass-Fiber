package com.fox.mobile.ui.home

import com.fox.mobile.ui.depth.RIDGE_NORMALIZED_POINTS
import com.fox.mobile.ui.depth.WallpaperBox
import com.fox.mobile.ui.depth.ridgeYAtScreenX
import java.util.Calendar

/**
 * WordItem represents one daily reflection, quote, or reminder shown under the mountain ridge.
 *
 * Extracted from `docs/prototypes/Fox_Line_v30.html` lines 775-779:
 * - k: kind / category
 * - t: main text / quote
 * - by: author or biblical reference (null if reminder)
 */
data class WordItem(
    val kind: String,
    val text: String,
    val attribution: String? = null
) {
    /**
     * Formats the attribution line as in the prototype:
     * `$('#wBy').textContent = w.by ? `— ${w.by} · ${w.k}` : w.k`
     */
    val formattedAttribution: String
        get() = if (!attribution.isNullOrBlank()) "— $attribution · $kind" else kind

    /**
     * Formats accessible screen reader label as in the prototype:
     * `${w.k}: ${w.t} ${w.by || ''}. Tap for the next one.`
     */
    val contentDescription: String
        get() = "$kind: $text ${attribution ?: ""}. Tap for the next one."
}

/**
 * WordsRepository provides access to the daily reflection items.
 *
 * Current implementation contains the exact 3 prototype sample items from Fox_Line_v30.html.
 * Phase 13 will replace/augment this with Google Keep note integration.
 */
interface WordsRepository {
    fun getWords(): List<WordItem>
    fun getInitialIndex(hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): Int
    fun getWordAt(index: Int): WordItem
    fun getNextWord(currentIndex: Int): Pair<Int, WordItem>
}

object SampleWordsRepository : WordsRepository {
    /**
     * Exact WORDS constant from docs/prototypes/Fox_Line_v30.html (lines 775-779):
     *
     * const WORDS = [
     *   { k: 'Your verse', t: 'I can do all things through Christ which strengtheneth me.', by: 'Philippians 4:13' },
     *   { k: 'Saved quote', t: 'Stay hungry. Stay foolish.', by: 'Steve Jobs' },
     *   { k: 'Fox reminder', t: 'Revise unit 3 tonight. Your phone goes quiet at 1 AM.' }
     * ];
     */
    val SAMPLE_WORDS: List<WordItem> = listOf(
        WordItem(
            kind = "Your verse",
            text = "I can do all things through Christ which strengtheneth me.",
            attribution = "Philippians 4:13"
        ),
        WordItem(
            kind = "Saved quote",
            text = "Stay hungry. Stay foolish.",
            attribution = "Steve Jobs"
        ),
        WordItem(
            kind = "Fox reminder",
            text = "Revise unit 3 tonight. Your phone goes quiet at 1 AM.",
            attribution = null
        )
    )

    override fun getWords(): List<WordItem> = SAMPLE_WORDS

    /**
     * Time-of-day initial word selection from prototype line 780:
     * `let wi = (h => h < 12 ? 0 : h < 18 ? 1 : 2)(new Date().getHours());`
     *
     * - Before 12:00 PM (noon): Verse (index 0)
     * - 12:00 PM - 6:00 PM: Saved quote (index 1)
     * - After 6:00 PM: Fox reminder (index 2)
     */
    override fun getInitialIndex(hourOfDay: Int): Int {
        return when {
            hourOfDay < 12 -> 0
            hourOfDay < 18 -> 1
            else -> 2
        }
    }

    override fun getWordAt(index: Int): WordItem {
        val safeIndex = index.mod(SAMPLE_WORDS.size)
        return SAMPLE_WORDS[safeIndex]
    }

    override fun getNextWord(currentIndex: Int): Pair<Int, WordItem> {
        val nextIndex = (currentIndex + 1) % SAMPLE_WORDS.size
        return nextIndex to SAMPLE_WORDS[nextIndex]
    }
}

/**
 * Candidate fit configuration for the words block under the mountain ridge.
 *
 * @param maxWidthFraction Width of words container as fraction of work block width
 * @param fontSizeSp Font size in sp for the quote / reflection text
 */
data class WordsFitOption(
    val maxWidthFraction: Float,
    val fontSizeSp: Float
)

/**
 * Ordered fit candidates from docs/prototypes/Fox_Line_v30.html line 788:
 * [[68, 16], [80, 16], [92, 16], [92, 15], [92, 14]]
 */
val WORDS_FIT_CANDIDATES = listOf(
    WordsFitOption(0.68f, 16f),
    WordsFitOption(0.80f, 16f),
    WordsFitOption(0.92f, 16f),
    WordsFitOption(0.92f, 15f),
    WordsFitOption(0.92f, 14f)
)

/**
 * Computes the maximum screen Y coordinate (i.e. lowest mountain ridge point on screen)
 * within the horizontal span [leftPx - 8f .. rightPx + 8f].
 *
 * Checks all ridge points whose screen X falls in the padded span, plus the interpolated
 * ridge Y values at the span endpoints.
 */
fun maxRidgeYInSpan(leftPx: Float, rightPx: Float, box: WallpaperBox): Float {
    val spanLeft = minOf(leftPx, rightPx) - 8f
    val spanRight = maxOf(leftPx, rightPx) + 8f

    var maxY = maxOf(
        ridgeYAtScreenX(spanLeft, box),
        ridgeYAtScreenX(spanRight, box)
    )

    for (pt in RIDGE_NORMALIZED_POINTS) {
        val screenX = box.left + pt.x * box.iw
        if (screenX in spanLeft..spanRight) {
            val screenY = box.top + pt.y * box.ih
            if (screenY > maxY) {
                maxY = screenY
            }
        }
    }
    return maxY
}

/**
 * Pure Kotlin algorithm selecting the best fitting (maxWidthFraction, fontSizeSp)
 * combination so that the words text block sits comfortably under the mountain ridge.
 *
 * Prototype reference: `fitWords()` in `Fox_Line_v30.html` (lines 784-794).
 *
 * Tries candidates in order:
 * 1. 68% width at 16sp
 * 2. 80% width at 16sp
 * 3. 92% width at 16sp
 * 4. 92% width at 15sp
 * 5. 92% width at 14sp
 *
 * For each candidate:
 * - wPx = workBlockWidthPx * maxWidthFraction
 * - hPx = measureHeightPx(wPx, fontSizeSp)
 * - wordsBottomPx = workTopPx - gapAboveWorkPx
 * - wordsTopPx = wordsBottomPx - hPx
 * - maxRidgeY = maxRidgeYInSpan(leftEdgePx, leftEdgePx + wPx, box)
 *
 * Returns the first candidate where wordsTopPx >= maxRidgeY + minClearanceBelowRidgePx,
 * or the last candidate if all candidates are tight.
 */
fun selectWordsFit(
    workTopPx: Float,
    gapAboveWorkPx: Float,
    minClearanceBelowRidgePx: Float,
    leftEdgePx: Float,
    workBlockWidthPx: Float,
    box: WallpaperBox,
    measureHeightPx: (maxWidthPx: Float, fontSizeSp: Float) -> Float
): WordsFitOption {
    for (candidate in WORDS_FIT_CANDIDATES) {
        val wPx = workBlockWidthPx * candidate.maxWidthFraction
        val hPx = measureHeightPx(wPx, candidate.fontSizeSp)
        val wordsBottomPx = workTopPx - gapAboveWorkPx
        val wordsTopPx = wordsBottomPx - hPx
        val maxRidgeY = maxRidgeYInSpan(leftEdgePx, leftEdgePx + wPx, box)
        if (wordsTopPx >= maxRidgeY + minClearanceBelowRidgePx) {
            return candidate
        }
    }
    return WORDS_FIT_CANDIDATES.last()
}

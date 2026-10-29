package com.example.engine

import com.example.model.TaskItem
import java.util.Locale
import kotlin.math.max

class TaskDuplicateDetector {

    fun findPotentialDuplicates(candidateTitle: String, activeTasks: List<TaskItem>, currentTaskId: Long = 0): List<TaskItem> {
        val trimmedCandidate = candidateTitle.trim().lowercase(Locale.ROOT)
        if (trimmedCandidate.length < 3) return emptyList()

        return activeTasks.filter { task ->
            if (task.id == currentTaskId) return@filter false
            val existing = task.title.trim().lowercase(Locale.ROOT)
            if (existing == trimmedCandidate) return@filter true

            // Check token overlap (e.g. "Pay electric bill" vs "Pay electricity bills")
            val candidateTokens = trimmedCandidate.split(Regex("[\\s,._-]+")).filter { it.length > 2 }.toSet()
            val existingTokens = existing.split(Regex("[\\s,._-]+")).filter { it.length > 2 }.toSet()
            if (candidateTokens.isNotEmpty() && existingTokens.isNotEmpty()) {
                val intersection = candidateTokens.intersect(existingTokens).size
                val union = candidateTokens.union(existingTokens).size
                val jaccard = intersection.toFloat() / union.toFloat()
                if (jaccard >= 0.5f) return@filter true
            }

            // String distance similarity
            val sim = stringSimilarity(trimmedCandidate, existing)
            sim >= 0.72f
        }
    }

    private fun stringSimilarity(s1: String, s2: String): Float {
        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 1.0f
        val dist = levenshteinDistance(s1, s2)
        return (maxLen - dist).toFloat() / maxLen.toFloat()
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}

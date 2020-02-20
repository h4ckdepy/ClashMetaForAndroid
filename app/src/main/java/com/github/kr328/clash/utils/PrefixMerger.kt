package com.github.kr328.clash.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PrefixMerger {
    private val REGEX_PREFIX_TRIM = Regex("[-]*$")

    data class Result<T>(val prefix: String, val content: String, val value: T)

    suspend fun <T> merge(values: List<T>, transform: (T) -> String): List<Result<T>> =
        withContext(Dispatchers.Default) {
            val pairs = values.map {
                transform(it).trim() to it
            }

            val groups = mutableListOf<List<Pair<String, T>>>()
            var mergingGroup = mutableListOf<Pair<String, T>>()
            var currentCodePoint = 0
            val result = mutableListOf<Result<T>>()

            for (pair in pairs) {
                if (pair.first.codePointAt(0) == currentCodePoint) {
                    mergingGroup.add(pair)
                } else {
                    if (mergingGroup.isNotEmpty()) {
                        groups.add(mergingGroup)
                        mergingGroup = mutableListOf()
                    }

                    currentCodePoint = pair.first.codePointAt(0)
                    mergingGroup.add(pair)
                }
            }

            if (mergingGroup.isNotEmpty())
                groups.add(mergingGroup)

            for (group in groups) {
                var diffIndex = 0
                val size = group.map { it.first.length }.min() ?: 0

                diff@ for (charIndex in 0 until size) {
                    for (stringIndex in 0 until (group.size - 1)) {
                        if (group[stringIndex].first[charIndex] != group[stringIndex + 1].first[charIndex])
                            break@diff
                    }

                    diffIndex++
                }

                group.forEach {
                    val prefix = it.first.substring(0, diffIndex)
                        .replace(REGEX_PREFIX_TRIM, "")
                    val content = it.first.substring(diffIndex)

                    result.add(Result(prefix, content, it.second))
                }
            }

            result
        }
}
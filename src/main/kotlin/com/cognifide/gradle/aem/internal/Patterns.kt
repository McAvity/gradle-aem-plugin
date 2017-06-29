package com.cognifide.gradle.aem.internal

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOCase
import java.io.File

object Patterns {

    fun wildcard(file: File, matchers: Collection<String>): Boolean {
        return matchers.any { wildcard(file, it) }
    }

    fun wildcard(file: File, matcher: String): Boolean {
        return wildcard(normalizePath(file.absolutePath), matcher)
    }

    fun wildcard(path: String, matcher: String): Boolean {
        return FilenameUtils.wildcardMatch(path, matcher, IOCase.INSENSITIVE)
    }

    fun wildcardSensitive(path: String, matcher: String): Boolean {
        return FilenameUtils.wildcardMatch(path, matcher, IOCase.SENSITIVE)
    }

    private fun normalizePath(path: String): String {
        return path.replace("\\", "/")
    }

}
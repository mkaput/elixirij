package dev.murek.elixirij.testing

import junit.framework.TestCase

val TestCase.fixtureName: String
    get() = name.orEmpty()
        .removePrefix("test")
        .trim()
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .trim('_')
        .also { check(it.isNotEmpty()) { "Fixture name is empty after sanitization: '$name'" } }

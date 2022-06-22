@file:OptIn(ExperimentalContracts::class)

package com.trifork.timandroid.testHelpers

import org.junit.*
import kotlin.contracts.*


fun Boolean.assertTrue() {
    Assert.assertTrue(this)
}

fun Boolean.assertFalse() {
    Assert.assertFalse(this)
}

fun <T> Comparable<T>?.assert(expected: T) {
    Assert.assertEquals(expected, this)
}

fun Any?.assertNotNull() {
    contract {
        returns() implies (this@assertNotNull != null)
    }
    Assert.assertNotNull(this)
}


fun Any?.assertNull() {
    contract {
        returns() implies (this@assertNull == null)
    }
    Assert.assertNull(this)
}

fun ByteArray.assert(expected: ByteArray) {
    this.size.assert(expected.size)
    forEachIndexed { index, byte ->
        byte.assert(expected[index])
    }
}

inline fun <reified T> Any.assertIs() {
    contract {
        returns() implies (this@assertIs is T)
    }
    Assert.assertEquals(T::class.java, this::class.java)
}
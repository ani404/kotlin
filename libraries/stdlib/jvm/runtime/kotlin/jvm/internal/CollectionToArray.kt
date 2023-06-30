/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("CollectionToArray")

package kotlin.jvm.internal

// TODO: eventually should become internal @PublishedApi
@Deprecated("This function will be removed in a future release")
@DeprecatedSinceKotlin(warningSince = "1.9")
@JvmName("toArray")
fun collectionToArray(collection: Collection<*>): Array<Any?> = copyToArrayImpl(collection)

// Note: Array<Any?> here can have any reference array JVM type at run time
// TODO: eventually should become internal @PublishedApi
@Deprecated("This function will be removed in a future release")
@DeprecatedSinceKotlin(warningSince = "1.9")
@JvmName("toArray")
fun collectionToArray(collection: Collection<*>, a: Array<Any?>?): Array<Any?> = copyToArrayImpl(collection, a!!)
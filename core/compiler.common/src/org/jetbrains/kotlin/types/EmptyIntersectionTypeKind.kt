/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

enum class EmptyIntersectionTypeKind { NOT_EMPTY_INTERSECTION, MULTIPLE_CLASSES, SINGLE_FINAL_CLASS }

fun EmptyIntersectionTypeKind.isDefinitelyEmpty(): Boolean = this == EmptyIntersectionTypeKind.MULTIPLE_CLASSES

fun EmptyIntersectionTypeKind.isPossibleEmpty(): Boolean = this == EmptyIntersectionTypeKind.SINGLE_FINAL_CLASS

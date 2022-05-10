/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyse
import org.jetbrains.kotlin.analysis.api.tokens.KtAlwaysAccessibleLifetimeTokenFactory
import org.jetbrains.kotlin.psi.KtElement

internal inline fun <R> analyseForLightClasses(context: KtElement, action: KtAnalysisSession.() -> R): R =
    analyse(context, KtAlwaysAccessibleLifetimeTokenFactory, action)
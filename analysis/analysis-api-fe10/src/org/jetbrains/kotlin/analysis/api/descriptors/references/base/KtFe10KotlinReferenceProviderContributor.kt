/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.references.base

import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.analysis.api.descriptors.references.*
import org.jetbrains.kotlin.idea.references.KotlinPsiReferenceRegistrar
import org.jetbrains.kotlin.idea.references.KotlinReferenceProviderContributor
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.references.ReferenceAccess

class KtFe10KotlinReferenceProviderContributor : KotlinReferenceProviderContributor {
    override fun registerReferenceProviders(registrar: KotlinPsiReferenceRegistrar) {
        with(registrar) {
            registerProvider(factory = ::KtFe10SimpleNameReference)
            registerProvider(factory = ::KtFe10ForLoopInReference)
            registerProvider(factory = ::KtFe10InvokeFunctionReference)
            registerProvider(factory = ::KtFe10PropertyDelegationMethodsReference)
            registerProvider(factory = ::KtFe10DestructuringDeclarationEntry)
            registerProvider(factory = ::KtFe10ArrayAccessReference)
            registerProvider(factory = ::KtFe10ConstructorDelegationReference)
            registerProvider(factory = ::KtFe10CollectionLiteralReference)
            registerProvider(factory = ::Fe10KDocReference)

            registerMultiProvider<KtNameReferenceExpression> { nameReferenceExpression ->
                if (nameReferenceExpression.getReferencedNameElementType() != KtTokens.IDENTIFIER) {
                    return@registerMultiProvider PsiReference.EMPTY_ARRAY
                }
                if (nameReferenceExpression.parents.any { it is KtImportDirective || it is KtPackageDirective || it is KtUserType }) {
                    return@registerMultiProvider PsiReference.EMPTY_ARRAY
                }

                when (nameReferenceExpression.readWriteAccess(useResolveForReadWrite = false)) {
                    ReferenceAccess.READ ->
                        arrayOf(Fe10SyntheticPropertyAccessorReference(nameReferenceExpression, getter = true))
                    ReferenceAccess.WRITE ->
                        arrayOf(Fe10SyntheticPropertyAccessorReference(nameReferenceExpression, getter = false))
                    ReferenceAccess.READ_WRITE ->
                        arrayOf(
                            Fe10SyntheticPropertyAccessorReference(nameReferenceExpression, getter = true),
                            Fe10SyntheticPropertyAccessorReference(nameReferenceExpression, getter = false)
                        )
                }
            }
        }
    }
}
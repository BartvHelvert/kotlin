/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.swift

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Version
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.jetbrains.cidr.apple.gradle.AppleProjectDataService
import com.jetbrains.cidr.apple.gradle.AppleProjectModel
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContextUtil
import com.jetbrains.cidr.lang.toolchains.CidrCompilerSwitches
import com.jetbrains.cidr.lang.workspace.headerRoots.HeadersSearchPath
import com.jetbrains.cidr.xcode.frameworks.ApplePlatform
import com.jetbrains.cidr.xcode.frameworks.AppleSdkManager
import com.jetbrains.swift.codeinsight.resolve.MobileSwiftModuleManager
import com.jetbrains.swift.codeinsight.resolve.module.ModuleInfo
import com.jetbrains.swift.codeinsight.resolve.module.SwiftCustomIncludePathProvider
import com.jetbrains.swift.lang.SwiftNames
import com.jetbrains.swift.lang.parser.SwiftFileType
import java.io.File
import java.util.*

class MobileSwiftSourceKitCompileArgumentsCollector private constructor() :
    SwiftSourceKitCompileArgumentsCollector<MobileSwiftSourceKitCompileArgumentsCollector.Info>() {
    override fun collectInformation(file: PsiFile): Info? {
        val resolveConfiguration = OCInclusionContextUtil.getActiveConfiguration(file) ?: return null

        val swiftFiles = resolveConfiguration.sources.mapNotNull {
            if (FileUtil.namesEqual(it.extension, SwiftFileType.EXT)) VfsUtil.virtualToIoFile(it) else null
        }

        val includePaths = ArrayList<HeadersSearchPath>()
        SwiftSettings.getInstance().activeToolchainDir?.let {
            includePaths.add(HeadersSearchPath.includes(it.absoluteFile))
        }

        val roots = SwiftCustomIncludePathProvider.EP_NAME.extensionList
            .flatMap { it.getCustomLibrarySearchPaths(resolveConfiguration) }
            .mapNotNull { File(it) }
            .distinct()

        includePaths += roots.map { HeadersSearchPath.frameworks(it) }
        val distinctPaths = includePaths.distinct()

        var projectModel: AppleProjectModel? = null
        AppleProjectDataService.forEachProject(file.project) { appleProjectModel, _, _ ->
            projectModel = appleProjectModel
        }
        val targetModel = projectModel?.targets?.values?.firstOrNull() ?: return null
        val projectName = targetModel.name
        val bridgingHeaderPath = targetModel.bridgingHeader?.absolutePath
        val sdkInfo = MobileSwiftModuleManager.getAppleSdkInfo(resolveConfiguration) ?: return null
        val sdkVersion = AppleSdkManager.getInstance().findSdksForPlatform(ApplePlatform.Type.IOS_SIMULATOR).firstOrNull()?.versionString ?: return null

        // TODO: Provide "-simulator" suffix
        val targetTriple = ModuleInfo.getLLVMTargetTriple(sdkInfo, sdkVersion, null, null)

        return Info(
            moduleName = projectName,
            productName = projectName,
            compilerSwitches = CidrCompilerSwitches.EMPTY,
            file = file,
            sdkPath = sdkInfo.sdkPath ?: return null,
            target = targetTriple,
            document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return null,
            frameworkPaths = distinctPaths,
            includePaths = distinctPaths,
            swiftVersion = SwiftCompilerSettings.getCompilerVersion(),
            workingDirectory = targetModel.editableXcodeProjectDir.absolutePath,
            files = swiftFiles,
            bridgingHeaderPath = bridgingHeaderPath,
            swiftCompatibilityVersion = SwiftCompilerSettings.getSwiftCompatibilityVersion(resolveConfiguration)
        )
    }

    override fun prepareArguments(info: Info): List<String> {
        return collectCompileArguments(info)
    }

    override fun prepareArgumentsForCompletion(info: Info): List<String> {
        return collectCompileArguments(info)
    }

    private fun collectCompileArguments(info: Info): List<String> {
        val args = mutableListOf(
            "-module-name", info.moduleName,
            "-target", info.target
        )
        info.sdkPath?.let {
            args.add("-sdk")
            args.add(it)
        }

        if (info.files.size == 1 && !info.files.first().name.equals(SwiftNames.MAIN_FILE, ignoreCase = true)) {
            args.add("-parse-as-library")
        }

        info.swiftCompatibilityVersion?.let {
            args.add("-swift-version")
            args.add(it.toMinimalString())
        }

        for (frameworkPath in expandRecursive(info.frameworkPaths)) {
            args.add("-F")
            args.add(frameworkPath)
        }

        args.addAll(info.files.mapNotNull { file -> file.canonicalPath })

        for (includePath in expandRecursive(info.includePaths)) {
            args.addAll(listOf("-Xcc", "-I", "-Xcc", includePath, "-I", includePath))
        }

        args.addAll(info.compilerSwitches.getList(CidrCompilerSwitches.Format.RAW))

        if (info.bridgingHeaderPath != null) {
            args.add("-import-objc-header")
            args.add(info.bridgingHeaderPath)
        }

        args.add("-working-directory")
        args.add(info.workingDirectory)

        return args
    }

    private fun Version.toMinimalString(): String = when {
        bugfix > 0 -> "$major.$minor.$bugfix"
        minor > 0 -> "$major.$minor"
        else -> "$major"
    }

    class Info internal constructor(
        moduleName: String,
        productName: String,
        compilerSwitches: CidrCompilerSwitches,
        file: PsiFile,
        sdkPath: String,
        target: String,
        document: Document,
        frameworkPaths: Collection<HeadersSearchPath>,
        includePaths: Collection<HeadersSearchPath>,
        swiftVersion: Version,
        workingDirectory: String,
        override val files: Collection<File>,
        internal val bridgingHeaderPath: String?,
        val swiftCompatibilityVersion: Version?
    ) : SwiftSourceKitCompileArgumentsCollector.BaseInfo(
        moduleName, productName, compilerSwitches, file, sdkPath, target, document,
        frameworkPaths, includePaths, swiftVersion, workingDirectory
    )
}
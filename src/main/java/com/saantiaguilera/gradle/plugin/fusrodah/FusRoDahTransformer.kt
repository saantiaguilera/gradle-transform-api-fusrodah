package com.saantiaguilera.gradle.plugin.fusrodah

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import org.gradle.api.GradleException
import javassist.LoaderClassPath
import java.util.stream.Collectors
import java.io.IOException
import javassist.ClassPool
import javassist.NotFoundException
import java.io.File
import java.nio.file.Files
import java.util.HashSet
import java.util.jar.JarFile
import java.util.zip.ZipEntry


class FusRoDahTransformer(private val extension: FusRoDahExtension,
                          private val android: BaseExtension) : Transform() {

    override fun getName() = "FusRoDah"

    override fun getInputTypes() = setOf(QualifiedContent.DefaultContentType.CLASSES)

    override fun getScopes(): MutableSet<QualifiedContent.ScopeType> = TransformManager.SCOPE_FULL_PROJECT

    override fun isIncremental() = false

    override fun transform(transformInvocation: TransformInvocation) {
        val classNames = getClassNames(transformInvocation.inputs)

        val classPool = createClassPool(
            transformInvocation.inputs,
            transformInvocation.referencedInputs
        )

        val ctClasses = classNames.map { classPool.getCtClass(it) }.toSet()

        val processor = FusRoDahProcessor()
        ctClasses.forEach(processor::process)

        val outputPath = transformInvocation.outputProvider.getContentLocation(
            "classes",
            outputTypes,
            scopes,
            Format.DIRECTORY
        ).absolutePath

        ctClasses.forEach { it.writeFile(outputPath) }
    }

    private fun getClassNames(inputs: Collection<TransformInput>): Set<String> {
        val classNames = HashSet<String>()

        for (input in inputs) {
            classNames.addAll(getDirectoryInputs(input.directoryInputs))
            classNames.addAll(getJarInputs(input.jarInputs))
        }
        return classNames
    }

    private fun getDirectoryInputs(directoryInputs: Collection<DirectoryInput>): Set<String> {
        val classNames = HashSet<String>()
        for (input in directoryInputs) {
            try {
                classNames.addAll(processDirectoryInput(input))
            } catch (e: IOException) {
                throw GradleException(e.message ?: "Unknown error")
            }

        }
        return classNames
    }

    @Throws(IOException::class)
    fun processDirectoryInput(input: DirectoryInput): Set<String> {
        val dirPath = input.file.absolutePath

        return Files.walk(input.file.toPath())
            .map { file -> file.toAbsolutePath().toString() }
            .filter { path -> path.endsWith(SdkConstants.DOT_CLASS) }
            .map { path ->
                path.substring(
                    dirPath.length + 1,
                    path.length - SdkConstants.DOT_CLASS.length
                )
            }
            .map { path -> path.replace(File.separator, ".") }
            .collect(Collectors.toSet())
    }

    private fun getJarInputs(jarInputs: Collection<JarInput>): Set<String> {
        return jarInputs.stream()
            .map(QualifiedContent::getFile)
            .map(::JarFile)
            .map(JarFile::entries)
            .flatMap { it.toList().stream() }
            .filter { entry -> !entry.isDirectory && entry.name.endsWith(SdkConstants.DOT_CLASS) }
            .map(ZipEntry::getName)
            .map { name -> name.substring(0, name.length - SdkConstants.DOT_CLASS.length) }
            .map { name -> name.replace(File.separator, ".") }
            .collect(Collectors.toSet())
    }

    private fun createClassPool(
        inputs: Collection<TransformInput>,
        referencedInputs: Collection<TransformInput>
    ): ClassPool {
        val classPool = ClassPool()
        classPool.appendSystemPath()
        classPool.appendClassPath(LoaderClassPath(javaClass.classLoader))
        classPool.appendClassPath("${android.sdkDirectory.absolutePath}/platforms/${android.compileSdkVersion}/android.jar")

        (inputs + referencedInputs).stream()
            .flatMap { input -> (input.directoryInputs + input.jarInputs).stream() }
            .map { input -> input.file.absolutePath }
            .forEach { entry ->
                try {
                    classPool.appendClassPath(entry)
                } catch (e: NotFoundException) {
                    throw GradleException(e.message ?: "Unknown error")
                }
            }

        return classPool
    }

}
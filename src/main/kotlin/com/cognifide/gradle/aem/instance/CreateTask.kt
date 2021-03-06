package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.internal.file.resolver.FileResolver
import groovy.lang.Closure
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.util.ConfigureUtil
import java.io.File

open class CreateTask : AemDefaultTask() {

    companion object {
        const val NAME = "aemCreate"

        const val DOWNLOAD_DIR = "download"

        const val LICENSE_URL_PROP = "aem.instance.local.licenseUrl"

        const val JAR_URL_PROP = "aem.instance.local.jarUrl"
    }

    @Internal
    val instanceFileResolver = FileResolver(project, AemTask.temporaryDir(project, NAME, DOWNLOAD_DIR))

    @get:Internal
    val instanceFiles: List<File>
        get() = instanceFileResolver.allFiles()

    init {
        description = "Creates local AEM instance(s)."

        instanceFilesByProperties()
    }

    private fun instanceFilesByProperties() {
        val jarUrl = props.string(JAR_URL_PROP)
        if (!jarUrl.isNullOrBlank()) {
            instanceFileResolver.url(jarUrl!!)
        }

        val licenseUrl = props.string(LICENSE_URL_PROP)
        if (!licenseUrl.isNullOrBlank()) {
            instanceFileResolver.url(licenseUrl!!)
        }
    }

    fun instanceFiles(closure: Closure<*>) {
        ConfigureUtil.configure(closure, instanceFileResolver)
    }

    @TaskAction
    fun create() {
        val handles = Instance.handles(project).filter { !it.created }
        if (handles.isEmpty()) {
            logger.info("No instances to create")
            return
        }

        logger.info("Creating instances")
        handles.parallelStream().forEach { it.create(instanceFiles) }

        notifier.default("Instance(s) created", handles.names)
    }

}
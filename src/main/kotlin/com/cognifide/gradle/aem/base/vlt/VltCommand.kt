package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.FileOperations
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

// TODO https://github.com/Cognifide/gradle-aem-plugin/issues/135
class VltCommand(val project: Project) {

    val logger: Logger = project.logger

    val propertyParser = PropertyParser(project)

    val config = AemConfig.of(project)

    fun raw(command: String, props: Map<String, Any> = mapOf()) {
        val app = VltApp(project)
        val instance = determineInstance()
        val allProps = mapOf(
                "instance" to instance,
                "instances" to config.instances
        ) + props
        val fullCommand = propertyParser.expand("${config.vaultGlobalOptions} $command".trim(), allProps)

        app.execute(fullCommand)
    }

    fun checkout() {
        val contentDir = File(config.contentPath)
        if (!contentDir.exists()) {
            logger.info("JCR content directory to be checked out does not exist: ${contentDir.absolutePath}")
        }

        val filter = determineFilter()
        val props = mapOf("filter" to filter.file.absolutePath)

        filter.use {
            raw("checkout --force --filter {{filter}} {{instance.httpUrl}}/crx/server/crx.default", props)
        }
    }

    fun determineFilter(): VltFilter {
        val cmdFilterRoots = propertyParser.list("aem.checkout.filterRoots")

        return if (cmdFilterRoots.isNotEmpty()) {
            logger.info("Using Vault filter roots specified as command line property: $cmdFilterRoots")
            VltFilter.temporary(project, cmdFilterRoots)
        } else {
            val configFilter = FileOperations.find(project, config.vaultPath, config.checkoutFilterPath)
                    ?: throw VltException("Vault check out filter file does not exist at path: ${config.checkoutFilterPath} (or under directory: ${config.vaultPath}).")
            logger.info("Using Vault filter specified in configuration: ${config.checkoutFilterPath}")
            VltFilter(configFilter)
        }
    }

    fun determineInstance(): Instance {
        val cmdInstanceArg = propertyParser.string("aem.vlt.instance")
        if (!cmdInstanceArg.isNullOrBlank()) {
            val cmdInstance = Instance.parse(cmdInstanceArg!!).first()
            cmdInstance.validate()

            logger.info("Using instance specified by command line parameter: $cmdInstance")
            return cmdInstance
        }

        val namedInstance = Instance.filter(project, config.deployInstanceName).firstOrNull()
        if (namedInstance != null) {
            logger.info("Using first instance matching filter '${config.deployInstanceName}': $namedInstance")
            return namedInstance
        }

        val anyInstance = Instance.filter(project, Instance.FILTER_ANY).firstOrNull()
        if (anyInstance != null) {
            logger.info("Using first instance matching filter '${Instance.FILTER_ANY}': $anyInstance")
            return anyInstance
        }

        throw VltException("Vault instance cannot be determined neither by command line parameter nor AEM config.")
    }

    fun clean() {
        val contentDir = File(config.contentPath)
        if (!contentDir.exists()) {
            logger.warn("JCR content directory to be cleaned does not exist: ${contentDir.absolutePath}")
            return
        }

        val cleaner = VltCleaner(contentDir, logger)
        cleaner.removeFiles(config.cleanFilesDeleted)
        cleaner.cleanupDotContent(config.vaultSkipProperties, config.vaultLineSeparatorString)
    }

}
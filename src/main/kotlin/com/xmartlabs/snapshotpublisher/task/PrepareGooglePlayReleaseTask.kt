package com.xmartlabs.snapshotpublisher.task

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.tasks.internal.PlayPublishPackageBase
import com.xmartlabs.snapshotpublisher.Constants
import com.xmartlabs.snapshotpublisher.plugin.PlayPublisherPluginHelper
import com.xmartlabs.snapshotpublisher.utils.AndroidPublisherHelper
import com.xmartlabs.snapshotpublisher.utils.snapshotReleaseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

open class PrepareGooglePlayReleaseTask : DefaultTask() {
  companion object {
    const val MAX_RELEASE_NOTES_LENGTH = 500
  }

  @get:Internal
  internal lateinit var publishGooglePlayTask: PlayPublishPackageBase
  @get:Internal
  internal lateinit var variant: ApplicationVariant

  private val googlePlayConfig by lazy { project.snapshotReleaseExtension.googlePlay }
  private val publisher by lazy { AndroidPublisherHelper.buildPublisher(googlePlayConfig) }
  private val generatedReleaseNotesFile by lazy { File(project.buildDir, Constants.OUTPUT_RELEASE_NOTES_FILE_PATH) }

  @Suppress("unused")
  @TaskAction
  fun action() {
    createReleaseNotesFile()

    with(publishGooglePlayTask.extension) {
      defaultToAppBundles = googlePlayConfig.defaultToAppBundles
      releaseStatus = googlePlayConfig.releaseStatus
      resolutionStrategy = googlePlayConfig.resolutionStrategy
      track = googlePlayConfig.track
      serviceAccountCredentials = googlePlayConfig.serviceAccountCredentials
    }
  }

  private fun createReleaseNotesFile() {
    AndroidPublisherHelper.read(
        skipIfNotFound = false,
        publisher = publisher,
        variant = variant,
        project = project
    ) { editId ->
      val details = details().get(variant.applicationId, editId).execute()
      PlayPublisherPluginHelper.releaseNotesFile(project, variant, details.defaultLanguage, googlePlayConfig.track)
          .apply {
            parentFile.mkdirs()
            writeText(processGeneratedReleaseNotes())
          }
    }
  }

  private fun processGeneratedReleaseNotes(): String {
    val releaseNotes: String = generatedReleaseNotesFile.readText()
    if (releaseNotes.length <= MAX_RELEASE_NOTES_LENGTH) {
      return releaseNotes
    }

    var notes = ""
    releaseNotes.lines().forEach { line ->
      if (notes.length + line.length < MAX_RELEASE_NOTES_LENGTH) {
        notes += (if (notes.isEmpty()) "" else "\n") + line
      } else {
        return@forEach
      }
    }
    if (notes.isEmpty()) {
      notes = releaseNotes.substring(0, Math.min(releaseNotes.length, MAX_RELEASE_NOTES_LENGTH))
    }

    return notes
  }
}
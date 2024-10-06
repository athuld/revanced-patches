package src.main.kotlin.app.revanced.patches.googlephotos.misc.branding

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.util.ResourceGroup
import app.revanced.util.Utils.trimIndentMultiline
import app.revanced.util.copyResources
import java.io.File
import java.nio.file.Files

@Patch(
    name = "Custom G-Photos branding",
    description = "Applies a custom app name and icon. Defaults to \"Photos ReVanced\" and the ReVanced G-Photos logo.",
    compatiblePackages = [CompatiblePackage("com.google.android.apps.photos"),]
)
@Suppress("unused")
object CustomBrandingPatch : ResourcePatch() {
    private const val REVANCED_ICON = "ReVanced*Logo" // Can never be a valid path.
    private const val APP_NAME = "Photos ReVanced"

    private val iconResourceFileNames = arrayOf(
        "adaptiveproduct_photos_foreground_color_108.png",
        "photos_launchericon_product_logo_photos_launcher_color_48.webp",
        "product_logo_photos_round_launcher_color_48.png",
    ).map { it }.toTypedArray()

    private val mipmapDirectories = arrayOf(
        "xxxhdpi",
        "xxhdpi",
        "xhdpi",
        "hdpi",
        "mdpi",
    ).map { "mipmap-$it" }

    private var appName by stringPatchOption(
        key = "appName",
        default = APP_NAME,
        values = mapOf(
            "Photos ReVanced" to APP_NAME,
            "Photos" to "Photos",
        ),
        title = "App name",
        description = "The name of the app.",
    )

    private var icon by stringPatchOption(
        key = "iconPath",
        default = REVANCED_ICON,
        values = mapOf("ReVanced Logo" to REVANCED_ICON),
        title = "App icon",
        description = """
            The icon to apply to the app.
            
            If a path to a folder is provided, the folder must contain the following folders:

            ${mipmapDirectories.joinToString("\n") { "- $it" }}

            Each of these folders must contain the following files:

            ${iconResourceFileNames.joinToString("\n") { "- $it" }}
        """.trimIndentMultiline(),
    )

    private var isLauncherEnabled by stringPatchOption(
        key = "isLauncherEnabled",
        default = "true",
        title = "Enable app icon in launcher",
        description = "Should the app be available in launcher.",
    )

    override fun execute(context: ResourceContext) {
        icon?.let { icon ->
            // Change the app icon.
            mipmapDirectories.map { directory ->
                ResourceGroup(
                    directory,
                    *iconResourceFileNames,
                )
            }.let { resourceGroups ->
                if (icon != REVANCED_ICON) {
                    val path = File(icon)
                    val resourceDirectory = context.get("res")

                    resourceGroups.forEach { group ->
                        val fromDirectory = path.resolve(group.resourceDirectoryName)
                        val toDirectory = resourceDirectory.resolve(group.resourceDirectoryName)

                        group.resources.forEach { iconFileName ->
                            Files.write(
                                toDirectory.resolve(iconFileName).toPath(),
                                fromDirectory.resolve(iconFileName).readBytes(),
                            )
                        }
                    }
                } else {
                    resourceGroups.forEach { context.copyResources("custom-gphotos-branding", it) }
                }
            }
        }

        appName?.let { name ->
            // Change the app name.
            val manifest = context.get("AndroidManifest.xml")
            manifest.writeText(
                manifest.readText()
                    .replace(
                        "android:label=\"@string/photos_theme_google_photos",
                        "android:label=\"$name",
                    ),
            )
        }

        isLauncherEnabled?.let { isEnabled ->
            // Whether to enable icon in launcher
            if(isEnabled == "false"){
                val manifest = context.get("AndroidManifest.xml")
                manifest.writeText(
                    manifest.readText()
                        .replace(
                            "<category android:name=\"android.intent.category.LAUNCHER\"/>",
                            "",
                        ),
                )
            }
        }
    }
}

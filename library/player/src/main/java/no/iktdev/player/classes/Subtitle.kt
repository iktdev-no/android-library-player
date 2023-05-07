package no.iktdev.player.classes

import java.io.Serializable


data class Subtitle(
    val title: String,
    var language: String,
    val collection: String?,
    val subtitle: String,
    var subtitlePath: String? = null,
    val format: String

): Serializable {
    fun copy(): Subtitle {
        return Subtitle(
            title = title,
            language = language,
            collection = collection,
            subtitle = subtitle,
            subtitlePath = subtitlePath,
            format = format
        )
    }
}

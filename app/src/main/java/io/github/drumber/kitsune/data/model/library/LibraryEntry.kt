package io.github.drumber.kitsune.data.model.library

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type
import io.github.drumber.kitsune.R
import io.github.drumber.kitsune.data.model.auth.User
import io.github.drumber.kitsune.data.model.resource.Anime
import io.github.drumber.kitsune.data.model.resource.Manga
import io.github.drumber.kitsune.util.EmptyStringIdHandler
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "library_table")
@Type("libraryEntries")
data class LibraryEntry(
    @PrimaryKey @Id(EmptyStringIdHandler::class)
    var id: String = "",
    var createdAt: String? = null,
    var updatedAt: String? = null,
    var status: Status? = null,
    var progress: Int? = null,
    var volumesOwned: Int? = null,
    var reconsuming: Boolean? = null,
    var reconsumeCount: Int? = null,
    var notes: String? = null,
    @JsonProperty("private")
    var isPrivate: Boolean? = null,
    var reactionSkipped: ReactionSkip? = null,
    var progressedAt: String? = null,
    var startedAt: String? = null,
    var finishedAt: String? = null,
    @JsonInclude(JsonInclude.Include.ALWAYS)
    var ratingTwenty: Int? = null,
    @Embedded(prefix = "anime_")
    @Relationship("anime")
    var anime: Anime? = null,
    @Embedded(prefix = "manga_")
    @Relationship("manga")
    var manga: Manga? = null,
    @Ignore
    @Relationship("user")
    var user: User? = null
): Parcelable

enum class Status {
    @JsonProperty("completed") Completed,
    @JsonProperty("current") Current,
    @JsonProperty("dropped") Dropped,
    @JsonProperty("on_hold") OnHold,
    @JsonProperty("planned") Planned
}

enum class ReactionSkip {
    Unskipped,
    Skipped,
    Ignored
}

fun Status.getStringResId() = when (this) {
    Status.Completed -> R.string.library_status_completed
    Status.Current -> R.string.library_status_watching
    Status.Dropped -> R.string.library_status_dropped
    Status.OnHold -> R.string.library_status_on_hold
    Status.Planned -> R.string.library_status_planned
}

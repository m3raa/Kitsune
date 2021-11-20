package io.github.drumber.kitsune.data.model.resource

import android.content.Context
import android.os.Parcelable
import io.github.drumber.kitsune.R
import io.github.drumber.kitsune.data.model.TitlesPref
import io.github.drumber.kitsune.data.model.category.Category
import io.github.drumber.kitsune.data.model.production.AnimeProductionRole
import io.github.drumber.kitsune.preference.KitsunePref
import io.github.drumber.kitsune.util.TimeUtil
import io.github.drumber.kitsune.util.extensions.formatDate
import io.github.drumber.kitsune.util.extensions.toDate
import io.github.drumber.kitsune.util.originalOrDown
import io.github.drumber.kitsune.util.smallOrHigher
import kotlinx.parcelize.Parcelize
import java.util.*

sealed class ResourceAdapter(
    val id: String,
    val title: String,
    val titles: Titles,
    val description: String,
    val startDate: String?,
    val endDate: String?,
    val avgRating: String?,
    val userCount: Int,
    val favoriteCount: Int,
    val popularityRank: Int?,
    val ratingRank: Int?,
    val ageRating: AgeRating?,
    val ageRatingGuide: String?,
    val subtype: String,
    val status: Status?,
    val tba: String?,
    val posterImage: String?,
    val coverImage: String?,
    val categories: List<Category>?
) : Parcelable {

    val publishingYear: String
        get() = if (!startDate.isNullOrBlank()) {
            startDate.toDate().get(Calendar.YEAR).toString()
        } else "?"

    fun season(context: Context): String {
        val date = startDate?.toDate()
        val stringRes = when (date?.get(Calendar.MONTH)?.plus(1)) {
            in arrayOf(12, 1, 2) -> R.string.season_winter
            in 3..5 -> R.string.season_spring
            in 6..8 -> R.string.season_summer
            in 9..11 -> R.string.season_fall
            else -> R.string.no_information
        }
        return context.getString(stringRes)
    }

    val seasonYear: String get() {
        val date = startDate?.toDate()
        return date?.let {
            val year = date.get(Calendar.YEAR)
            val month = date.get(Calendar.MONTH) + 1
            if (month == 12) {
                year + 1
            } else {
                year
            }
        }?.toString() ?: "?"
    }

    val airedText: String get() {
        var airedText = formatDate(startDate)
        if(!endDate.isNullOrBlank() && startDate != endDate) {
            airedText += " - ${formatDate(endDate)}"
        }
        return airedText
    }

    private fun formatDate(dateString: String?): String {
        return if (!dateString.isNullOrBlank()) {
            dateString.toDate().formatDate()
        } else {
            "?"
        }
    }

    fun statusText(context: Context): String {
        val stringRes = when (status) {
            Status.Current -> if(isAnime()) R.string.status_current else R.string.status_current_manga
            Status.Finished -> R.string.status_finished
            Status.TBA -> R.string.status_tba
            Status.Unreleased -> R.string.status_unreleased
            Status.Upcoming -> R.string.status_upcoming
            null -> R.string.no_information
        }
        return context.getString(stringRes)
    }

    val ageRatingText: String? get() {
        if(ageRating == null) return null
        var ageRatingText = ageRating.name
        if(ageRatingGuide != null) {
            ageRatingText += " - $ageRatingGuide"
        }
        return ageRatingText
    }

    val serialization: String? get() = if(this is MangaResource) manga.serialization else null

    val chapters: String? get() = if(this is MangaResource) manga.chapterCount?.toString() else null

    val volumes: String? get() = if(this is MangaResource && manga.volumeCount?.equals(0) == false) {
            manga.volumeCount?.toString()
    } else { null }

    val episodes: String? get() = if(this is AnimeResource) anime.episodeCount?.toString() else null

    fun lengthText(context: Context): String? {
        if (this is AnimeResource) {
            val count = anime.episodeCount
            val length = anime.episodeLength ?: return null
            val lengthEachText = context.getString(R.string.data_length_each, length)
            return if (count == null) {
                lengthEachText
            } else {
                val minutes = count * length.toLong()
                val durationText = TimeUtil.timeToHumanReadableFormat(minutes * 60, context)
                if (count > 1) {
                    context.getString(R.string.data_length_total, durationText) + " ($lengthEachText)"
                } else {
                    durationText
                }
            }
        }
        return null
    }

    val trailerUrl: String?
        get() = if (this is AnimeResource && !anime.youtubeVideoId.isNullOrBlank()) {
            "https://www.youtube.com/watch?v=${anime.youtubeVideoId}"
        } else null

    val trailerCoverUrl: String?
        get() = if (this is AnimeResource && !anime.youtubeVideoId.isNullOrBlank()) {
            "https://img.youtube.com/vi/${anime.youtubeVideoId}/mqdefault.jpg"
        } else null

    fun getProducer(role: AnimeProductionRole): String? {
        return if (this is AnimeResource) {
            anime.animeProduction?.filter { it.role == role }
                ?.mapNotNull { it.producer?.name }
                ?.distinct()
                ?.joinToString(", ")
        } else {
            null
        }
    }

    fun hasStreamingLinks() = this is AnimeResource && !anime.streamingLinks.isNullOrEmpty()

    fun isAnime() = this is AnimeResource

    @Parcelize
    class AnimeResource(val anime: Anime) : ResourceAdapter(
        id = anime.id,
        title = getTitle(anime.titles, anime.canonicalTitle),
        titles = anime.titles.require(),
        description = anime.description.orEmpty(),
        startDate = anime.startDate,
        endDate = anime.endDate,
        avgRating = anime.averageRating,
        userCount = anime.userCount.orNull(),
        favoriteCount = anime.favoritesCount.orNull(),
        popularityRank = anime.popularityRank,
        ratingRank = anime.ratingRank,
        ageRating = anime.ageRating,
        ageRatingGuide = anime.ageRatingGuide,
        subtype = anime.subtype?.name.orEmpty().replaceFirstChar(Char::titlecase),
        status = anime.status,
        tba = anime.tba,
        posterImage = anime.posterImage?.smallOrHigher(),
        coverImage = anime.coverImage?.originalOrDown(),
        categories = anime.categories
    ), Parcelable

    @Parcelize
    class MangaResource(val manga: Manga) : ResourceAdapter(
        id = manga.id,
        title = getTitle(manga.titles, manga.canonicalTitle),
        titles = manga.titles.require(),
        description = manga.description.orEmpty(),
        startDate = manga.startDate,
        endDate = manga.endDate,
        avgRating = manga.averageRating,
        userCount = manga.userCount.orNull(),
        favoriteCount = manga.favoritesCount.orNull(),
        popularityRank = manga.popularityRank,
        ratingRank = manga.ratingRank,
        ageRating = manga.ageRating,
        ageRatingGuide = manga.ageRatingGuide,
        subtype = manga.subtype?.name.orEmpty().replaceFirstChar(Char::titlecase),
        status = manga.status,
        tba = manga.tba,
        posterImage = manga.posterImage?.smallOrHigher(),
        coverImage = manga.coverImage?.originalOrDown(),
        categories = manga.categories
    ), Parcelable

    companion object {
        fun fromResource(resource: Resource) = when (resource) {
            is Anime -> AnimeResource(resource)
            is Manga -> MangaResource(resource)
            else -> throw IllegalStateException("Unknown resource subclass: ${resource::class.java}")
        }
    }

}

private fun getTitle(title: Titles?, canonical: String?): String {
    val nf = "<No title found>"
    return when (KitsunePref.titles) {
        TitlesPref.Canonical -> canonical ?: nf
        TitlesPref.Romanized -> title?.enJp ?: canonical ?: nf
        TitlesPref.English -> title?.en ?: canonical ?: nf
    }
}

private fun Titles?.require(): Titles {
    return this ?: Titles(null, null, null)
}

private fun Int?.orNull() = this ?: 0

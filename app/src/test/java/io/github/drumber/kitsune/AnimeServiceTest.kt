package io.github.drumber.kitsune

import io.github.drumber.kitsune.data.service.Filter
import io.github.drumber.kitsune.data.service.anime.AnimeService
import io.github.drumber.kitsune.di.serviceModule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AnimeServiceTest : BaseTest() {

    override val koinModules = listOf(serviceModule)

    @Test
    fun fetchAllAnime() = runBlocking {
        val animeService = getKoin().get<AnimeService>()
        val response = animeService.allAnime()
        val animeList = response.get()
        assertNotNull(animeList)
        println("Received ${animeList?.size} anime")
        println("First: ${animeList?.first()}")
    }

    @Test
    fun fetchSingleAnime() = runBlocking {
        val animeService = getKoin().get<AnimeService>()
        val response = animeService.getAnime("1")
        val anime = response.get()
        assertNotNull(anime)
        println("Received anime: $anime")
    }

    @Test
    fun fetchTrending() = runBlocking {
        val animeService = getKoin().get<AnimeService>()
        val response = animeService.trending()
        val animeList = response.get()
        assertNotNull(animeList)
        println("Received ${animeList?.size} anime")
        println("First: ${animeList?.first()}")
    }

    @Test
    fun filterTest() = runBlocking {
        val animeService = getKoin().get<AnimeService>()
        val responseAll = animeService.allAnime(
            Filter()
                .pageOffset(5)
                .pageLimit(5)
                .options
        )
        assertEquals(5, responseAll.get()?.size)

        val responseSingle = animeService.allAnime(
            Filter()
                .filter("slug", "cowboy-bebop")
                .fields("anime", "titles")
                .include("categories")
                .options
        )
        val singleAnime = responseSingle.get()?.first()
        assertNull(singleAnime?.createdAt)
        assertNotNull(singleAnime?.titles)
        assertEquals("Cowboy Bebop", singleAnime?.titles?.en)
    }

    @Test
    fun filterIncludeTest() = runBlocking {
        val animeService = getKoin().get<AnimeService>()

        val response = animeService.allAnime(
            Filter()
                .filter("slug", "one-piece")
                .include(
                    "categories",
                    "animeProductions.producer",
                    "streamingLinks",
                    "mediaRelationships",
                    "mediaRelationships.destination"
                )
                .options
        )
        val anime = response.get()?.first()
        assertNotNull(anime)
        println("Anime with included relationships: $anime")
        println("\nGot ${anime?.mediaRelationships?.size} related media objects.")

        assertFalse(anime?.categories.isNullOrEmpty())
        assertFalse(anime?.animeProduction.isNullOrEmpty())
        assertFalse(anime?.streamingLinks.isNullOrEmpty())
        assertFalse(anime?.mediaRelationships.isNullOrEmpty())
        assertNotNull(anime?.mediaRelationships?.firstOrNull())
    }

}
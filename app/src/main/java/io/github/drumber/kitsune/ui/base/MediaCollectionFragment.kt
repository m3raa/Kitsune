package io.github.drumber.kitsune.ui.base

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import com.bumptech.glide.Glide
import io.github.drumber.kitsune.data.model.MediaType
import io.github.drumber.kitsune.data.model.media.BaseMedia
import io.github.drumber.kitsune.data.model.media.MediaAdapter
import io.github.drumber.kitsune.ui.adapter.OnItemClickListener
import io.github.drumber.kitsune.ui.adapter.paging.AnimeAdapter
import io.github.drumber.kitsune.ui.adapter.paging.MangaAdapter
import io.github.drumber.kitsune.ui.adapter.paging.MediaPagingAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest

abstract class MediaCollectionFragment(
    @LayoutRes contentLayoutId: Int
): BaseCollectionFragment(contentLayoutId), OnItemClickListener<BaseMedia> {

    private var dataFlowScope: Job? = null

    abstract val collectionViewModel: MediaCollectionViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val glide = Glide.with(this)
        collectionViewModel.mediaSelector.observe(viewLifecycleOwner) { selector ->
            val adapter = when(selector.mediaType) {
                MediaType.Anime -> AnimeAdapter(glide, this::onItemClick).setupAdapter()
                MediaType.Manga -> MangaAdapter(glide, this::onItemClick).setupAdapter()
            }
            setRecyclerViewAdapter(adapter)
        }
    }

    private inline fun <reified T : BaseMedia> MediaPagingAdapter<T>.setupAdapter(): MediaPagingAdapter<T> {
        dataFlowScope?.cancel()
        dataFlowScope = viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            collectionViewModel.dataSource.collectLatest { data ->
                (data as? PagingData<T>)?.let { this@setupAdapter.submitData(it) }
            }
        }
        return this
    }

    override fun onItemClick(view: View, item: BaseMedia) {
        val model = MediaAdapter.fromMedia(item)
        onMediaClicked(view, model)
    }

    open fun onMediaClicked(view: View, model: MediaAdapter) {}

    override fun onDestroyView() {
        super.onDestroyView()
        dataFlowScope = null
    }

}
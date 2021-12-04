package io.github.drumber.kitsune.ui.details

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.chip.Chip
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.snackbar.Snackbar
import io.github.drumber.kitsune.GlideApp
import io.github.drumber.kitsune.R
import io.github.drumber.kitsune.constants.Kitsu
import io.github.drumber.kitsune.constants.SortFilter
import io.github.drumber.kitsune.data.model.MediaSelector
import io.github.drumber.kitsune.data.model.MediaType
import io.github.drumber.kitsune.data.model.category.Category
import io.github.drumber.kitsune.data.model.library.Status
import io.github.drumber.kitsune.data.model.library.getStringResId
import io.github.drumber.kitsune.data.model.media.Anime
import io.github.drumber.kitsune.data.model.media.MediaAdapter
import io.github.drumber.kitsune.data.service.Filter
import io.github.drumber.kitsune.databinding.FragmentDetailsBinding
import io.github.drumber.kitsune.ui.adapter.MediaRecyclerViewAdapter
import io.github.drumber.kitsune.ui.adapter.StreamingLinkAdapter
import io.github.drumber.kitsune.ui.authentication.AuthenticationActivity
import io.github.drumber.kitsune.ui.base.BaseFragment
import io.github.drumber.kitsune.ui.widget.FadingToolbarOffsetListener
import io.github.drumber.kitsune.util.extensions.*
import io.github.drumber.kitsune.util.initMarginWindowInsetsListener
import io.github.drumber.kitsune.util.initPaddingWindowInsetsListener
import io.github.drumber.kitsune.util.initWindowInsetsListener
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.CopyOnWriteArrayList

class DetailsFragment : BaseFragment(R.layout.fragment_details, true),
    NavigationBarView.OnItemReselectedListener {

    private val args: DetailsFragmentArgs by navArgs()

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailsViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (context?.isNightMode() == false) {
            activity?.clearLightStatusBar()
        }

        initAppBar()

        viewModel.initMediaAdapter(args.model)

        viewModel.mediaAdapter.observe(viewLifecycleOwner) { model ->
            binding.data = model
            showCategoryChips(model)
            showFranchise(model)
            showStreamingLinks(model)

            val glide = GlideApp.with(this)

            glide.load(model.coverImage)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.cover_placeholder)
                .into(binding.ivCover)

            glide.load(model.posterImage)
                .transform(CenterCrop(), RoundedCorners(8))
                .placeholder(R.drawable.ic_insert_photo_48)
                .into(binding.ivThumbnail)
        }

        viewModel.libraryEntry.observe(viewLifecycleOwner) {
            it?.status?.let { status ->
                binding.btnManageLibrary.setText(status.getStringResId())
            } ?: binding.btnManageLibrary.setText(R.string.library_action_add)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.isVisible = isLoading
        }

        binding.apply {
            content.initPaddingWindowInsetsListener(left = true, right = true)
            btnManageLibrary.setOnClickListener { showManageLibraryBottomSheet() }
            btnMediaUnits.setOnClickListener {
                val media = args.model.media
                val libraryEntry = viewModel.libraryEntry.value
                val action = DetailsFragmentDirections.actionDetailsFragmentToEpisodesFragment(media, libraryEntry?.id)
                findNavController().navigate(action)
            }
            btnCharacters.setOnClickListener {
                val media = args.model.media
                val action = DetailsFragmentDirections.actionDetailsFragmentToCharactersFragment(media.id, media is Anime)
                findNavController().navigate(action)
            }
        }

        setFragmentResultListener(ManageLibraryBottomSheet.STATUS_REQUEST_KEY) { _, bundle ->
            val libraryEntryStatus = bundle.get(ManageLibraryBottomSheet.BUNDLE_STATUS) as? Status
            libraryEntryStatus?.let { viewModel.updateLibraryEntryStatus(it) }
        }

        setFragmentResultListener(ManageLibraryBottomSheet.REMOVE_REQUEST_KEY) { _, bundle ->
            val shouldRemove = !bundle.getBoolean(ManageLibraryBottomSheet.BUNDLE_EXISTS_IN_LIBRARY)
            if (shouldRemove) {
                viewModel.removeLibraryEntry()
            }
        }
    }

    private fun initAppBar() {
        binding.apply {
            appBarLayout.addOnOffsetChangedListener(
                FadingToolbarOffsetListener(
                    requireActivity(),
                    toolbar
                )
            )

            toolbar.setNavigationOnClickListener { goBack() }
            toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_share_media -> {
                        val url = viewModel.mediaAdapter.value?.let {
                            val prefix = if (it.isAnime()) Kitsu.ANIME_URL_PREFIX else Kitsu.MANGA_URL_PREFIX
                            prefix + it.media.slug
                        }
                        if (url != null) {
                            startUrlShareIntent(url)
                        } else {
                            showSomethingWrongToast()
                        }
                    }
                }
                true
            }

            val defaultTitleMarginStart = collapsingToolbar.expandedTitleMarginStart
            val defaultTitleMarginEnd = collapsingToolbar.expandedTitleMarginStart
            ViewCompat.setOnApplyWindowInsetsListener(collapsingToolbar) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                val isRtl = ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL
                collapsingToolbar.expandedTitleMarginStart =
                    defaultTitleMarginStart + if (isRtl) insets.right else insets.left
                collapsingToolbar.expandedTitleMarginEnd =
                    defaultTitleMarginEnd + if (isRtl) insets.left else insets.right
                windowInsets
            }
            toolbar.initWindowInsetsListener(consume = false)
        }
    }

    private fun showCategoryChips(mediaAdapter: MediaAdapter) {
        if (!mediaAdapter.categories.isNullOrEmpty()) {
            binding.chipGroupCategories.removeAllViews()

            mediaAdapter.categories.orEmpty()
                .sortedBy { it.title }
                .forEach { category ->
                    val chip = Chip(requireContext())
                    chip.text = category.title
                    chip.setOnClickListener {
                        onCategoryChipClicked(category, mediaAdapter)
                    }
                    binding.chipGroupCategories.addView(chip)
            }
        }
    }

    private fun onCategoryChipClicked(category: Category, mediaAdapter: MediaAdapter) {
        val categorySlug = category.slug ?: return
        val title = category.title ?: getString(R.string.no_information)

        val mediaSelector = MediaSelector(
            if (mediaAdapter.isAnime()) MediaType.Anime else MediaType.Manga,
            Filter()
                .filter("categories", categorySlug)
                .sort(SortFilter.POPULARITY_DESC.queryParam)
        )

        val action = DetailsFragmentDirections.actionDetailsFragmentToMediaListFragment(mediaSelector, title)
        findNavController().navigate(action)
    }

    private fun showFranchise(mediaAdapter: MediaAdapter) {
        val data = mediaAdapter.media.mediaRelationships?.sortedBy {
            it.role?.ordinal
        }?.mapNotNull {
            it.media?.let { media -> MediaAdapter.fromMedia(media) }
        } ?: emptyList()

        if (binding.rvFranchise.adapter !is MediaRecyclerViewAdapter) {
            val glide = GlideApp.with(this)
            val adapter = MediaRecyclerViewAdapter(CopyOnWriteArrayList(data), glide) { media ->
                onFranchiseItemClicked(media)
            }
            adapter.overrideWidth = resources.getDimensionPixelSize(R.dimen.media_item_width_small)
            adapter.overrideHeight = resources.getDimensionPixelSize(R.dimen.media_item_height_small)
            binding.rvFranchise.adapter = adapter
        } else {
            val adapter = binding.rvFranchise.adapter as MediaRecyclerViewAdapter
            adapter.dataSet.addAll(0, data)
            adapter.notifyDataSetChanged()
        }
    }

    private fun onFranchiseItemClicked(mediaAdapter: MediaAdapter) {
        val action = DetailsFragmentDirections.actionDetailsFragmentSelf(mediaAdapter)
        findNavController().navigateSafe(R.id.details_fragment, action)
    }

    private fun showStreamingLinks(mediaAdapter: MediaAdapter) {
        val data = (mediaAdapter.media as? Anime)?.streamingLinks ?: emptyList()

        if (binding.rvStreamer.adapter !is StreamingLinkAdapter) {
            val glide = GlideApp.with(this)
            val adapter = StreamingLinkAdapter(CopyOnWriteArrayList(data), glide) { streamingLink ->
                streamingLink.url?.let { url ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                }
            }
            binding.rvStreamer.adapter = adapter
        } else {
            val adapter = binding.rvStreamer.adapter as StreamingLinkAdapter
            adapter.dataSet.addAll(0, data)
            adapter.notifyDataSetChanged()
        }
    }

    private fun showManageLibraryBottomSheet() {
        if (viewModel.isLoggedIn()) {
            viewModel.mediaAdapter.value?.let { mediaAdapter ->
                val sheetManageLibrary = ManageLibraryBottomSheet()
                val bundle = bundleOf(
                    ManageLibraryBottomSheet.BUNDLE_TITLE to mediaAdapter.title,
                    ManageLibraryBottomSheet.BUNDLE_IS_ANIME to mediaAdapter.isAnime(),
                    ManageLibraryBottomSheet.BUNDLE_EXISTS_IN_LIBRARY to (viewModel.libraryEntry.value != null)
                )
                sheetManageLibrary.arguments = bundle
                sheetManageLibrary.show(parentFragmentManager, ManageLibraryBottomSheet.TAG)
            }
        } else {
            Snackbar.make(
                binding.btnManageLibrary,
                R.string.info_log_in_required,
                Snackbar.LENGTH_LONG
            ).apply {
                view.initMarginWindowInsetsListener(left = true, right = true)
                setAction(R.string.action_log_in) {
                    val intent = Intent(requireActivity(), AuthenticationActivity::class.java)
                    startActivity(intent)
                }
            }.show()
        }
    }

    private fun goBack() {
        findNavController().navigateUp()
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        goBack()
    }

    override fun onPause() {
        super.onPause()
        if (activity?.isLightStatusBar() == false && context?.isNightMode() == false) {
            activity?.setLightStatusBar()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}
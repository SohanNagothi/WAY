package com.example.way.ui.selfdefense

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.way.data.model.SelfDefenseContent
import com.example.way.data.model.SelfDefenseItem
import com.example.way.databinding.FragmentSelfDefenseBinding
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

/**
 * Self Defense screen — safety text guides and YouTube links.
 */
@AndroidEntryPoint
class SelfDefenseFragment : Fragment() {

    private var _binding: FragmentSelfDefenseBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SelfDefenseAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelfDefenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SelfDefenseAdapter(onVideoOpenClick = { video ->
            openVideoInYouTube(video)
        })
        binding.rvTips.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTips.adapter = adapter

        // Show all items by default
        adapter.submitList(SelfDefenseContent.getAllItems())

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val items = when (tab?.position) {
                    0 -> SelfDefenseContent.getAllItems()
                    1 -> SelfDefenseContent.getTips()
                    2 -> SelfDefenseContent.getVideos()
                    else -> SelfDefenseContent.getAllItems()
                }
                adapter.submitList(items)
                binding.rvTips.scrollToPosition(0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun openVideoInYouTube(video: SelfDefenseItem.VideoTip) {
        val id = video.youtubeId
        val watchUrl = if (id.isNotBlank()) {
            "https://www.youtube.com/watch?v=$id"
        } else {
            video.youtubeUrl
        }

        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(watchUrl)).apply {
            `package` = "com.google.android.youtube"
        }

        try {
            startActivity(appIntent)
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(watchUrl)))
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Unable to open YouTube link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

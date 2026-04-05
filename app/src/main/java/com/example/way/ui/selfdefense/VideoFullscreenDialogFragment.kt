package com.example.way.ui.selfdefense

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.example.way.R

class VideoFullscreenDialogFragment : DialogFragment() {

    private var webView: WebView? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_video_fullscreen, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val youtubeId = requireArguments().getString(ARG_YOUTUBE_ID).orEmpty()
        val closeBtn = view.findViewById<Button>(R.id.btnClose)
        val player = view.findViewById<WebView>(R.id.wvFullscreen)
        webView = player

        closeBtn.setOnClickListener { dismissAllowingStateLoss() }

        player.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        player.webChromeClient = WebChromeClient()
        player.webViewClient = WebViewClient()

        val html = """
            <html>
            <body style="margin:0;padding:0;background:#000;">
              <iframe
                width="100%"
                height="100%"
                src="https://www.youtube.com/embed/$youtubeId?autoplay=1&playsinline=1"
                frameborder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                allowfullscreen>
              </iframe>
            </body>
            </html>
        """.trimIndent()

        player.loadDataWithBaseURL(
            "https://www.youtube.com",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    override fun onDestroyView() {
        webView?.loadUrl("about:blank")
        webView = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_YOUTUBE_ID = "youtube_id"

        fun newInstance(youtubeId: String): VideoFullscreenDialogFragment {
            return VideoFullscreenDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_YOUTUBE_ID, youtubeId)
                }
            }
        }
    }
}

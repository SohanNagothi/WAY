package com.example.way.data.model

sealed class SelfDefenseItem {
    data class TextTip(
        val title: String,
        val body: String,
        val category: String = "tip"
    ) : SelfDefenseItem()

    data class VideoTip(
        val title: String,
        val youtubeUrl: String,
        val category: String = "video"
    ) : SelfDefenseItem() {

        val youtubeId: String
            get() = extractYouTubeId(youtubeUrl)

        companion object {
            private fun extractYouTubeId(url: String): String {
                val value = url.trim()

                // https://youtu.be/<id>?...
                val shortMatch = Regex("youtu\\.be/([A-Za-z0-9_-]{6,})").find(value)
                if (shortMatch != null) return shortMatch.groupValues[1]

                // https://youtube.com/shorts/<id>?...
                val shortsMatch = Regex("youtube\\.com/shorts/([A-Za-z0-9_-]{6,})").find(value)
                if (shortsMatch != null) return shortsMatch.groupValues[1]

                // https://youtube.com/watch?v=<id>
                val watchMatch = Regex("[?&]v=([A-Za-z0-9_-]{6,})").find(value)
                if (watchMatch != null) return watchMatch.groupValues[1]

                return ""
            }
        }
    }
}

package com.example.way.data.model

/**
 * Hardcoded self-defense content. Can be moved to Firestore in a future phase.
 */
object SelfDefenseContent {

    fun getAllItems(): List<SelfDefenseItem> = tips + videos

    fun getTips(): List<SelfDefenseItem> = tips

    fun getVideos(): List<SelfDefenseItem> = videos

    private val tips = listOf(
        SelfDefenseItem.TextTip(
            title = "Stay Aware of Your Surroundings",
            body = "Always keep your head up and avoid looking at your phone while walking, especially at night. Use one earbud instead of both so you can hear approaching people or vehicles. Walk in well-lit areas and avoid shortcuts through alleys or isolated paths."
        ),
        SelfDefenseItem.TextTip(
            title = "Trust Your Instincts",
            body = "If something feels wrong, it probably is. Don't ignore gut feelings — cross the street, enter a shop, or change your route. It's better to seem rude than to be in danger. Your safety is more important than being polite."
        ),
        SelfDefenseItem.TextTip(
            title = "Walk With Confidence",
            body = "Predators target people who look vulnerable. Walk with purpose, make brief eye contact with people around you, and keep a strong posture. This signals that you are alert and not an easy target."
        ),
        SelfDefenseItem.TextTip(
            title = "Share Your Location",
            body = "Always tell someone your route and expected arrival time. Use location-sharing apps with trusted contacts. WAY does this automatically during walk sessions — make sure to add your emergency contacts."
        ),
        SelfDefenseItem.TextTip(
            title = "Know Basic Self-Defense Moves",
            body = "Learn to strike vulnerable areas: eyes, nose, throat, groin, and knees. Use the heel of your palm for strikes. Practice breaking free from wrist grabs. Even basic knowledge can give you precious seconds to escape."
        ),
        SelfDefenseItem.TextTip(
            title = "Use Everyday Objects as Tools",
            body = "Keys held between fingers, an umbrella, a water bottle, or even a pen can be used as defensive tools. Hold your keys ready when walking to your car or home. A bright flashlight can temporarily blind an attacker."
        ),
        SelfDefenseItem.TextTip(
            title = "Establish Safe Checkpoints",
            body = "Identify safe locations along your regular routes — stores, restaurants, police stations, or well-populated areas. If you feel threatened, head toward the nearest checkpoint. Plan your routes to pass through these safe zones."
        ),
        SelfDefenseItem.TextTip(
            title = "The Power of Your Voice",
            body = "If confronted, yell 'FIRE!' instead of 'HELP!' — more people respond to fire. Be loud and assertive. Make a scene. Attract attention. Most attackers want an easy, quiet target and will flee when attention is drawn."
        )
    )

    private val videos = listOf(
        SelfDefenseItem.VideoTip(
            title = "3 Effective Self-Defense Moves",
            youtubeUrl = "https://youtu.be/KVpxP3ZZtAc?si=fECpsJEAEqFY4tJT"
        ),
        SelfDefenseItem.VideoTip(
            title = "Quick Street Safety Techniques",
            youtubeUrl = "https://youtu.be/M4_8PoRQP8w?si=vIG48CjiZ16Lu-P6"
        ),
        SelfDefenseItem.VideoTip(
            title = "Short Drill: Escape and Create Distance",
            youtubeUrl = "https://www.youtube.com/watch?v=QT6n94JvyWE"
        ),
        SelfDefenseItem.VideoTip(
            title = "Short Drill: Wrist Release Practice",
            youtubeUrl = "https://www.youtube.com/watch?v=T2Xc4mRURkc"
        ),
        SelfDefenseItem.VideoTip(
            title = "5 Self-Defense Moves Every Woman Should Know",
            youtubeUrl = "https://www.youtube.com/watch?v=T7aNSRoDCmg"
        )
    )
}

package com.reelmonster.app.monster

import androidx.compose.ui.graphics.Color

data class Stage(
    val index: Int,
    val name: String,
    val range: String,
    val body: Color,
    val belly: Color,
    val scale: Float
)

val STAGES = listOf(
    Stage(0, "Baby Blob", "0–9", Color(0xFF7BE0A4), Color(0xFFD5F8E4), 0.80f),
    Stage(1, "Snack Goblin", "10–24", Color(0xFF4FC3F7), Color(0xFFD3F1FD), 0.86f),
    Stage(2, "Scroll Sprite", "25–49", Color(0xFFB388FF), Color(0xFFE6DAFF), 0.92f),
    Stage(3, "Thumb Titan", "50–99", Color(0xFFFFB74D), Color(0xFFFFE8C6), 0.98f),
    Stage(4, "Doomscroll Demon", "100–199", Color(0xFFFF6E6E), Color(0xFFFFD6D6), 1.04f),
    Stage(5, "Reel Overlord", "200+", Color(0xFFC62FF0), Color(0xFFEFC9FB), 1.10f)
)

fun stageIndex(count: Int): Int = when {
    count >= 200 -> 5
    count >= 100 -> 4
    count >= 50 -> 3
    count >= 25 -> 2
    count >= 10 -> 1
    else -> 0
}

/** 42 light-hearted messages, 7 per stage. Teasing, never insulting. */
val STAGE_MESSAGES: List<List<String>> = listOf(
    listOf(
        "Fresh day, fresh monster! Zero guilt so far. Enjoy it.",
        "I just hatched. Please feed me only a few reels.",
        "Tiny blob, tiny count. Look at us being balanced!",
        "Is that a Reel in the wild? Bold of you to start the day with one.",
        "I'm still small and cute. Let's keep it that way, shall we?",
        "Your thumb is warming up. Mine too, mostly because I have no legs.",
        "A few quick videos? Nothing wrong with a snack-sized scroll."
    ),
    listOf(
        "Ten already? My eyes are getting wider... in a good way.",
        "I grew horns! Thanks for the algorithm-flavoured vitamins.",
        "Quick question: did you come here for something specific? Asking for a blob.",
        "This is fine. Totally fine. Just a casual little scroll.",
        "I'm gaining levels, you're gaining dance trends. Fair trade.",
        "Your thumb called. It wants a raise.",
        "Plot twist: the next video is exactly like the last one."
    ),
    listOf(
        "Look at us, scrolling like professionals.",
        "I've seen more cooking videos today than your fridge has seen ingredients.",
        "My antenna picked up a signal: 'Go drink some water'.",
        "You swipe, I grow. It's a very one-sided friendship.",
        "Twenty-five-ish videos in. Your thumb deserves a medal and a stretch.",
        "Somewhere, a to-do list is waving at you shyly.",
        "I'd suggest standing up for a minute. I'd do it myself, but I have no knees. You do!"
    ),
    listOf(
        "Fifty videos! Your thumb is basically a gym member.",
        "I've learned twelve life hacks today and applied exactly zero.",
        "Fun fact: the pet videos are great, but a real cuddle might be even better.",
        "I'm getting heavy over here. Maybe a quick walk break?",
        "The algorithm knows you better than your group chat now.",
        "Halfway to triple digits. Dramatic music, please.",
        "Pro tip: blinking is free and also recommended."
    ),
    listOf(
        "Triple digits! I'm a demon now and I look fabulous.",
        "Your screen time and my horns are both growing. Coincidence?",
        "Let's make a deal: three more reels, then we go outside.",
        "I have seen things. Mostly cats, but still.",
        "My eyes have more red lines than your calendar.",
        "Your thumb has run a marathon without leaving the couch.",
        "Gentle reminder: the sun still exists, and it misses you."
    ),
    listOf(
        "Two hundred! I bow before your legendary thumb.",
        "I'm the Overlord now. My first decree: stretch your neck.",
        "At this point, we should negotiate a lunch break.",
        "Achievement unlocked: Epic Scroll. Prize: a glass of water.",
        "The feed is endless but you are not. Snack and stretch time!",
        "I have three eyes and all of them need a blink.",
        "Tomorrow I reset to a baby blob. Let's see if you can keep me tiny."
    )
)

fun milestoneMessage(count: Int): String? = when {
    count == 1 -> "First one of the day! The monster stirs..."
    count == 5 -> "5 down. Warm-up complete!"
    count == 10 -> "Level up! Snack Goblin unlocked."
    count == 25 -> "25! Scroll Sprite energy detected."
    count == 50 -> "Half a hundred! Thumb Titan has arrived."
    count == 75 -> "75. The thumb union is requesting a break."
    count == 100 -> "TRIPLE DIGITS! Doomscroll Demon awakens."
    count == 150 -> "150! Even my horns are impressed."
    count == 200 -> "200!! The Reel Overlord is crowned."
    count > 200 && count % 50 == 0 -> "$count and counting. Hydrate, Overlord."
    else -> null
}

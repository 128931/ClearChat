package io.github.onetwoeight.clearchat.utilities

import kotlin.random.Random


/**
 * @author onetwoeight
 * @since 5/14/2022
 */
object Random {

    /**
     * Random Spaces Generator (RSG).
     * Randomly generates a number of spaces to be announced because of clients such as Lunar, Wurst, FDP, etc.
     * Have anti-spam features where it will stack a msg sent twice as "hello [x2]" and if we broadcast
     * a single space a thousand times for them, it would view as " [x1000]" not clearing their chat.
     *
     * @param length The number of random spaces that will be created
     * @return String with a random amount of spaces
     */
    @Suppress("KDocUnresolvedReference")
    fun nextSpace(length: Int): String {
        val random = Random.nextInt(length)
        val builder = StringBuilder(random)
        for (i in 0..random) {
            builder.append(" ")
        }
        return "$builder"
    }
}
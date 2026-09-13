package kodama.core.util

object BonsaiConstants {
    /**
     * Bonsai Flags:
     * - Baik -> Green
     * - Baik Sekali -> Red
     * - Best 10 -> Yellow
     * - Best in Class -> Blue
     * - Best in Show -> White
     *
     * As far as I know, Green and Red flags is the only mutually exclusive flag here.
     */
    object FlagsThreshold {
        const val GREEN = 280
        const val RED = 350
        const val FULL = 400
    }

}

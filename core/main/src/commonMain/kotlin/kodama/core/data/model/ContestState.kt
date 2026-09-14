package kodama.core.data.model

import androidx.compose.ui.graphics.vector.ImageVector
import kodama.resources.icons.draft_orders
import kodama.resources.icons.edit
import kodama.resources.icons.lock
import kodama.resources.icons.rate_review
import kodama.resources.icons.reviews
import kodama.resources.icons.schedule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ContestState(val symbol: ImageVector, val serialName: String) {
    @SerialName("draft") Draft(draft_orders, "draft"),
    @SerialName("accepting") Accepting(edit, "accepting"),
    @SerialName("closed") Closed(lock, "closed"),
    @SerialName("reviewing") Reviewing(rate_review, "reviewing"),
    @SerialName("review_done") ReviewDone(reviews, "review_done"),
    @SerialName("finished") Finished(schedule, "finished"),
    @SerialName("ended") Ended(schedule, "ended");
}

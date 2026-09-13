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
enum class ContestState(val symbol: ImageVector) {
    @SerialName("draft") Draft(draft_orders),
    @SerialName("accepting") Accepting(edit),
    @SerialName("closed") Closed(lock),
    @SerialName("reviewing") Reviewing(rate_review),
    @SerialName("review_done") ReviewDone(reviews),
    @SerialName("finished") Finished(schedule),
    @SerialName("ended") Ended(schedule);
}

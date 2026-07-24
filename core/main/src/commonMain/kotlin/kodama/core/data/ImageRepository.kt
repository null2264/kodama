package kodama.core.data

import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.BucketApi

class ImageRepository(private val storage: Storage) {

    private val bucket: BucketApi
        get() = storage.from("kodama-images")

    suspend fun uploadBonsaiPict(bonsaiId: String, bytes: ByteArray, mimeType: String = "image/webp"): String {
        val path = "bonsai/$bonsaiId/pict"
        bucket.upload(path, bytes) {
            this.contentType = mimeType
        }
        return path
    }

    suspend fun uploadContestBanner(contestId: String, bytes: ByteArray, mimeType: String = "image/webp"): String {
        val path = "contest/$contestId/banner"
        bucket.upload(path, bytes) {
            this.contentType = mimeType
        }
        return path
    }

    fun getPublicUrl(path: String): String {
        return bucket.publicUrl(path)
    }

    suspend fun deleteImage(path: String) {
        bucket.delete(listOf(path))
    }
}
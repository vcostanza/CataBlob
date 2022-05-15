package software.blob.catablob.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

/**
 * A collection of [Item] instances
 * @param uid Unique ID
 * @param name The name of this collection
 * @param imageUri Collection image URI
 * @param notes Miscellaneous notes related to this collection
 */
@Entity(tableName = TBL_COLLECTIONS)
data class ItemCollection(
    @PrimaryKey val uid: String = UUID.randomUUID().toString(),
    var name: String,
    var imageUri: String = "",
    var notes: String = "") {

    override fun toString() = name
}
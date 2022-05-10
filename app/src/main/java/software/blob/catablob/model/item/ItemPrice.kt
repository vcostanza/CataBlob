package software.blob.catablob.model.item

import java.util.*

/**
 * Represents the price of an [Item] in its native currency
 */
data class ItemPrice(val value: Double, val currency: Currency)
package software.blob.catablob.model.product

import android.content.Context
import software.blob.catablob.R
import software.blob.catablob.model.item.ItemCondition

/**
 * Product categories
 */
enum class ProductCategory {
    FOOD,
    HEALTH,
    PET,
    CLOTHING,
    SPORT,
    BOOKS,
    COLLECTABLES,
    TOYS,
    HOME,
    FURNITURE,
    TOOLS,
    MUSIC,
    ELECTRONICS,
    MEDIA,
    ARTS,
    BUSINESS,
    DIGITAL,
    OTHER;

    /**
     * Get the list of valid conditions for this product category
     */
    val conditions: List<ItemCondition> get() {
        return when (this) {
            HEALTH -> listOf(
                ItemCondition.NEW,
                ItemCondition.OPEN_BOX,
                ItemCondition.USED,
                ItemCondition.NOT_WORKING)
            BOOKS, MEDIA -> listOf(
                ItemCondition.NEW,
                ItemCondition.LIKE_NEW,
                ItemCondition.VERY_GOOD,
                ItemCondition.GOOD,
                ItemCondition.ACCEPTABLE,
                ItemCondition.DIGITAL
            )
            PET, COLLECTABLES, TOYS -> listOf(
                ItemCondition.NEW,
                ItemCondition.USED,
                ItemCondition.DIGITAL
            )
            CLOTHING, SPORT -> listOf(
                ItemCondition.NEW_WITH_TAGS,
                ItemCondition.NEW_WITHOUT_TAGS,
                ItemCondition.NEW_DEFECTS,
                ItemCondition.USED
            )
            ELECTRONICS, HOME, FURNITURE, TOOLS, MUSIC, ARTS, BUSINESS -> listOf(
                ItemCondition.NEW,
                ItemCondition.OPEN_BOX,
                ItemCondition.REFURBISHED,
                ItemCondition.USED,
                ItemCondition.NOT_WORKING
            )
            DIGITAL -> listOf(ItemCondition.DIGITAL)
            else -> listOf(ItemCondition.NEW)
        }
    }

    /**
     * Get the localized name/description for this product category
     * @param context Application context used for string resource lookup
     * @return Localized category name
     */
    fun getLocalizedName(context: Context): String {
        return context.getString(when (this) {
            FOOD -> R.string.category_food
            HEALTH -> R.string.category_health
            PET -> R.string.category_pet
            CLOTHING -> R.string.category_clothing
            SPORT -> R.string.category_sport
            BOOKS -> R.string.category_books
            COLLECTABLES -> R.string.category_collectables
            TOYS -> R.string.category_toys
            HOME -> R.string.category_home
            FURNITURE -> R.string.category_furniture
            TOOLS -> R.string.category_tools
            MUSIC -> R.string.category_music
            ARTS -> R.string.category_arts
            ELECTRONICS -> R.string.category_electronics
            MEDIA -> R.string.category_media
            BUSINESS -> R.string.category_business
            DIGITAL -> R.string.category_digital
            else -> R.string.category_other
        })
    }
}
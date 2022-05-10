package software.blob.catablob.model.item

/**
 * Various terms to describe the condition of an [Item]
 * Some conditions are only applicable to certain categories
 */
enum class ItemCondition {

    // Generic conditions
    NEW,
    LIKE_NEW,
    VERY_GOOD,
    GOOD,
    ACCEPTABLE,

    // CLothing
    NEW_WITH_TAGS,
    NEW_WITHOUT_TAGS,
    NEW_DEFECTS,

    // Electronics
    OPEN_BOX,
    REFURBISHED,
    USED,
    NOT_WORKING,

    // Entertainment/Digital good
    DIGITAL
}
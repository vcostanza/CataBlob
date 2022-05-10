package software.blob.catablob.model.item

/**
 * An [ArrayList] of [Item] instances
 * @param name The name of this collection
 * @param description A short description of this collection
 * @param initialCapacity Initial list capacity (default = 10)
 */
class ItemCollection(name: String, description: String = "", initialCapacity: Int = 10)
    : ArrayList<Item>(initialCapacity)
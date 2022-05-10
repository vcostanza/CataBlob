package software.blob.catablob.util

import android.database.Cursor

/**
 * Get a string value from a database cursor using the column name
 * @param columnName Column name
 * @return String value
 * @throws [IllegalArgumentException] if the column does not exist
 */
fun Cursor.getString(columnName: String): String = getString(getColumnIndexOrThrow(columnName))

/**
 * Get an integer value from a database cursor using the column name
 * @param columnName Column name
 * @return Integer value
 * @throws [IllegalArgumentException] if the column does not exist
 */
fun Cursor.getInt(columnName: String): Int = getInt(getColumnIndexOrThrow(columnName))
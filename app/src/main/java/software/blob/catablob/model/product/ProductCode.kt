package software.blob.catablob.model.product

import com.google.zxing.BarcodeFormat
import com.google.zxing.Result

/**
 * Simple data class for a product/bar code and its format
 * @param code Product code string
 * @param format Code format
 */
data class ProductCode(var code: String, var format: BarcodeFormat) {

    /**
     * Convert Zebra Crossing query result to [ProductCode]
     * @param res Zxing result
     */
    constructor(res: Result) : this(res.text, res.barcodeFormat)
}
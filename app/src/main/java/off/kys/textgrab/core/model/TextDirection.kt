package off.kys.textgrab.core.model

/**
 * Lightweight, allocation-free RTL detection covering the Arabic and Hebrew
 * Unicode blocks (including Arabic Supplement, Extended-A and presentation forms).
 * Good enough to choose a layout direction per line without pulling in ICU.
 */
fun String.isRtl(): Boolean {
    for (ch in this) {
        val code = ch.code
        val rtl = code in 0x0590..0x05FF ||   // Hebrew
            code in 0x0600..0x06FF ||          // Arabic
            code in 0x0750..0x077F ||          // Arabic Supplement
            code in 0x08A0..0x08FF ||          // Arabic Extended-A
            code in 0xFB1D..0xFDFF ||          // Hebrew + Arabic Presentation Forms-A
            code in 0xFE70..0xFEFF             // Arabic Presentation Forms-B
        if (rtl) return true
    }
    return false
}

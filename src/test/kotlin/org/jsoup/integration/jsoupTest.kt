package org.jsoup.integration

import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class JsoupTest {
    @Test
    fun jsoupTest() {
        val doc = Jsoup.parse("<b>jsoup")
        doc.body()
        // It's odd to me that (prior to adding notnulll annotations), the Kotlin compiler doesn't NPE warn on this, as .parse() is !, which includes ?.
        // And compiler / Intellij doesn't have a code inspect for it either. At least have it be an option
        Assertions.assertEquals("<b>jsoup</b>", doc.body().html())
    }
}

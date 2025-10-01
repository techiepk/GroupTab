import org.junit.Test
import kotlin.test.assertEquals

class CategoryMappingTest {

    @Test
    fun testCategoryMapping() {
        // Test some key merchants from each category
        assertEquals("Food & Dining", getCategory("Piri Piri Flaming Grill"))
        assertEquals("Food & Dining", getCategory("Naixue"))
        assertEquals("Food & Dining", getCategory("BN-Icon Siam"))
        assertEquals("Food & Dining", getCategory("Amici"))

        assertEquals("Food & Dining", getCategory("PF Changs"))
        assertEquals("Food & Dining", getCategory("Bombay Bungalow"))
        assertEquals("Food & Dining", getCategory("Heytea"))
        assertEquals("Food & Dining", getCategory("Google Nomadtable"))


        assertEquals("Groceries", getCategory("7-11"))

        assertEquals("Transportation", getCategory("Airports of Thailand"))
        assertEquals("Transportation", getCategory("Expressway"))
        assertEquals("Transportation", getCategory("Grab A-123"))
        assertEquals("Transportation", getCategory("SATS T1"))
        assertEquals("Transportation", getCategory("Pyxbolt Services"))

        assertEquals("Shopping", getCategory("Uniqlo TRX"))
        assertEquals("Shopping", getCategory("Lyn"))
        assertEquals("Shopping", getCategory("Apple Central World"))
        assertEquals("Shopping", getCategory("OpenAI ChatGPT"))
        assertEquals("Shopping", getCategory("Sukhumvit City Mall"))
        assertEquals("Shopping", getCategory("The Emsphere"))
        assertEquals("Shopping", getCategory("Central World"))

        assertEquals("Entertainment", getCategory("Major Cineplex"))
        assertEquals("Entertainment", getCategory("Ticketmelon"))
        assertEquals("Entertainment", getCategory("2C2P Major Cineplex"))

        assertEquals("Travel", getCategory("Four Points by Sheraton"))
        assertEquals("Travel", getCategory("Crowne Plaza KLCC"))
        assertEquals("Travel", getCategory("Dusit Thani Bangkok"))
        assertEquals("Travel", getCategory("Marina Bay Sands"))
        assertEquals("Travel", getCategory("Hilton Garden Inn"))
        assertEquals("Travel", getCategory("WESTIN KL-FRONT OFFICE KUALA LUMPUR MY MY"))

        assertEquals("Healthcare", getCategory("Life Pharm"))
        assertEquals("Healthcare", getCategory("Bumrungrad"))
        assertEquals("Healthcare", getCategory("Medex"))

        assertEquals("Personal Care", getCategory("Sultans of Shave"))
        assertEquals("Personal Care", getCategory("Mandarin Oriental Spa"))
        assertEquals("Personal Care", getCategory("Truefitt and Hill"))

        assertEquals("Tax", getCategory("Abu Dhabi Judicial Dep"))
        assertEquals("Tax", getCategory("Sharjah Finance Depart"))

        assertEquals("Bills & Utilities", getCategory("Tamdeed Projects"))

        assertEquals("Banking", getCategory("My Fatoorah"))

        // Test some should remain as Others (too generic)
        assertEquals("Others", getCategory("The Empire Tower"))
        assertEquals("Others", getCategory("Phetsathorn Co.,Ltd."))
        assertEquals("Others", getCategory("Twin Made"))
    }
}

// Helper function to test categorization
private fun getCategory(merchantName: String): String {
    return com.pennywiseai.tracker.ui.icons.CategoryMapping.getCategory(merchantName)
}
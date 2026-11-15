package ai.deulleo.mobile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 폰 앱의 핵심 사용자 흐름을 단계별로 따라가며 확인합니다. */
class MobileStoreTest {

    private fun ids(s: MobileStore) = s.state.rows.map { it.id }

    @Test
    fun `home shows leave time and reasons for today`() {
        val s = MobileStore()
        assertEquals("14:15", s.state.leaveAt)
        assertEquals(3, s.state.rows.size)
        assertTrue(s.state.rows.all { it.reason != null })
        assertNotNull(s.state.vehicle)
    }

    @Test
    fun `adding the same item twice does not create duplicate rows`() {
        val s = MobileStore()
        s.addItem("케이크"); s.addItem("케이크")
        assertEquals(ids(s).distinct(), ids(s))
        assertEquals(1, s.state.rows.count { it.category == "빵집" })
    }

    @Test
    fun `adding something that cannot be found keeps the plan and says so`() {
        val s = MobileStore()
        val before = ids(s)
        s.addItem("asdf")
        assertEquals(before, ids(s))
        assertTrue(s.state.rows.none { it.changed }, "없는 품목인데 기존 일정이 바뀐 것으로 표시됨")
        assertTrue(s.state.notice?.body?.contains("찾지 못했어요") == true)
    }

    @Test
    fun `very long or blank input is ignored safely`() {
        val s = MobileStore()
        val before = ids(s)
        s.addItem("   "); s.addItem("가".repeat(500))
        assertEquals(before, ids(s))
    }

    @Test
    fun `screenshot add works once and moves the florist next to the bakery`() {
        val s = MobileStore()
        s.addFromScreenshot(); s.addFromScreenshot()
        assertEquals(1, s.state.rows.count { it.title == "케이크 찾기" })
        assertEquals("늦은꽃 플라워", s.state.rows.first { it.id == "flower" }.placeName)
        assertEquals("문자", s.state.rows.first { it.title == "케이크 찾기" }.source)
    }

    @Test
    fun `screenshot dated today is added to today even when another day is open`() {
        val s = MobileStore()
        s.selectDay(19)
        s.addFromScreenshot()
        assertEquals(TODAY, s.state.day, "11월 14일 예약이 19일 동선에 들어감")
        assertTrue(s.state.rows.any { it.title == "케이크 찾기" })
    }

    @Test
    fun `vehicle care can be removed and restored`() {
        val s = MobileStore()
        s.remove("care-TIRE_PRESSURE")
        assertFalse(ids(s).contains("care-TIRE_PRESSURE"))
        assertEquals(false, s.state.vehicle?.included)
        s.restoreCare()
        assertTrue(ids(s).contains("care-TIRE_PRESSURE"))
        assertEquals(true, s.state.vehicle?.included)
    }

    @Test
    fun `swapping a shop keeps the errand and updates the place`() {
        val s = MobileStore()
        s.openDetail("flower")
        val alt = s.state.alternatives.first()
        assertTrue(alt.cost.contains("집"))
        s.swapTo(alt.placeId)
        assertEquals(alt.name, s.state.rows.first { it.id == "flower" }.placeName)
        assertEquals(Overlay.NONE, s.state.overlay)
    }

    @Test
    fun `accepting the suggestion adds a cafe labelled as a suggestion and hides it`() {
        val s = MobileStore()
        assertNotNull(s.state.recommendation)
        s.acceptRecommendation()
        assertNull(s.state.recommendation)
        assertEquals("추천", s.state.rows.first { it.category == "카페" }.source)
    }

    @Test
    fun `disconnecting the car removes the vehicle notice and its stop`() {
        val s = MobileStore()
        s.setCarConnected(false)
        assertNull(s.state.vehicle)
        assertFalse(ids(s).contains("care-TIRE_PRESSURE"))
    }

    @Test
    fun `pickup day explains the waiting spot`() {
        val s = MobileStore()
        s.selectDay(19)
        assertEquals("16:00", s.state.leaveAt)
        assertTrue(s.state.rows.first { it.id == "pickup" }.reason!!.contains("자양 공영주차장"))
        assertNull(s.state.vehicle)
    }

    @Test
    fun `an empty day accepts a new stop`() {
        val s = MobileStore()
        s.selectDay(16)
        assertTrue(s.state.rows.isEmpty())
        s.addItem("약")
        assertEquals(1, s.state.rows.size)
    }

    @Test
    fun `back closes overlays one step at a time`() {
        val s = MobileStore()
        s.open(Overlay.MAP); s.open(Overlay.HANDOFF)
        s.back(); assertEquals(Overlay.MAP, s.state.overlay)
        s.back(); assertEquals(Overlay.NONE, s.state.overlay)
        s.openDetail("없는-id"); assertEquals(Overlay.NONE, s.state.overlay)
    }
}

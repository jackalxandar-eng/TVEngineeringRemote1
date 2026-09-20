package com.openai.tvengineering.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IrProtocolsTest {
    @Test fun necFrameHasExpectedShape() {
        val p = IrProtocols.nec(0x20, 0x10)
        assertEquals(67, p.size)
        assertEquals(9000, p[0])
        assertEquals(4500, p[1])
        assertEquals(560, p.last())
        assertTrue(p.sum() < 2_000_000)
    }

    @Test fun rawParserAcceptsCommonSeparators() {
        val p = IrProtocols.parseRawMicros("9000, 4500 560;560\n1690")
        assertEquals(listOf(9000,4500,560,560,1690), p.toList())
    }

    @Test fun pulse32HasExpectedShape() {
        val p = IrProtocols.pulseDistance32("20DF10EF")
        assertEquals(67, p.size)
    }
}

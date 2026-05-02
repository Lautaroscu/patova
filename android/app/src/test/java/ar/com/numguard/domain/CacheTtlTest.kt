package ar.com.numguard.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CacheTtlTest {

    @Test
    fun `ttl for BLOCK is 24 hours`() {
        val ttl = VerdictDecision.ttlMillisForVerdict("BLOCK")
        assertEquals(24L * 60 * 60 * 1000, ttl)
    }

    @Test
    fun `ttl for ALLOW is 7 days`() {
        val ttl = VerdictDecision.ttlMillisForVerdict("ALLOW")
        assertEquals(7L * 24 * 60 * 60 * 1000, ttl)
    }

    @Test
    fun `ttl for SUSPECT is 30 minutes`() {
        val ttl = VerdictDecision.ttlMillisForVerdict("SUSPECT")
        assertEquals(30L * 60 * 1000, ttl)
    }

    @Test
    fun `ttl for UNKNOWN is 5 minutes`() {
        val ttl = VerdictDecision.ttlMillisForVerdict("UNKNOWN")
        assertEquals(5L * 60 * 1000, ttl)
    }

    @Test
    fun `ttl for INVALID_PREFIX is 24 hours`() {
        val ttl = VerdictDecision.ttlMillisForVerdict("INVALID_PREFIX")
        assertEquals(24L * 60 * 60 * 1000, ttl)
    }

    @Test
    fun `ttl for unknown verdict defaults to 5 minutes`() {
        val ttl = VerdictDecision.ttlMillisForVerdict("BOGUS")
        assertEquals(5L * 60 * 1000, ttl)
    }

    @Test
    fun `ttl for BLOCK is greater than SUSPECT`() {
        val blockTtl = VerdictDecision.ttlMillisForVerdict("BLOCK")
        val suspectTtl = VerdictDecision.ttlMillisForVerdict("SUSPECT")
        assertTrue(blockTtl > suspectTtl)
    }

    @Test
    fun `ttl for ALLOW is the longest`() {
        val allowTtl = VerdictDecision.ttlMillisForVerdict("ALLOW")
        val blockTtl = VerdictDecision.ttlMillisForVerdict("BLOCK")
        assertTrue(allowTtl > blockTtl)
    }

    @Test
    fun `maskE164 hides middle digits`() {
        val masked = VerdictDecision.maskE164("+541112345678")
        assertTrue(masked.startsWith("+541"))
        assertTrue(masked.contains("****"))
        assertTrue(masked.endsWith("78"))
        assertEquals("+541****78", masked)
    }

    @Test
    fun `maskE164 short number returns only asterisks`() {
        val masked = VerdictDecision.maskE164("+54")
        assertEquals("****", masked)
    }

    @Test
    fun `sha256 hash is 64 hex characters`() {
        val hash = PhoneHashing.sha256("+541112345678")
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in "0123456789abcdef" })
    }

    @Test
    fun `sha256 hash is deterministic`() {
        val hash1 = PhoneHashing.sha256("+541112345678")
        val hash2 = PhoneHashing.sha256("+541112345678")
        assertEquals(hash1, hash2)
    }

    @Test
    fun `sha256 hash differs for different inputs`() {
        val hash1 = PhoneHashing.sha256("+541112345678")
        val hash2 = PhoneHashing.sha256("+541198765432")
        assertFalse(hash1 == hash2)
    }
}

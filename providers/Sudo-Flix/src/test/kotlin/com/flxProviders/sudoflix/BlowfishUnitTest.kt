package com.flxProviders.sudoflix

import com.flxProviders.sudoflix.api.primewire.util.decrypt.getLinks
import org.junit.Test


internal class BlowfishUnitTest {
    private val encryptedInput = "cUH+bKi3kLvoVjEsQghrx+lRwbzg/ZsoJGTMSAYsVegNSLfucc93ureO1nVSiR/F/TM5UXQheFOpNZK66Mm2kghItI1w54qbhvi3br4fh9UTUAlmVZqiug==cq6D2-FRAG"

    @Test
    fun testBlowfish() {
        val links = getLinks(encryptedInput)
        val correctKeys = listOf("Fjf29", "B16CO", "FOOXk", "alOxm", "eJIxn", "93cLz", "NNo5K", "UeVIC", "Y0old", "FA7Nl", "RTaQj", "PLfum", "H_KTs", "y3EQy", "4CViL", "esVlN", "ef8Og", "000")

        assert(correctKeys == links)
    }
}
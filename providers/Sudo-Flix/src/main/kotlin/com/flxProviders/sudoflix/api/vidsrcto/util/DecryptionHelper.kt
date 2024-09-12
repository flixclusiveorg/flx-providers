package com.flxProviders.sudoflix.api.vidsrcto.util

import android.util.Base64
import com.flxProviders.sudoflix.api.vidsrcto.VIDSRCTO_KEY
import java.net.URLDecoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

internal object VidSrcToDecryptionUtil {
    fun decodeUrl(encryptedSourceUrl: String): String {
        val standardizedInput = encryptedSourceUrl
            .replace('_', '/')
            .replace('-', '+')

        var data = standardizedInput.toByteArray()
        data = Base64.decode(data, Base64.DEFAULT)
        val rc4Key = SecretKeySpec(VIDSRCTO_KEY.toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)
        data = cipher.doFinal(data)
        return URLDecoder.decode(data.toString(Charsets.UTF_8), "utf-8")
    }
}
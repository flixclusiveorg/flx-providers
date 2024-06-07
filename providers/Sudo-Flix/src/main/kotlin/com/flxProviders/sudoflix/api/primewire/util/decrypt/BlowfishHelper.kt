package com.flxProviders.sudoflix.api.primewire.util.decrypt

fun getLinks(encryptedInput: String): List<String> {
    val key = encryptedInput.takeLast(10)
    val data = encryptedInput.dropLast(10)
    val cipher = Blowfish(key)
    val decryptedData = cipher.decrypt(cipher.base64(data))
        .chunked(5)  // This splits the string into chunks of size 5

    return decryptedData
}
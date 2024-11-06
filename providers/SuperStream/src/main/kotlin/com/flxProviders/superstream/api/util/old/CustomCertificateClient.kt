package com.flxProviders.superstream.api.util.old

import com.flixclusive.core.util.log.errorLog
import okhttp3.OkHttpClient
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.InputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

internal const val CLIENT_CERT_NAME: String = "cert"
internal const val CLIENT_KEY_NAME: String = "key"
internal const val CLIENT_CA_NAME: String = "ca"
internal const val CLIENT_CA4_NAME: String = "ca4"

internal class CustomCertificateClient {

    fun createOkHttpClient(
        clientCertStream: InputStream,
        caCertStreams: List<InputStream>,
        privateKeyInputStream: InputStream,
        privateKeyPassword: CharArray? = null
    ): OkHttpClient {
        val certificateFactory = CertificateFactory.getInstance("X.509")

        // Load client certificate
        val clientCertificate = clientCertStream.use {
            certificateFactory.generateCertificate(it) as X509Certificate
        }

        // Load CA certificates
        val caCertificates = caCertStreams.flatMap { stream ->
            stream.use {
                loadCertificatesFromText(it)
            }
        }

        // Load private key with updated method
        val privateKey = privateKeyInputStream.use { loadPrivateKey(it) }

        // Create client KeyStore (for client authentication)
        val clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        clientKeyStore.load(null, null)
        clientKeyStore.setKeyEntry(
            "client-key",
            privateKey,
            privateKeyPassword,
            arrayOf(clientCertificate)
        )

        // Set up KeyManager for client authentication
        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(clientKeyStore, privateKeyPassword)
        val keyManagers = keyManagerFactory.keyManagers

        // Create trust store with all certificates
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
        trustStore.load(null, null)

        // Add client certificate
        trustStore.setCertificateEntry("client-cert", clientCertificate)

        // Add all CA certificates
        caCertificates.forEachIndexed { index, cert ->
            trustStore.setCertificateEntry("ca-cert-$index", cert)
        }

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(trustStore)
        val trustManagers = trustManagerFactory.trustManagers
        val trustManager = trustManagers[0] as X509TrustManager

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagers, trustManagers, null)

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .build()
    }

    private fun loadPrivateKey(privateKeyInputStream: InputStream): PrivateKey {
        val pemParser = PEMParser(privateKeyInputStream.reader())
        val pemObject = pemParser.readObject()

        val converter = JcaPEMKeyConverter()

        return when (pemObject) {
            is PEMKeyPair -> {
                converter.getPrivateKey(pemObject.privateKeyInfo)
            }
            is PrivateKeyInfo -> {
                converter.getPrivateKey(pemObject)
            }
            else -> {
                throw IllegalArgumentException("Unsupported private key format")
            }
        }
    }

    private fun loadCertificatesFromText(inputStream: InputStream): List<X509Certificate> {
        val certificates = mutableListOf<X509Certificate>()
        val certificateFactory = CertificateFactory.getInstance("X.509")

        val certificatesText = inputStream.bufferedReader().use { it.readText() }

        certificatesText.split("-----BEGIN CERTIFICATE-----")
            .filter { it.isNotBlank() }
            .forEach { certificatePart ->
                // Reconstruct the certificate block with its BEGIN and END markers
                val fullCertificate = "-----BEGIN CERTIFICATE-----$certificatePart"

                // Convert the string to an InputStream and parse it
                fullCertificate.byteInputStream().use { certStream ->
                    try {
                        val certificate = certificateFactory.generateCertificate(certStream) as X509Certificate
                        certificates.add(certificate)
                    } catch (e: Exception) {
                        errorLog("Error parsing certificate: ${e.message}")
                    }
                }
            }

        inputStream.close()

        return certificates
    }
}

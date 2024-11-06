package com.flxProviders.superstream.util

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

class CustomCertificateClient {
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
        val caCertificates = caCertStreams.map { stream ->
            stream.use { certificateFactory.generateCertificate(it) as X509Certificate }
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
}

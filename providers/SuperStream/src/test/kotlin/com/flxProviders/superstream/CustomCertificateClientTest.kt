package com.flxProviders.superstream

import com.flxProviders.superstream.util.CustomCertificateClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream

class CustomCertificateClientTest {
    
    private lateinit var certificateClient: CustomCertificateClient
    private lateinit var testCertPath: String
    private lateinit var testKeyPath: String
    private lateinit var testCaPath: String
    private lateinit var testCa4Path: String

    @Before
    fun setup() {
        certificateClient = CustomCertificateClient()
        // Update these paths to match your project's test resources directory
        testCertPath = "src/test/certs/cert.crt"
        testKeyPath = "src/test/certs/key.key"
        testCaPath = "src/test/certs/ca.crt"
    }

    @Test
    fun `test certificate client creation with valid certificates`() = scope.runTest {
        // Given
        val certFile = File(testCertPath)
        val keyFile = File(testKeyPath)
        val caFile = File(testCaPath)

        val certStream = FileInputStream(certFile)
        val keyStream = FileInputStream(keyFile)
        val caStream = FileInputStream(caFile)

        // When
        val client = certificateClient.createOkHttpClient(
            clientCertStream = certStream,
            caCertStreams = listOf(caStream),
            privateKeyInputStream = keyStream,
            privateKeyPassword = null
        )

        // Then
        assertNotNull(client)
        assertTrue(client is OkHttpClient)
    }

    @Test(expected = Exception::class)
    fun `test certificate client creation with invalid certificate`() = scope.runTest {
        // Given
        val invalidCertContent = "invalid certificate content"
        val invalidCertStream = ByteArrayInputStream(invalidCertContent.toByteArray())
        val keyFile = File(testKeyPath)
        val caFile = File(testCaPath)

        val keyStream = FileInputStream(keyFile)
        val caStream = FileInputStream(caFile)

        // When/Then - should throw exception
        certificateClient.createOkHttpClient(
            clientCertStream = invalidCertStream,
            caCertStreams = listOf(caStream),
            privateKeyInputStream = keyStream,
            privateKeyPassword = null
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = TestScope(UnconfinedTestDispatcher())

    @Test
    fun `test complete certificate chain validation`() = scope.runTest {
        // Given
        val certFile = File(testCertPath)
        val keyFile = File(testKeyPath)
        val caFile = File(testCaPath)
        val ca4File = File("src/test/certs/ca4.cer")

        val certStream = FileInputStream(certFile)
        val keyStream = FileInputStream(keyFile)
        val caStream = FileInputStream(caFile)
        val ca4Stream = FileInputStream(ca4File)

        // When
        val client = certificateClient.createOkHttpClient(
            clientCertStream = certStream,
            caCertStreams = listOf(caStream, ca4Stream),
            privateKeyInputStream = keyStream,
            privateKeyPassword = null
        )

        // Then
        assertNotNull(client)
        // Additional assertions can be added here to verify SSL configuration
    }
}
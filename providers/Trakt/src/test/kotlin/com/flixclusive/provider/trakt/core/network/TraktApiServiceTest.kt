package com.flixclusive.provider.trakt.core.network

import org.junit.jupiter.api.BeforeEach

class TraktApiServiceTest {
    private lateinit var client: OkHttpClient
    private lateinit var apiService: TraktApiService

    @BeforeEach
    fun setUp() {
        apiService = TraktApiService.create()
    }

}
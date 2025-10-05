package com.example.configcat

import com.configcat.ConfigCatClient
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class ConfigCatDemoApplicationTests {
    @MockkBean(name = "userManagementConfigCatClient")
    private lateinit var userManagementClient: ConfigCatClient

    @MockkBean(name = "paymentConfigCatClient")
    private lateinit var paymentClient: ConfigCatClient

    @Test
    fun contextLoads() {
        // This test verifies that the Spring context loads successfully with mocked ConfigCat clients
    }
}

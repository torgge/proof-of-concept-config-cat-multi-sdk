package com.example.configcat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ConfigCatDemoApplication

fun main(args: Array<String>) {
    runApplication<ConfigCatDemoApplication>(*args)
}

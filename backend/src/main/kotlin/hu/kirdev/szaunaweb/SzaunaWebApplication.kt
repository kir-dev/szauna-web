package hu.kirdev.szaunaweb

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SzaunaWebApplication

fun main(args: Array<String>) {
    runApplication<SzaunaWebApplication>(*args)
}

package hu.kirdev.szaunaweb

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class SzaunaWebApplication

fun main(args: Array<String>) {
    runApplication<SzaunaWebApplication>(*args)
}

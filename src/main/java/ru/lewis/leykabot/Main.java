package ru.lewis.leykabot;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class Main {

    @PostConstruct
    public void init() {
        // O'zbekiston (Toshkent, UTC+5) vaqt mintaqasini butun tizim uchun o'rnatish
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tashkent"));
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tashkent"));
        SpringApplication.run(Main.class, args);
    }
}
package com.comp5619.libraryreservationreviewhub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.comp5619.libraryreservationreviewhub.mapper")
public class LibraryReservationReviewHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryReservationReviewHubApplication.class, args);
    }

}

package com.helospark.financialdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinancialDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancialDataApplication.class, args);
    }

}

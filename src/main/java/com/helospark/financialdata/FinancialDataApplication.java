package com.helospark.financialdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.helospark.financialdata.management.user.repository.User;
import com.helospark.financialdata.management.watchlist.repository.JobLastRunData;

@SpringBootApplication
@EnableScheduling
public class FinancialDataApplication {

    public static void main(String[] args) {
        System.out.println("Application classloader: "
                + FinancialDataApplication.class.getClassLoader());

        System.out.println("User classloader: "
                + User.class.getClassLoader());

        System.out.println("JobLastRunData classloader: "
                + JobLastRunData.class.getClassLoader());

        SpringApplication.run(FinancialDataApplication.class, args);
    }

}

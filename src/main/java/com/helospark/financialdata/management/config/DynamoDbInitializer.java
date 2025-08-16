package com.helospark.financialdata.management.config;

import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTimeToLiveRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTimeToLiveResult;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TimeToLiveSpecification;
import com.amazonaws.services.dynamodbv2.model.UpdateTableRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateTimeToLiveRequest;
import com.helospark.financialdata.management.payment.repository.StripeUserMapping;
import com.helospark.financialdata.management.payment.repository.UserLastPayment;
import com.helospark.financialdata.management.screener.repository.Screener;
import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.management.user.repository.ConfirmationEmail;
import com.helospark.financialdata.management.user.repository.PersistentSignin;
import com.helospark.financialdata.management.user.repository.User;
import com.helospark.financialdata.management.user.repository.UserRepository;
import com.helospark.financialdata.management.user.repository.ViewedStocks;
import com.helospark.financialdata.management.watchlist.repository.JobLastRunData;
import com.helospark.financialdata.management.watchlist.repository.PortfolioPerformanceHistory;
import com.helospark.financialdata.management.watchlist.repository.Watchlist;
import com.helospark.financialdata.management.watchlist.repository.WatchlistExpectationHistory;

import jakarta.annotation.PostConstruct;

@Component
public class DynamoDbInitializer {
    private static final String ADMIN_EMAIL = "admin@longtermstockfundamentals.com";
    private static final String ROOT_EMAIL = "root@longtermstockfundamentals.com";
    @Autowired
    AmazonDynamoDB amazonDynamoDB;
    @Autowired
    DynamoDBMapper mapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    @PostConstruct
    public void createTables() {
        boolean wasUserTableCreated = createTable("User", User.class);
        createTable("PersistentSignin", PersistentSignin.class);
        createTable("ViewedStocks", ViewedStocks.class);
        boolean wasConfirmationEmailTableCreated = createTable("ConfirmationEmail", ConfirmationEmail.class);
        createTable("StripeUserMapping", StripeUserMapping.class);
        createTable("UserLastPayment", UserLastPayment.class);
        createTable("JobLastRunData", JobLastRunData.class);
        createTable("PortfolioPerformanceHistory", PortfolioPerformanceHistory.class);
        createTableWithProvisioning("Watchlist", Watchlist.class, 5L, 5L);
        createTable("WatchlistExpectationHistory", WatchlistExpectationHistory.class);
        createTable("Screener", Screener.class);

        if (wasUserTableCreated || userRepository.findByEmail(ADMIN_EMAIL).isEmpty()) {
            User user = new User();
            user.setAccountType(AccountType.ADMIN);
            user.setActivated(true);
            user.setEmail(ADMIN_EMAIL);
            user.setPassword("$2a$10$a83Kk3OS5I.HUR7i8G8NkOlTOIQ6XMGk/YUUGtVr2rRm7M6345ufu");
            user.setRegistered(LocalDate.now().toString());
            userRepository.save(user);
        }
        if (wasUserTableCreated || userRepository.findByEmail(ROOT_EMAIL).isEmpty()) {
            User user = new User();
            user.setAccountType(AccountType.ADMIN);
            user.setActivated(true);
            user.setEmail(ROOT_EMAIL);
            user.setPassword(passwordEncoder.encode("changeme1"));
            user.setRegistered(LocalDate.now().toString());
            userRepository.save(user);
        }
        if (wasConfirmationEmailTableCreated) {
            UpdateTimeToLiveRequest ttlRequest = new UpdateTimeToLiveRequest();
            ttlRequest.setTableName("ConfirmationEmail");
            ttlRequest.setTimeToLiveSpecification(new TimeToLiveSpecification().withEnabled(true).withAttributeName("expiration"));
            amazonDynamoDB.updateTimeToLive(ttlRequest);
        }
        if (!isTimeToLiveEnabled("PersistentSignin")) {
            UpdateTimeToLiveRequest ttlRequest = new UpdateTimeToLiveRequest();
            ttlRequest.setTableName("PersistentSignin");
            ttlRequest.setTimeToLiveSpecification(new TimeToLiveSpecification().withEnabled(true).withAttributeName("expiration"));
            amazonDynamoDB.updateTimeToLive(ttlRequest);
        }

    }

    private void migrateToProvisionedBillingMode(String string, long read, long write) {
        UpdateTableRequest updateTableRequest = new UpdateTableRequest(string, new ProvisionedThroughput(read, write));
        amazonDynamoDB.updateTable(updateTableRequest);
    }

    private boolean isProvisionedTable(String string) {
        DescribeTableRequest describeRequest = new DescribeTableRequest(string);
        DescribeTableResult describeTableResult = amazonDynamoDB.describeTable(describeRequest);
        return describeTableResult.getTable().getBillingModeSummary().getBillingMode().equals("PROVISIONED");
    }

    public boolean isTimeToLiveEnabled(String tableName) {
        DescribeTimeToLiveRequest ttlDescribeRequest = new DescribeTimeToLiveRequest().withTableName(tableName);
        DescribeTimeToLiveResult hasTimeToLive = amazonDynamoDB.describeTimeToLive(ttlDescribeRequest);
        return !hasTimeToLive.getTimeToLiveDescription().getTimeToLiveStatus().equals("DISABLED");
    }

    public boolean createTable(String tableName, Class<?> class1) {
        if (!doesTableExist(tableName)) {
            CreateTableRequest tableRequest = mapper.generateCreateTableRequest(class1);
            tableRequest.setBillingMode("PAY_PER_REQUEST");
            amazonDynamoDB.createTable(tableRequest);

            for (int i = 0; i < 10; ++i) {
                if (doesTableExist(tableName)) {
                    break;
                } else {
                    System.out.println("Waiting for " + tableName + " table to be created");
                    exceptionlessSleep(1);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean forceRecreateTable(String tableName, Class<?> class1) {
        if (doesTableExist(tableName)) {
            amazonDynamoDB.deleteTable(tableName);
        }
        CreateTableRequest tableRequest = mapper.generateCreateTableRequest(class1);
        tableRequest.setBillingMode("PAY_PER_REQUEST");
        amazonDynamoDB.createTable(tableRequest);

        for (int i = 0; i < 10; ++i) {
            if (doesTableExist(tableName)) {
                break;
            } else {
                System.out.println("Waiting for " + tableName + " table to be created");
                exceptionlessSleep(1);
            }
        }
        return true;
    }

    public boolean createTableWithProvisioning(String tableName, Class<?> class1, Long provisionedRead, Long provisionedWrite) {
        if (!doesTableExist(tableName)) {
            CreateTableRequest tableRequest = mapper.generateCreateTableRequest(class1);
            tableRequest.setBillingMode("PROVISIONED");
            tableRequest.setProvisionedThroughput(new ProvisionedThroughput(provisionedRead, provisionedWrite));
            amazonDynamoDB.createTable(tableRequest);

            for (int i = 0; i < 10; ++i) {
                if (doesTableExist(tableName)) {
                    break;
                } else {
                    System.out.println("Waiting for " + tableName + " table to be created");
                    exceptionlessSleep(1);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private void exceptionlessSleep(int i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean doesTableExist(String tableName) {
        try {
            return amazonDynamoDB.describeTable(tableName) != null;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

}

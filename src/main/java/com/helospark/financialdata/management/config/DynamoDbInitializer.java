package com.helospark.financialdata.management.config;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.helospark.financialdata.management.chartorder.ChartOrder;
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
import com.helospark.financialdata.management.watchlist.repository.PortfolioTransaction;
import com.helospark.financialdata.management.watchlist.repository.Watchlist;
import com.helospark.financialdata.management.watchlist.repository.WatchlistElement;
import com.helospark.financialdata.management.watchlist.repository.WatchlistExpectationHistory;
import com.helospark.financialdata.management.watchlist.repository.WatchlistMigrationService;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTimeToLiveRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTimeToLiveResponse;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TimeToLiveSpecification;
import software.amazon.awssdk.services.dynamodb.model.TimeToLiveStatus;
import software.amazon.awssdk.services.dynamodb.model.UpdateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveRequest;

@Component
public class DynamoDbInitializer {
    private static final String ADMIN_EMAIL = "admin@longtermstockfundamentals.com";
    private static final String ROOT_EMAIL = "root@longtermstockfundamentals.com";
    @Autowired
    DynamoDbClient dynamoDbClient;
    @Autowired
    UserRepository userRepository;
    @Autowired
    BCryptPasswordEncoder passwordEncoder;
    @Autowired
    WatchlistMigrationService watchlistMigrationService;
    @Autowired
    DynamoDbEnhancedClient dynamoDbEnhancedClient;

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
        boolean wasSegmentedCreated = createTableWithProvisioning("WatchlistSegmented", WatchlistElement.class, 5L, 5L);

        if (wasSegmentedCreated) {
            watchlistMigrationService.migrateAllWatchlists();
        }

        createTable("WatchlistExpectationHistory", WatchlistExpectationHistory.class);
        createTable("Screener", Screener.class);
        createTable("PortfolioTransactionT", PortfolioTransaction.class);
        createTable("ChartOrder", ChartOrder.class);

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
            setExpirationToTable("ConfirmationEmail");
        }
        if (!isTimeToLiveEnabled("PersistentSignin")) {
            setExpirationToTable("PersistentSignin");
        }

    }

    public void setExpirationToTable(String tableNameToSetExpirationTo) {
        UpdateTimeToLiveRequest request = UpdateTimeToLiveRequest.builder()
                .tableName(tableNameToSetExpirationTo)
                .timeToLiveSpecification(
                        TimeToLiveSpecification.builder()
                                .enabled(true)
                                .attributeName("expiration")
                                .build())
                .build();

        dynamoDbClient.updateTimeToLive(request);
    }

    private void migrateToProvisionedBillingMode(String tableName, long read, long write) {
        UpdateTableRequest request = UpdateTableRequest.builder()
                .tableName(tableName)
                .billingMode(BillingMode.PROVISIONED)
                .provisionedThroughput(
                        ProvisionedThroughput.builder()
                                .readCapacityUnits(read)
                                .writeCapacityUnits(write)
                                .build())
                .build();

        dynamoDbClient.updateTable(request);
    }

    private boolean isProvisionedTable(String tableName) {
        DescribeTableResponse response = dynamoDbClient.describeTable(
                DescribeTableRequest.builder()
                        .tableName(tableName)
                        .build());

        return response.table()
                .billingModeSummary()
                .billingMode() == BillingMode.PROVISIONED;
    }

    public boolean isTimeToLiveEnabled(String tableName) {
        DescribeTimeToLiveResponse response = dynamoDbClient.describeTimeToLive(
                DescribeTimeToLiveRequest.builder()
                        .tableName(tableName)
                        .build());

        return response.timeToLiveDescription()
                .timeToLiveStatus() != TimeToLiveStatus.DISABLED;
    }

    public boolean createTable(String tableName, Class<?> clazz) {
        if (!doesTableExist(tableName)) {
            DynamoDbTable<?> table = dynamoDbEnhancedClient.table(
                    tableName,
                    TableSchema.fromBean(clazz));

            table.createTable();

            return true;
        }

        return false;
    }

    public boolean forceRecreateTable(String tableName, Class<?> clazz) {
        if (doesTableExist(tableName)) {
            dynamoDbClient.deleteTable(
                    DeleteTableRequest.builder()
                            .tableName(tableName)
                            .build());
        }

        createTable(tableName, clazz);

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
        return createTable(tableName, class1); // TODO: Temporary removed with v1
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
            dynamoDbClient.describeTable(
                    DescribeTableRequest.builder()
                            .tableName(tableName)
                            .build());
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

}

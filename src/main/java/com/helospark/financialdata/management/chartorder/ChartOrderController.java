package com.helospark.financialdata.management.chartorder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helospark.financialdata.management.user.LoginController;
import com.helospark.financialdata.management.user.repository.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@RestController
public class ChartOrderController {
    @Autowired
    private ChartOrderRepository repository;
    @Autowired
    private LoginController loginController;
    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/chart-order")
    public Map<String, String> getChartOrders(HttpServletRequest request) {
        User user = loginController.findUserOrThrow(request);
        List<ChartOrder> chartOrders = repository.getAll(user.getEmail());

        Map<String, String> result = new LinkedHashMap<>();

        result.putAll(getBuiltInCustomUis(result));

        for (var element : chartOrders) {
            result.put(element.getName(), element.getFormatJson());
        }

        return result;
    }

    private Map<String, String> getBuiltInCustomUis(Map<String, String> result) {
        ObjectMapper objectMapper = new ObjectMapper();
        ClassPathResource resource = new ClassPathResource("data/built-in-custom-ui.json");

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<Map<String, String>>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    @DeleteMapping("/chart-order")
    public void getChartOrders(HttpServletRequest request, @NotNull @RequestParam("name") String name) {
        User user = loginController.findUserOrThrow(request);

        repository.delete(user.getEmail(), name);
    }

    @PostMapping("/chart-order")
    public void getChartOrders(HttpServletRequest request, @Valid @RequestBody ChartOrderRequestObject body) throws JsonProcessingException {
        User user = loginController.findUserOrThrow(request);

        ChartOrder chartOrder = new ChartOrder();
        chartOrder.setUserEmail(user.getEmail());
        chartOrder.setName(body.name);
        chartOrder.setFormatJson(objectMapper.writeValueAsString(body.order));

        repository.save(chartOrder);
    }

    public static class ChartOrderKeyValue {
        @NotNull
        public String id;
        public boolean enabled;
    }

    public static class ChartOrderRequestObject {
        @NotBlank
        @NotNull
        public String name;
        @NotNull
        @NotEmpty
        public List<ChartOrderKeyValue> order;
    }
}

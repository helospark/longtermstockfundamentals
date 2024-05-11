package com.helospark.financialdata.service;

import java.util.Optional;

import com.helospark.financialdata.management.watchlist.domain.Moats;

public class MoatScoreCalculator {
    private static final double NETWORK_EFFECT_WEIGHT = 1;
    private static final double SWITCHING_COST_WEIGHT = 0.8;
    private static final double ECONOMY_OF_SCALE_WEIGHT = 0.6;

    private static final double BRAND_WEIGHT = 0.7;
    private static final double INTANGIBLES_WEIGHT = 0.7;
    private static final double COST_ADVANTAGE_WEIGHT = 0.5;

    public static Optional<Double> calculate(Moats moats) {
        double total = 0.0;
        double sum = 0.0;

        if (moats == null) {
            return Optional.empty();
        }

        sum += (moats.networkEffect / 5.0) * NETWORK_EFFECT_WEIGHT;
        total += NETWORK_EFFECT_WEIGHT;

        sum += (moats.switchingCost / 5.0) * SWITCHING_COST_WEIGHT;
        total += SWITCHING_COST_WEIGHT;

        sum += (moats.economyOfScale / 5.0) * ECONOMY_OF_SCALE_WEIGHT;
        total += ECONOMY_OF_SCALE_WEIGHT;

        sum += (moats.brand / 5.0) * BRAND_WEIGHT;
        total += BRAND_WEIGHT;

        sum += (moats.intangibles / 5.0) * INTANGIBLES_WEIGHT;
        total += INTANGIBLES_WEIGHT;

        sum += (moats.costAdvantage / 5.0) * COST_ADVANTAGE_WEIGHT;
        total += COST_ADVANTAGE_WEIGHT;

        if (sum < 0.01) {
            return Optional.empty();
        }

        return Optional.of((sum / total) * 10.0);
    }

}

package com.helospark.financialdata.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.helospark.financialdata.domain.Profile;
import com.helospark.financialdata.service.DataLoader;

public class FindAlternateTickers {

    public static void main(String[] args) {
        Map<String, List<String>> nameToProfile = new HashMap<>();
        Map<String, Profile> profiles = new ConcurrentHashMap<>();

        int index = 0;
        int total = DataLoader.provideAllSymbols().size();

        DataLoader.provideAllSymbols().parallelStream().forEach(symbol -> {
            if (profiles.size() % 1000 == 0) {
                System.out.println((int) (((double) profiles.size() / total) * 100) + " %");
            }
            Profile profile = null;
            List<Profile> readProfile = DataLoader.readFinancialFile(symbol, "profile.json", Profile.class);
            if (!readProfile.isEmpty()) {
                profile = readProfile.get(0);
            } else {
                profile = new Profile();
                profile.symbol = symbol;
            }
            profiles.put(symbol, profile);
        });

        index = 0;
        for (var symbol : DataLoader.provideAllSymbols()) {
            ++index;
            if (index % 1000 == 0) {
                System.out.println((int) (((double) index / total) * 100) + " %");
            }
            Profile profile = profiles.get(symbol);

            if (profile != null && profile.companyName != null && !profile.companyName.isBlank()) {
                if (nameToProfile.containsKey(profile.companyName)) {
                    nameToProfile.get(profile.companyName).add(profile.symbol);
                } else {
                    ArrayList<String> newList = new ArrayList<>();
                    newList.add(profile.symbol);
                    nameToProfile.put(profile.companyName, newList);
                }
            }
        }

        for (var element : nameToProfile.entrySet()) {
            if (element.getValue().size() > 1) {
                System.out.println(element.getKey() + "\t\t\t" + element.getValue());
            }
        }
    }

}

package ru.lewis.leykabot.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.lewis.leykabot.model.database.entity.PriceSetting;
import ru.lewis.leykabot.repository.PriceSettingRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceService {

    private final PriceSettingRepository repository;
    private final Map<String, Integer> priceCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Initial user requested prices in So'm
        initDefault("STAR_50", 12000);
        initDefault("STAR_100", 23000);
        initDefault("STAR_150", 34000);
        initDefault("STAR_250", 53000);
        initDefault("STAR_350", 78000);
        initDefault("STAR_500", 110000);
        initDefault("STAR_750", 160000);
        initDefault("STAR_1000", 215000);
        initDefault("STAR_PER_UNIT", 230);

        initDefault("PREMIUM_1", 50000);
        initDefault("PREMIUM_3", 170000);
        initDefault("PREMIUM_6", 230000);
        initDefault("PREMIUM_12", 300000);

        initDefault("PUBG_60", 11000);
        initDefault("PUBG_325", 55000);
        initDefault("PUBG_660", 110000);
        initDefault("PUBG_1800", 275000);
        initDefault("PUBG_3850", 545000);
        initDefault("PUBG_8100", 1090000);
    }

    private void initDefault(String key, int defaultVal) {
        PriceSetting setting = repository.findById(key).orElseGet(() -> {
            PriceSetting s = new PriceSetting(key, defaultVal);
            return repository.save(s);
        });
        priceCache.put(key, setting.getPrice());
    }

    public int getPrice(String key, int fallback) {
        return priceCache.getOrDefault(key, fallback);
    }

    public void setPrice(String key, int price) {
        priceCache.put(key, price);
        repository.save(new PriceSetting(key, price));

        if ("STAR_PER_UNIT".equals(key)) {
            int[] amounts = {50, 100, 150, 250, 350, 500, 750, 1000};
            for (int a : amounts) {
                int pkgPrice = (int) (Math.round((a * price) / 100.0) * 100);
                priceCache.put("STAR_" + a, pkgPrice);
                repository.save(new PriceSetting("STAR_" + a, pkgPrice));
            }
        }
    }

    public int getStarsPrice(int amount) {
        String key = "STAR_" + amount;
        if (priceCache.containsKey(key)) {
            return priceCache.get(key);
        }
        int perUnit = priceCache.getOrDefault("STAR_PER_UNIT", 230);
        return (int) (Math.round((amount * perUnit) / 100.0) * 100);
    }

    public int getPremiumPrice(int months) {
        String key = "PREMIUM_" + months;
        return priceCache.getOrDefault(key, switch (months) {
            case 1 -> 50000;
            case 3 -> 170000;
            case 6 -> 230000;
            case 12 -> 300000;
            default -> months * 35000;
        });
    }

    public int getPubgPrice(int uc, int fallback) {
        String key = "PUBG_" + uc;
        return priceCache.getOrDefault(key, fallback);
    }

    public Map<Integer, Integer> getAllStarPrices() {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        map.put(50, getPrice("STAR_50", 12000));
        map.put(100, getPrice("STAR_100", 23000));
        map.put(150, getPrice("STAR_150", 34000));
        map.put(250, getPrice("STAR_250", 53000));
        map.put(350, getPrice("STAR_350", 78000));
        map.put(500, getPrice("STAR_500", 110000));
        map.put(750, getPrice("STAR_750", 160000));
        map.put(1000, getPrice("STAR_1000", 215000));
        return map;
    }

    public Map<Integer, Integer> getAllPremiumPrices() {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        map.put(1, getPrice("PREMIUM_1", 50000));
        map.put(3, getPrice("PREMIUM_3", 170000));
        map.put(6, getPrice("PREMIUM_6", 230000));
        map.put(12, getPrice("PREMIUM_12", 300000));
        return map;
    }

    public Map<Integer, Integer> getAllPubgPrices() {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        map.put(60, getPrice("PUBG_60", 11000));
        map.put(325, getPrice("PUBG_325", 55000));
        map.put(660, getPrice("PUBG_660", 110000));
        map.put(1800, getPrice("PUBG_1800", 275000));
        map.put(3850, getPrice("PUBG_3850", 545000));
        map.put(8100, getPrice("PUBG_8100", 1090000));
        return map;
    }
}

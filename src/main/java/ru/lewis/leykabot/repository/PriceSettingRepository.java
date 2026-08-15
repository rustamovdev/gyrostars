package ru.lewis.leykabot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.lewis.leykabot.model.database.entity.PriceSetting;

@Repository
public interface PriceSettingRepository extends JpaRepository<PriceSetting, String> {
}

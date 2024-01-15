package com.burakack.bistMatrix.repository;

import com.burakack.bistMatrix.entity.StockData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockDataRepository extends JpaRepository<StockData, Long> {

    Optional<StockData> findByHisseAdi(String name);
}
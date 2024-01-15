package com.burakack.bistMatrix.service;


import com.burakack.bistMatrix.entity.StockData;
import com.burakack.bistMatrix.repository.StockDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StockDataService {
    @Autowired
    private StockDataRepository stockDataRepository;

    public void saveStockData(StockData stockData) {
        stockDataRepository.save(stockData);
    }

    @Transactional
    public Optional<StockData> findByStockName(String name) {
        return stockDataRepository.findByHisseAdi(name);

    }

    //get all stock data
    @Transactional
    public List<StockData> getAllStockData() {
        return stockDataRepository.findAll();
    }
}

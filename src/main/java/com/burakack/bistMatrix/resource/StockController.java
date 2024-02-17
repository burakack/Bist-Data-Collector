package com.burakack.bistMatrix.resource;


import com.burakack.bistMatrix.entity.StockData;
import com.burakack.bistMatrix.service.StockDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock")
public class StockController {

    @Autowired
    private StockDataService stockDataService;


    @GetMapping
    public StockData getStockDatawithName(@RequestParam String name) {
        return stockDataService.findByStockName(name.toUpperCase()).orElse(null);
    }

    @GetMapping("/all")
    public List<StockData> getAllStockData() {
        return stockDataService.getAllStockData();
    }


}

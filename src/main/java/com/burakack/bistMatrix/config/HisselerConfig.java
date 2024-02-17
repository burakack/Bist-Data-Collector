package com.burakack.bistMatrix.config;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class HisselerConfig {

    private final RestTemplate restTemplate;

    //logger
    private static final Logger logger = LoggerFactory.getLogger(HisselerConfig.class);

    @Value("${data.hisselerurl}")
    private String hisselerurl;

    public HisselerConfig(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Bean
    public Hisseler myHisseData() {
        Hisseler data = new Hisseler();

        String pageContent = restTemplate.getForObject(hisselerurl, String.class);

        String currency = Jsoup.parse(pageContent).select(".currency").text();

        currency = currency.replaceAll("\\s+", ",");
        String[] tokens = currency.split(",");
        Map<String, Integer> tokenCount = new HashMap<>();

        for (String token : tokens) {
            tokenCount.put(token, tokenCount.getOrDefault(token, 0) + 1);
        }

        StringBuilder resultBuilder = new StringBuilder();
        for (String token : tokens) { if (tokenCount.get(token) == 1) {
                resultBuilder.append(token).append(",");
            }
        }

        String result = resultBuilder.toString().replaceAll(",$", "");

        logger.info("Hisseler: \n" + result);


        data.setHisseler(result.split(","));

        return data;
    }
}
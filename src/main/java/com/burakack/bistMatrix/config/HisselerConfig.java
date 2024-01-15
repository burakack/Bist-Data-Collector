package com.burakack.bistMatrix.config;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

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

        logger.info("Hisseler: \n" + currency);


        data.setHisseler(currency.split(","));
        return data;
    }
}
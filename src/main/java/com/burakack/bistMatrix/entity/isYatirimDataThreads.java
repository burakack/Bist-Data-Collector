package com.burakack.bistMatrix.entity;

import com.burakack.bistMatrix.config.Hisseler;
import com.burakack.bistMatrix.dto.isYatirimResponse;
import com.burakack.bistMatrix.service.StockDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Component
public class isYatirimDataThreads implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(isYatirimDataThreads.class);

    //get urls from application.properties
    private String[] urls;


    private final Hisseler hisseler;


    @Value("${data.isyatirim.url}")
    private String isyatirimUrl;

    @Value("${data.isyatirim.url2}")
    private String isyatirimUrl2;


    @Autowired
    private StockDataService stockDataService;

    private final RestTemplate restTemplate;

    @Autowired
    public isYatirimDataThreads(Hisseler hisseler, RestTemplate restTemplate) {
        this.hisseler = hisseler;
        this.restTemplate = restTemplate;
    }

    @Override
    public void run(String... args) {

        //hissleri baseurl ile birlestirip urls arrayine atiyoruz
        urls = new String[hisseler.getHisseler().length];
        for (int i = 0; i < hisseler.getHisseler().length; i++) {

            urls[i] = isyatirimUrl + hisseler.getHisseler()[i];
        }

        if (urls == null) {
            System.out.println("URLs not configured. Please check your configuration.");
            return;
        }

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(urls.length);


        for (String url : urls) {
            //tüm threadleri ayni anda baslatma ki cok fazla istek olmasın
            int initialDelay = 0;
            for (int i = 0; i < hisseler.getHisseler().length; i++) {
                if (urls[i].equals(url)) {
                    initialDelay = i;
                    break;
                }
            }

            executorService.scheduleAtFixedRate(() -> {
                try {

                    //veritabaninda var mi kontrol
                    StockData stockData = stockDataService.findByStockName(url.substring(url.lastIndexOf("=") + 1)).stream().findFirst()
                            .orElse(new StockData());


                    stockData.setHisseAdi(url.substring(url.lastIndexOf("=") + 1));


                    //istek at json donuyor
                    String url2 = restTemplate.getForObject(isyatirimUrl2 + stockData.getHisseAdi() + ".E.BIST", String.class);
                    ObjectMapper objectMapper = new ObjectMapper();

                    //bos degilse datayı al
                    if (url2 != null) {
                        List<isYatirimResponse> isYatirimResponse = objectMapper.readValue(url2, new TypeReference<List<isYatirimResponse>>() {
                        });

                        for (isYatirimResponse data : isYatirimResponse) {
                            if (data.getLast() != 0)
                                stockData.setFiyat(data.getLast());
                            if (data.getDailyChange() != 0)
                                stockData.setGunlukDegisim(data.getDailyChange());
                            if (data.getDailyChangePercentage() != 0)
                                stockData.setGunlukDegisimYuzdesi(data.getDailyChangePercentage());
                        }
                    }

                    //diger isyatirim istegi
                    String pageContent = restTemplate.getForObject(url, String.class);
                    Document doc = Jsoup.parse(pageContent);

                    //sayfadan verileri al
                    Element tipElement = doc.select(".tip span").first();
                    String tip = (tipElement != null) ? tipElement.text() : "Belirtilmemiş";

                    Element hedefFiyatElement = doc.select(".center li:nth-child(2) span").first();
                    String hedefFiyat = (hedefFiyatElement != null) ? hedefFiyatElement.text() : null;

                    Element getiriPotElement = doc.select(".center li:nth-child(3) span").first();
                    String getiriPot = (getiriPotElement != null) ? getiriPotElement.text() : "Belirtilmemiş";

                    Element oneriTarihiElement = doc.select(".stock-offer .center ul li:nth-child(1) span").first();
                    String oneriTarihi = (oneriTarihiElement != null) ? oneriTarihiElement.text() : "Belirtilmemiş";

                    if (hedefFiyat != null) {
                        //once noktayı çıkart
                        hedefFiyat = hedefFiyat.replace(".", "");
                        //virgülü noktaya cevir
                        if (!hedefFiyat.equals("-"))
                            stockData.setIsyatirimHedefFiyat(Float.parseFloat(hedefFiyat.replace(",", ".")));
                    }

                    // Güncelleme işlemleri
                    stockData.setIsyatirimOneri(tip);
                    stockData.setIsyatirimPotansiyel(getiriPot);
                    stockData.setIsyatirimOneriTarih(oneriTarihi);

                    logger.info("name = " + url.substring(url.lastIndexOf("=") + 1));
                    logger.info("fiyat = " + stockData.getFiyat());
                    logger.info("hedefFiyat = " + hedefFiyat);
                    logger.info("günlük değişim = " + stockData.getGunlukDegisim());
                    logger.info("günlük değişim yüzdesi = " + stockData.getGunlukDegisimYuzdesi());
                    logger.info("oneriTarihi = " + oneriTarihi);
                    logger.info("getiriPot = " + getiriPot);
                    logger.info("is yatırım öneri = " + tip);
                    logger.info("------------------------------------");
                    stockDataService.saveStockData(stockData);
                } catch (Exception e) {
                    logger.error("Error in data thread" + url, url, e);
                }

            }, initialDelay, 250, TimeUnit.SECONDS);
        }
    }
}

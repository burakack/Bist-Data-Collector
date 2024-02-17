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

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Component
public class DataThreads implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataThreads.class);

    private String[] urls;


    private final Hisseler hisseler;


    @Value("${data.isyatirim.url}")
    private String isyatirimUrl;

    @Value("${data.isyatirim.url2}")
    private String isyatirimUrl2;

    @Value("${data.temmettuhisseler.url}")
    private String temetturluHisselerUrl;


    @Autowired
    private StockDataService stockDataService;

    private final RestTemplate restTemplate;

    @Autowired
    public DataThreads(Hisseler hisseler, RestTemplate restTemplate) {
        this.hisseler = hisseler;
        this.restTemplate = restTemplate;
    }

    @Override
    public void run(String... args) {

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
                    StockData stockData = stockDataService.findByStockName(url.substring(url.lastIndexOf("=") + 1)).stream().findFirst().orElse(new StockData());

                    if(stockData.getHisseAdi() == null)
                        stockData.setHisseAdi(url.substring(url.lastIndexOf("=") + 1));

                    //isyatirim istegi
                    String url2 = restTemplate.getForObject(isyatirimUrl2 + stockData.getHisseAdi() + ".E.BIST", String.class);
                    ObjectMapper objectMapper = new ObjectMapper();

                    //bos degilse datayı al
                    if (url2 != null) {
                        List<isYatirimResponse> isYatirimResponse = objectMapper.readValue(url2, new TypeReference<List<isYatirimResponse>>() {
                        });

                        for (isYatirimResponse data : isYatirimResponse) {
                             stockData.setFiyat(data.getLast());
                            stockData.setGunlukDegisim(data.getDailyChange());

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

                    Element pddd = doc.select("#ctl00_ctl58_g_76ae4504_9743_4791_98df_dce2ca95cc0d > div.box-content > div > table > tbody > tr:nth-child(3) > td").first();
                    String PdDd = (pddd != null) ? pddd.text() : "Belirtilmemiş";

                    Element fk = doc.select("#ctl00_ctl58_g_76ae4504_9743_4791_98df_dce2ca95cc0d > div.box-content > div > table > tbody > tr:nth-child(1) > td").first();
                    String fkk = (pddd != null) ? fk.text() : "Belirtilmemiş";

                    if (!PdDd.equals("-") &&! PdDd.equals("A/D") )
                        stockData.setPiyasaDegeriDefterDegeri(Float.parseFloat(PdDd.replace(",", ".")));
                    if (!fkk.equals("-") &&! fkk.equals("A/D") )
                        stockData.setFiyatKazancOrani(Float.parseFloat(fkk.replace(",", ".")));

                    if (!oneriTarihi.equals("Belirtilmemiş")) {
                        //tarih formatını degistir
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
                        Date date = dateFormat.parse(oneriTarihi);
                        stockData.setIsyatirimOneriTarih(date);
                    } else {
                        stockData.setIsyatirimOneriTarih(null);
                    }


                    if (hedefFiyat != null) {
                        //once noktayı çıka
                        hedefFiyat = hedefFiyat.replace(".", "");
                        //virgülü noktaya cevir
                        if (!hedefFiyat.equals("-"))
                            stockData.setIsyatirimHedefFiyat(Float.parseFloat(hedefFiyat.replace(",", ".")));
                    }

                    // Güncelleme işlemleri
                    stockData.setIsyatirimOneri(tip);
                    stockData.setIsyatirimPotansiyel(getiriPot);
                    //temettu hisseleri istegi
                    String temettuUrl = "https://www.isyatirim.com.tr/tr-tr/analiz/hisse-senedi-analiz/temettu-hisseleri";

                    pageContent = restTemplate.getForObject(temetturluHisselerUrl, String.class);
                    doc = Jsoup.parse(pageContent);



                    logger.debug("Ad = " + url.substring(url.lastIndexOf("=") + 1));
                    logger.debug("Fiyat = " + stockData.getFiyat());
                    logger.debug("İŞ yatırım Hedef Fiyatı = " + hedefFiyat);
                    logger.debug("TL Bazında Günlük Değişim = " + stockData.getGunlukDegisim());
                    logger.debug("Günlük Değişim Yüzdesi = " + stockData.getGunlukDegisimYuzdesi());
                    logger.debug("Öneri Tarihi = " + oneriTarihi);
                    logger.debug("Getiri Potansiyeli = " + getiriPot);
                    logger.debug("İs Yatırım Önerisi = " + tip);
                    logger.debug("Piyasa Degeri / Defter Değeri = " + PdDd);
                    logger.debug("------------------------------------");
                    stockDataService.saveStockData(stockData);
                } catch (Exception e) {
                    logger.error("Error in data thread" + url, url, e);
                }

            }, initialDelay, 250, TimeUnit.SECONDS);
        }
    }
}

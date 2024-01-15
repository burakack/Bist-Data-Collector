package com.burakack.bistMatrix.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Entity
@Data
public class StockData {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ad", nullable = true)
    private String hisseAdi;

    @Column(name = "fiyat", nullable = true)
    private Float fiyat;

    @Column(name = "isyatirim_tip", nullable = true)
    private String isyatirimOneri;

    @Column(name = "isyatirim_hedefFiyat", nullable = true)
    private Float isyatirimHedefFiyat;

    @Column(name = "isyatirim_potansiyel", nullable = true)
    private String isyatirimPotansiyel;

    @Column(name = "isyatirim_oneri_tarih", nullable = true)
    private String isyatirimOneriTarih;

    @Column(name = "gunluk_degisim", nullable = true)
    private Float gunlukDegisim;

    @Column(name = "gunluk_degisim_yuzdesi", nullable = true)
    private Float gunlukDegisimYuzdesi;


    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_date",updatable = false)
    private Date createDate;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_date")
    private Date modifyDate;


}

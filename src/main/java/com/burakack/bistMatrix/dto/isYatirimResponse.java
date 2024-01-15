package com.burakack.bistMatrix.dto;


import lombok.Data;

@Data
public class isYatirimResponse {
    private Float dailyChangePercentage;
    private Float dailyChange;
    private String c;
    private Float last;
    private Float dailyVolume;
    private Float previousDayClose;
    private String description;

    // Getter ve Setter metotları

    @Override
    public String toString() {
        return "MyObject{" +
                "dailyChangePercentage=" + dailyChangePercentage +
                ", dailyChange=" + dailyChange +
                ", c='" + c + '\'' +
                ", last=" + last +
                ", dailyVolume=" + dailyVolume +
                ", previousDayClose=" + previousDayClose +
                ", description='" + description + '\'' +
                '}';
    }
}
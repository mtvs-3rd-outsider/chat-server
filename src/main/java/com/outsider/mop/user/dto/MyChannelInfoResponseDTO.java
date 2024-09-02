package com.outsider.mop.user.dto;

import lombok.Data;

@Data
public class MyChannelInfoResponseDTO {
    private String name;
    private String username;
    private String bio;
    private String location;
    private String website;
    private String birthdate;
    private String joinedDate;
    private String gender;
    private int points;
    private int transactions;
    private String profitRate;
    private String positionValue;
    private int tradeCount;
    private int followingCount;
    private int followerCount;

    public MyChannelInfoResponseDTO(String name, String username, String bio, String location, String website, String birthdate, String joinedDate, String gender, int points, int transactions, String profitRate, String positionValue, int tradeCount, int followingCount, int followerCount) {
        this.name = name;
        this.username = username;
        this.bio = bio;
        this.location = location;
        this.website = website;
        this.birthdate = birthdate;
        this.joinedDate = joinedDate;
        this.gender = gender;
        this.points = points;
        this.transactions = transactions;
        this.profitRate = profitRate;
        this.positionValue = positionValue;
        this.tradeCount = tradeCount;
        this.followingCount = followingCount;
        this.followerCount = followerCount;
    }
}

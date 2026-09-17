package com.sds.cleancode.restaurant;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TestableBookingScheduler extends BookingScheduler {
    private final String datetime;
    public TestableBookingScheduler(int capacityPerHour, String datetime){
        super(capacityPerHour);
        this.datetime = datetime;
    }

    @Override
    public LocalDateTime getNow(){
        DateTimeFormatter format = DateTimeFormatter.ofPattern(("yyyy/MM/dd HH:mm"));
        return LocalDateTime.parse(datetime, format);
    }
}

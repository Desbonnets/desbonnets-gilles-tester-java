package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }

    public void calculateFare(Ticket ticket, boolean discount){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        long inHour = ticket.getInTime().getTime();
        long outHour = ticket.getOutTime().getTime();

        double duration = (double) (outHour - inHour) / (1000 * 60 * 60);

        if(duration <= 0.5){
            ticket.setPrice(0);
            return;
        }

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                BigDecimal bd = new BigDecimal(duration * Fare.CAR_RATE_PER_HOUR * (discount ? 0.95 : 1));
                bd = bd.setScale(3, RoundingMode.HALF_UP);
                ticket.setPrice(bd.doubleValue());
                break;
            }
            case BIKE: {
                BigDecimal bd = new BigDecimal(duration * Fare.BIKE_RATE_PER_HOUR * (discount ? 0.95 : 1));
                bd = bd.setScale(3, RoundingMode.HALF_UP);
                ticket.setPrice(bd.doubleValue());
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }
    }
}
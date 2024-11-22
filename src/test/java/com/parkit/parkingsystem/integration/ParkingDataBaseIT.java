package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private String vehicleRegNumber = "ABCDEF";

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegNumber);
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }

    /**
     * Tests the process of parking a car. Verifies that a ticket is created and that the parking spot
     * is marked as unavailable. Additionally, it checks if the vehicle registration number on the ticket
     * matches the expected one.
     */
    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);
        assertNotNull(ticket, "Le ticket ne doit pas être null");
        assertEquals(vehicleRegNumber, ticket.getVehicleRegNumber(), "Le numéro de véhicule doit correspondre");

        ParkingSpot parkingSpot = ticket.getParkingSpot();
        assertFalse(parkingSpot.isAvailable(), "Le parking doit être marqué comme non disponible");
    }

    /**
     * Tests the process of a vehicle exiting the parking lot after being parked. Verifies that the
     * exit time is set correctly and that the fare is calculated based on the parking duration.
     * The test simulates a one-hour parking time for the vehicle.
     */
    @Test
    public void testParkingLotExit(){
        testParkingACar();

        Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - ( 60 * 60 * 1000)); // Une heure dans le passé
        ticket.setInTime(inTime);
        ticketDAO.saveTicket(ticket);

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();

        ticket = ticketDAO.getTicket(vehicleRegNumber);

        System.out.println(ticket.getOutTime());

        assertNotNull(ticket.getOutTime(), "L'heure de sortie ne doit pas être null");
        assertTrue(ticket.getPrice() > 0, "Le tarif doit être supérieur à 0");
    }

    /**
     * Tests the process of a recurring user (i.e., a user who has previously parked) exiting the parking lot.
     * Verifies that a 5% discount is applied to the fare based on the duration of parking.
     * The test simulates a one-hour parking for the returning user and checks that the correct discounted fare is calculated.
     */
    @Test
    public void testParkingLotExitRecurringUser() {

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // Une heure dans le passé
        ticketDAO.saveTicket(ticket);

        parkingService.processExitingVehicle();

        ticket = ticketDAO.getTicket(vehicleRegNumber);

        // Vérifier que le prix a été calculé avec la remise de 5%
        BigDecimal bd = new BigDecimal(Fare.CAR_RATE_PER_HOUR * Fare.DISCOUNT);
        bd = bd.setScale(3, RoundingMode.HALF_EVEN);// Arrondis 3 chiffres après la virgule
        double expectedPrice = bd.doubleValue();
        double actualPrice = ticket.getPrice();

        assertEquals(expectedPrice, actualPrice);
    }

}

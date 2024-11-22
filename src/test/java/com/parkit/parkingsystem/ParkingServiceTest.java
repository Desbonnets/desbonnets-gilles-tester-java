package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach
    private void setUpPerTest() {
        try {
            lenient().when(inputReaderUtil.readSelection()).thenReturn(1); // 1 pour CAR ou 2 pour BIKE
            lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");
            lenient().when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            lenient().when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
            lenient().when(ticketDAO.getNbTicket("DISCOUNT")).thenReturn(2);
            lenient().when(ticketDAO.getNbTicket("NOT_DISCOUNT")).thenReturn(0);

            lenient().when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
            lenient().when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(2);

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void processExitingVehicleTest(){
        parkingService.processExitingVehicle();

        verify(ticketDAO, times(1)).getTicket(anyString());
        verify(ticketDAO, times(1)).getNbTicket(anyString());
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
    }

    @Test
    void testProcessIncomingVehicle(){

        parkingService.processIncomingVehicle();

        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
        verify(ticketDAO, times(1)).getNbTicket(anyString());
    }

    @Test
    void processExitingVehicleTestUnableUpdate () {
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        parkingService.processExitingVehicle();

        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, times(0)).updateParking(any(ParkingSpot.class));  // car la mise à jour échoue
    }

    @Test
    void testGetNextParkingNumberIfAvailable () {

        assertEquals(2, parkingService.getNextParkingNumberIfAvailable().getId());
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(any(ParkingType.class));
    }

    @Test
    void testGetNextParkingNumberIfAvailableParkingNumberNotFound () {
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0);

        assertEquals(null, parkingService.getNextParkingNumberIfAvailable());
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(any(ParkingType.class));
    }

    @Test
    void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
        when(inputReaderUtil.readSelection()).thenReturn(3); // 1 pour CAR ou 2 pour BIKE

        assertEquals(null, parkingService.getNextParkingNumberIfAvailable());
        verify(parkingSpotDAO, times(0)).getNextAvailableSlot(any(ParkingType.class));
    }

}

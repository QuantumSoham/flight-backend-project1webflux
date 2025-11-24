package com.flightapp.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class PnrGeneratorTest {

    @Test
    void generatedPnrIsNotNullOrEmpty() {
        String flightNumber = "AI-102";
        String seatSignature = "12A-12B";

        String pnr = PnrGenerator.generatePnr(flightNumber, seatSignature);

        assertNotNull(pnr, "PNR should not be null");
        assertFalse(pnr.isEmpty(), "PNR should not be empty");
    }

    @Test
    void pnrHasCorrectPrefixAndLength() {
        String flightNumber = "AI-102";
        String seatSignature = "12A-12B";

        // replicate the prefix logic from PnrGenerator
        String sanitized = flightNumber.replaceAll("[^A-Z0-9]", "").toUpperCase();
        String prefix = sanitized.length() > 3 ? sanitized.substring(0, 3) : sanitized;

        String pnr = PnrGenerator.generatePnr(flightNumber, seatSignature);

        // prefix
        assertTrue(pnr.startsWith(prefix),
                () -> "PNR should start with prefix '" + prefix + "', but was: " + pnr);

        // length = prefix (<=3) + timePart(8) + random(4) + hash(3)
        int expectedLength = prefix.length() + 8 + 4 + 3;
        assertEquals(expectedLength, pnr.length(),
                () -> "PNR length should be " + expectedLength + " but was " + pnr.length());
    }

    @Test
    void pnrContainsOnlyAlphaNumericUppercase() {
        String flightNumber = "AI-102";
        String seatSignature = "12A-12B";

        String pnr = PnrGenerator.generatePnr(flightNumber, seatSignature);

        for (char c : pnr.toCharArray()) {
            assertTrue(
                    (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'),
                    () -> "PNR contains invalid character: " + c
            );
        }
    }

    @RepeatedTest(5)
    void multiplePnrsShouldUsuallyDiffer() {
        String flightNumber = "AI-102";
        String seatSignature = "12A-12B";

        String pnr1 = PnrGenerator.generatePnr(flightNumber, seatSignature);
        String pnr2 = PnrGenerator.generatePnr(flightNumber, seatSignature);

        // Not mathematically guaranteed, but practically always true
        assertNotEquals(pnr1, pnr2,
                () -> "Two generated PNRs for same input should usually differ, but got: " + pnr1);
    }

    @Test
    void differentSeatSignaturesProduceDifferentPnrs() {
        String flightNumber = "AI-102";

        String seatsA = "12A-12B-14C";
        String seatsB = "14C-15D-16E";

        String pnrA = PnrGenerator.generatePnr(flightNumber, seatsA);
        String pnrB = PnrGenerator.generatePnr(flightNumber, seatsB);

        assertNotEquals(pnrA, pnrB,
                () -> "Different seat signatures should result in different PNRs");
    }

    
}

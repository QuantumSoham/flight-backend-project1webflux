package com.flightapp.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class PnrGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";

    
    public static String generatePnr(String flightNumber, String seatSignature) {
        // BASE PNR = FLIGHT NUMBER 3 CHAR + MMDDHHMM + 4 random characters
        String prefix = flightNumber.replaceAll("[^A-Z0-9]", "").toUpperCase();//converting all a-z small char to upper case
        if (prefix.length() > 3) {
            prefix = prefix.substring(0, 3);
        }//if prefix length is more than 3 shorten it to 3 char

        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmm"));//get date and time sign

        StringBuilder sb = new StringBuilder(prefix).append(timePart);//string builder to append , as string builder memory efficient and mutable

        for (int i = 0; i < 4; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        } //add final 4 characters randomly to my string - this is similar to password salting

        String basePnr = sb.toString();//converting string builder object to string object

        //PNR HASH= hashSHA-256(BASE PNR) + seatSignature to get a short suffix
        String toHash = basePnr + ":" + (seatSignature == null ? "" : seatSignature);
        String hashSuffix = shortHash(toHash, 3); // 3-char hash tail

        // final PNR = base PNR + hash tail 
        return basePnr + hashSuffix;
    }

    // CALCULATING SHA-256 and turn first bits into ALPHABET chars
    private static String shortHash(String input, int length) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();
            int bits = 0;
            int value = 0;
            //LOGIC TAKEN FROM NET SOURCES 
            for (byte b : hash) {
                value = (value << 8) | (b & 0xFF);
                bits += 8;
                while (bits >= 5 && result.length() < length) {
                    int idx = (value >> (bits - 5)) & 0b1_1111; // 0–31
                    bits -= 5;
                    result.append(ALPHABET.charAt(idx % ALPHABET.length()));
                }
                if (result.length() == length) break;
            }

            //PADDING BITS
            while (result.length() < length) {
                result.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }

            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PNR hash", e);
        }
    }

    
}

//package com.flightapp.util;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.security.SecureRandom;
//
//@Component
//public class PnrGenerator {
//
//	private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
//	private final SecureRandom random = new SecureRandom();
//	private final int length;
//
//	public PnrGenerator(@Value("${app.pnr.length:8}") int length) {
//		this.length = length;
//	}
//
//	public String generate() {
//		StringBuilder sb = new StringBuilder(length);
//		for (int i = 0; i < length; i++) {
//			sb.append(ALPHANUM.charAt(random.nextInt(ALPHANUM.length())));
//		}
//		return sb.toString();
//	}
//}

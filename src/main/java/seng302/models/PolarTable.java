package seng302.models;

import java.io.*;
import java.util.HashMap;

/**
 * A static class for parsing and storing the polars. Will parse the whole polar table and also store the optimised
 * upwind and downwind in separate tables here as well
 * Created by wmu16 on 22/05/17.
 */
public final class PolarTable {

    //A Polar table will consist of a wind speed key to a hashmap value of pairs of wind angles and boat speeds
    private static HashMap<Double, HashMap<Double, Double>> polarTable;
    private static HashMap<Double, HashMap<Double, Double>> upwindOptimal;
    private static HashMap<Double, HashMap<Double, Double>> downwindOptimal;

    private static int upTwaIndex;
    private static int dnTwaIndex;


    /**
     * Iterates through each row of the polar table, in pairs, to extract the row into a hashmap of angle to boat speed.
     * These angle boatspeed hashmaps are then added to an outer hashmap at the end of wind speed key to each row hashmap
     * as a value
     * @param file containing the polar csv information
     */
    public static void parsePolarFile(String file) {
        polarTable = new HashMap<>();
        upwindOptimal = new HashMap<>();
        downwindOptimal = new HashMap<>();

        String line;
        Boolean isHeaderLine = true;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            while ((line = br.readLine()) != null) {
                String[] thisLine = line.split(",");

                //Initial line in file
                if (isHeaderLine) {
                    deduceHeaders(thisLine);
                    isHeaderLine = false;
                } else {
                    HashMap<Double, Double> thisPolar = new HashMap<>();
                    HashMap<Double, Double> thisUpWindPolar = new HashMap<>();
                    HashMap<Double, Double> thisDnWindPolar = new HashMap<>();
                    Double thisWindSpeed = Double.parseDouble(thisLine[0]);

                    // -3 <== -1 for length -1, and a further -2 as we iterate in pairs of 2 so finish before final 2
                    for (int i = 1; i < thisLine.length; i += 2) {
                        Double thisWindAngle = Double.parseDouble(thisLine[i]);
                        Double thisBoatSpeed = Double.parseDouble(thisLine[i + 1]);
                        thisPolar.put(thisWindAngle, thisBoatSpeed);
                        if (i == upTwaIndex) {
                            thisUpWindPolar.put(thisWindAngle, thisBoatSpeed);
                        } else if (i == dnTwaIndex) {
                            thisDnWindPolar.put(thisWindAngle, thisBoatSpeed);
                        }
                    }

                    polarTable.put(thisWindSpeed, thisPolar);
                    upwindOptimal.put(thisWindSpeed, thisUpWindPolar);
                    downwindOptimal.put(thisWindSpeed, thisDnWindPolar);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    /**
     * Parses the header line of a polar file
     * @param thisLine The line which is the header of a polar file
     */
    private static void deduceHeaders(String[] thisLine) {

        for (int i = 0; i < thisLine.length; i++) {
            String thisItem = thisLine[i];
            if (thisItem.toLowerCase().startsWith("uptwa")) {
                upTwaIndex = i;
            }
            else if (thisItem.toLowerCase().startsWith("dntwa")) {
                dnTwaIndex = i;
            }
        }
    }


    /**
     * @return The entire polar table
     */
    public static HashMap<Double, HashMap<Double, Double>> getPolarTable() {
        return polarTable;
    }


    /**
     * @return The polar table just containing the optimal upwind values
     */
    public static HashMap<Double, HashMap<Double, Double>> getUpwindOptimal() {
        return upwindOptimal;
    }


    /**
     * @return The polar table just containing the optimal downwind values
     */
    public static HashMap<Double, HashMap<Double, Double>> getDownwindOptimal() {
        return downwindOptimal;
    }

}
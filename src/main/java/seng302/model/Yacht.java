package seng302.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.scene.paint.Color;
import seng302.gameServer.GameState;
import seng302.model.mark.CompoundMark;
import seng302.model.mark.Mark;
import seng302.utilities.GeoUtility;

/**
 * Yacht class for the racing boat.
 *
 * Class created to store more variables (eg. boat statuses) compared to the XMLParser boat class,
 * also done outside Boat class because some old variables are not used anymore.
 */
public class Yacht {

    @FunctionalInterface
    public interface YachtLocationListener {
        void notifyLocation(Yacht yacht, double lat, double lon, double heading, double velocity);
    }

    private static final Double ROUNDING_DISTANCE = 50d; // TODO: 3/08/17 wmu16 - Look into this value further

    //BOTH AFAIK
    private String boatType;
    private Integer sourceId;
    private String hullID; //matches HullNum in the XML spec.
    private String shortName;
    private String boatName;
    private String country;

    private Long estimateTimeAtFinish;
    private Integer currentMarkSeqID = 1;
    private Long markRoundTime;
    private Double distanceToNextMark;
    private Long timeTillNext;
    private Double heading;
    private Integer legNumber = 0;

    //SERVER SIDE
    private final Double TURN_STEP = 5.0;
    private Double lastHeading;
    private Boolean sailIn;
    private GeoPoint location;
    private Integer boatStatus;
    private Double velocity;
    //MARK ROUNDING INFO
    private GeoPoint lastLocation;  //For purposes of mark rounding calculations
    private Boolean hasEnteredRoundingZone; //The distance that the boat must be from the mark to round
    private Boolean hasPassedFirstLine; //The line extrapolated from the next mark to the current mark

    //CLIENT SIDE
    private List<YachtLocationListener> locationListeners = new ArrayList<>();
    private ReadOnlyDoubleWrapper velocityProperty = new ReadOnlyDoubleWrapper();
    private ReadOnlyLongWrapper timeTillNextProperty = new ReadOnlyLongWrapper();
    private ReadOnlyLongWrapper timeSinceLastMarkProperty = new ReadOnlyLongWrapper();
    private CompoundMark lastMarkRounded;
    private Integer positionInt = 0;
    private Color colour;

    public Yacht(String boatType, Integer sourceId, String hullID, String shortName,
            String boatName, String country) {
        this.boatType = boatType;
        this.sourceId = sourceId;
        this.hullID = hullID;
        this.shortName = shortName;
        this.boatName = boatName;
        this.country = country;
        this.sailIn = false;
        this.location = new GeoPoint(57.670341, 11.826856);
        this.lastLocation = location;
        this.heading = 120.0;   //In degrees
        this.velocity = 0d;     //in mms-1

        this.hasEnteredRoundingZone = false;
        this.hasPassedFirstLine = false;
    }

    /**
     * @param timeInterval since last update in milliseconds
     */
    public void update(Long timeInterval) {

        Double secondsElapsed = timeInterval / 1000000.0;
        Double windSpeedKnots = GameState.getWindSpeedKnots();
        Double trueWindAngle = Math.abs(GameState.getWindDirection() - heading);
        Double boatSpeedInKnots = PolarTable.getBoatSpeed(windSpeedKnots, trueWindAngle);
        Double maxBoatSpeed = boatSpeedInKnots / 1.943844492 * 1000;
        if (sailIn && velocity <= maxBoatSpeed && maxBoatSpeed != 0d) {

            if (velocity < maxBoatSpeed) {
                velocity += maxBoatSpeed / 15;  // Acceleration
            }
            if (velocity > maxBoatSpeed) {
                velocity = maxBoatSpeed;        // Prevent the boats from exceeding top speed
            }

        } else { // Deceleration

            if (velocity > 0d) {
                if (maxBoatSpeed != 0d) {
                    velocity -= maxBoatSpeed / 600;
                } else {
                    velocity -= velocity / 100;
                }
                if (velocity < 0) {
                    velocity = 0d;
                }
            }
        }

        //UPDATE BOAT LOCATION
        location = GeoUtility.getGeoCoordinate(location, heading, velocity * secondsElapsed);

        //CHECK FOR MARK ROUNDING
        //Algorithm wont currently work on last gate and start gate
        checkForMarkRounding();

        // TODO: 3/08/17 wmu16 - Implement line cross check here
    }


    /**
     * Calculates the distance to the next mark (closest of the two if a gate mark). For purposes
     * of mark rounding
     * @return A distance in metres. Returns -1 if there is no next mark
     * @throws IndexOutOfBoundsException If the next mark is null (ie the last mark in the race)
     *         Check first using {@link seng302.model.mark.MarkOrder#isLastMark(Integer)}
     */
    public Double calcDistanceToNextMark() throws IndexOutOfBoundsException {
        CompoundMark nextMark = GameState.getMarkOrder().getCurrentMark(currentMarkSeqID);

        if (nextMark.isGate()) {
            Mark sub1 = nextMark.getSubMark(1);
            Mark sub2 = nextMark.getSubMark(2);
            Double distance1 = GeoUtility.getDistance(location, sub1);
            Double distance2 = GeoUtility.getDistance(location, sub2);
            return (distance1 < distance2) ? distance1 : distance2;
        } else {
            return GeoUtility.getDistance(location, nextMark.getSubMark(1));
        }
    }


    /**
     * This algorithm checks for mark rounding. And increments the currentMarSeqID number attribute
     * of the yacht if so.
     * The algorithm works by using the last mark, the current mark, the next mark and the change in
     * boats location, like so:
     * -Condition 1:
     *  The boat has entered the mark rounding distance
     * -Condition 2:
     *  The boat has passed the line extending from the last mark to the current mark
     * -Condition 3:
     *  The boat has passed the line extending from the next mark to the current mark
     *
     * A more visual representation of this algorithm can be seen on the Wiki under
     * 'mark passing algorithm'
     */
    private void checkForMarkRounding() {
        if (!GameState.getMarkOrder().isLastMark(currentMarkSeqID) && currentMarkSeqID != 0) {
            distanceToNextMark = calcDistanceToNextMark();
//            System.out.println("distanceToNextMark = " + distanceToNextMark);
            CompoundMark nextMark = GameState.getMarkOrder().getNextMark(currentMarkSeqID);
            CompoundMark currentMark = GameState.getMarkOrder().getCurrentMark(currentMarkSeqID);
            CompoundMark prevMark = GameState.getMarkOrder().getPreviousMark(currentMarkSeqID);

            //1 TEST FOR ENTERING THE ROUDNING DISTANCE
            if (distanceToNextMark < ROUNDING_DISTANCE) {
                hasEnteredRoundingZone = true;
//                System.out.println("Entered rounding zone!");
            }

            //If the current mark is a gate mark we need to check both its marks for rounding, thus
            //we loop
            for (Mark thisCurrentMark : currentMark.getMarks()) {

                //2 TEST FOR CROSSING NEXT - CURRENT LINE FIRST
                if (GeoUtility.isPointInTriangle(lastLocation, location, nextMark.getMarks().get(0),
                    thisCurrentMark)) {
                    hasPassedFirstLine = true;
                    System.out.println("Passed first line!");
                }

                //3 TEST FOR CROSSING PREV - CURRENT LINE SECOND
                if (GeoUtility.isPointInTriangle(lastLocation, location, prevMark.getMarks().get(0),
                    thisCurrentMark)) {
                    if (hasPassedFirstLine && hasEnteredRoundingZone) {
                        currentMarkSeqID++;
                        hasPassedFirstLine = false;
                        hasEnteredRoundingZone = false;
                        System.out.println("SUCCESFUL ROUDNING!");
                        break;
                    }
                }
            }
        }
    }

    public void adjustHeading(Double amount) {
        Double newVal = heading + amount;
        lastHeading = heading;
        heading = (double) Math.floorMod(newVal.longValue(), 360L);
    }

    public void tackGybe(Double windDirection) {
        Double normalizedHeading = normalizeHeading();
        adjustHeading(-2 * normalizedHeading);
    }

    public void toggleSailIn() {
        sailIn = !sailIn;
    }

    public void turnUpwind() {
        Double normalizedHeading = normalizeHeading();
        if (normalizedHeading == 0) {
            if (lastHeading < 180) {
                adjustHeading(-TURN_STEP);
            } else {
                adjustHeading(TURN_STEP);
            }
        } else if (normalizedHeading == 180) {
            if (lastHeading < 180) {
                adjustHeading(TURN_STEP);
            } else {
                adjustHeading(-TURN_STEP);
            }
        } else if (normalizedHeading < 180) {
            adjustHeading(-TURN_STEP);
        } else {
            adjustHeading(TURN_STEP);
        }
    }

    public void turnDownwind() {
        Double normalizedHeading = normalizeHeading();
        if (normalizedHeading == 0) {
            if (lastHeading < 180) {
                adjustHeading(TURN_STEP);
            } else {
                adjustHeading(-TURN_STEP);
            }
        } else if (normalizedHeading == 180) {
            if (lastHeading < 180) {
                adjustHeading(-TURN_STEP);
            } else {
                adjustHeading(TURN_STEP);
            }
        } else if (normalizedHeading < 180) {
            adjustHeading(TURN_STEP);
        } else {
            adjustHeading(-TURN_STEP);
        }
    }

    public void turnToVMG() {
        Double normalizedHeading = normalizeHeading();
        Double optimalHeading;
        HashMap<Double, Double> optimalPolarMap;

        if (normalizedHeading >= 90 && normalizedHeading <= 270) { // Downwind
            optimalPolarMap = PolarTable.getOptimalDownwindVMG(GameState.getWindSpeedKnots());
            optimalHeading = optimalPolarMap.keySet().iterator().next();
        } else {
            optimalPolarMap = PolarTable.getOptimalUpwindVMG(GameState.getWindSpeedKnots());
            optimalHeading = optimalPolarMap.keySet().iterator().next();
        }
        // Take optimal heading and turn into correct
        optimalHeading =
            optimalHeading + (double) Math.floorMod(GameState.getWindDirection().longValue(), 360L);

        turnTowardsHeading(optimalHeading);

    }

    private void turnTowardsHeading(Double newHeading) {
        System.out.println(newHeading);
        if (heading < 90 && newHeading > 270) {
            adjustHeading(-TURN_STEP);
        } else {
            if (heading < newHeading) {
                adjustHeading(TURN_STEP);
            } else {
                adjustHeading(-TURN_STEP);
            }
        }
    }

    private Double normalizeHeading() {
        Double normalizedHeading = heading - GameState.windDirection;
        normalizedHeading = (double) Math.floorMod(normalizedHeading.longValue(), 360L);
        return normalizedHeading;
    }

    public String getBoatType() {
        return boatType;
    }

    public Integer getSourceId() {
        //@TODO Remove and merge with Creating Game Loop
        if (sourceId == null) return 0;
        return sourceId;
    }

    public String getHullID() {
        if (hullID == null) return "";
        return hullID;
    }

    public String getShortName() {
        return shortName;
    }

    public String getBoatName() {
        return boatName;
    }

    public String getCountry() {
        if (country == null) return "";
        return country;
    }

    public Integer getBoatStatus() {
        return boatStatus;
    }

    public void setBoatStatus(Integer boatStatus) {
        this.boatStatus = boatStatus;
    }

    public Integer getLegNumber() {
        return legNumber;
    }

    public void setLegNumber(Integer legNumber) {
//        if (colour != null  && position != "-" && legNumber != this.legNumber) {
//            RaceViewController.updateYachtPositionSparkline(this, legNumber);
//        }
        this.legNumber = legNumber;
    }

    public void setEstimateTimeTillNextMark(Long estimateTimeTillNextMark) {
        timeTillNext = estimateTimeTillNextMark;
    }

    public String getEstimateTimeAtFinish() {
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return format.format(estimateTimeAtFinish);
    }

    public void setEstimateTimeAtFinish(Long estimateTimeAtFinish) {
        this.estimateTimeAtFinish = estimateTimeAtFinish;
    }

    public Integer getPositionInteger() {
        return positionInt;
    }

    public void setPositionInteger(Integer position) {
        this.positionInt = position;
    }

    public void updateVelocityProperty(double velocity) {
        this.velocityProperty.set(velocity);
    }

    public void setMarkRoundingTime(Long markRoundingTime) {
        this.markRoundTime = markRoundingTime;
    }

    public ReadOnlyDoubleProperty getVelocityProperty() {
        return velocityProperty.getReadOnlyProperty();
    }

    public double getVelocityMMS() {
        return velocity;
    }

    public ReadOnlyLongProperty timeTillNextProperty() {
        return timeTillNextProperty.getReadOnlyProperty();
    }

    public Double getVelocityKnots() {
        return velocity / 1000 * 1.943844492; // TODO: 26/07/17 cir27 - remove magic number
    }

    public Long getTimeTillNext() {
        return timeTillNext;
    }

    public Long getMarkRoundTime() {
        return markRoundTime;
    }

    public CompoundMark getLastMarkRounded() {
        return lastMarkRounded;
    }

    public void setLastMarkRounded(CompoundMark lastMarkRounded) {
        this.lastMarkRounded = lastMarkRounded;
    }

    public GeoPoint getLocation() {
        return location;
    }

    /**
     * Sets the current location of the boat in lat and long whilst preserving the last location
     *
     * @param lat Latitude
     * @param lng Longitude
     */
    public void setLocation(Double lat, Double lng) {
        lastLocation.setLat(location.getLat());
        lastLocation.setLng(location.getLng());
        location.setLat(lat);
        location.setLng(lng);
    }

    public Double getHeading() {
        return heading;
    }

    public void setHeading(Double heading) {
        this.heading = heading;
    }

    public Boolean getSailIn() {
        return sailIn;
    }

    @Override
    public String toString() {
        return boatName;
    }

    public void updateTimeSinceLastMarkProperty(long timeSinceLastMark) {
        this.timeSinceLastMarkProperty.set(timeSinceLastMark);
    }

    public ReadOnlyLongProperty timeSinceLastMarkProperty () {
        return timeSinceLastMarkProperty.getReadOnlyProperty();
    }

    public void setTimeTillNext(Long timeTillNext) {
        this.timeTillNext = timeTillNext;
    }


    public Color getColour() {
        return colour;
    }

    public void setColour(Color colour) {
        this.colour = colour;
    }


    public Double getVelocity() {
        return velocity;
    }

    public void setVelocity(Double velocity) {
        this.velocity = velocity;
    }

    public Double getDistanceToNextMark() {
        return distanceToNextMark;
    }

    public void updateLocation(double lat, double lng, double heading, double velocity) {
        setLocation(lat, lng);
        this.heading = heading;
        this.velocity = velocity;
        updateVelocityProperty(velocity);
        for (YachtLocationListener yll : locationListeners) {
            yll.notifyLocation(this, lat, lng, heading, velocity);
        }
    }

    public void addLocationListener (YachtLocationListener listener) {
        locationListeners.add(listener);
    }
}

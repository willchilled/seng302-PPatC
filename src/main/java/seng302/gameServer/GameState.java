package seng302.gameServer;

import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import seng302.gameServer.messages.*;
import seng302.model.*;
import seng302.model.mark.CompoundMark;
import seng302.model.mark.Mark;
import seng302.model.mark.MarkOrder;
import seng302.model.token.Token;
import seng302.model.token.TokenType;
import seng302.utilities.GeoUtility;
import seng302.utilities.XMLParser;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.*;

/**
 * A Static class to hold information about the current state of the game (model)
 * Also contains logic for updating itself on regular time intervals on its own thread
 * Created by wmu16 on 10/07/17.
 */
public class GameState implements Runnable {

    @FunctionalInterface
    interface NewMessageListener {
        void notify(Message message);
    }

    private static Logger logger = LoggerFactory.getLogger(GameState.class);


    static final int WARNING_TIME = 10 * -1000;
    static final int PREPATORY_TIME = 5 * -1000;
    private static final int TIME_TILL_START = 10 * 1000;

    private static final Long POWERUP_TIMEOUT_MS = 10_000L;

    private static final Integer STATE_UPDATES_PER_SECOND = 60;
    private static Double ROUNDING_DISTANCE = 50d; // TODO: 14/08/17 wmu16 - Look into this value further
    private static final Double MARK_COLLISION_DISTANCE = 15d;
    public static final Double YACHT_COLLISION_DISTANCE = 25.0;
    private static final Double BOUNCE_DISTANCE_MARK = 20.0;
    public static final Double BOUNCE_DISTANCE_YACHT = 30.0;
    private static final Double COLLISION_VELOCITY_PENALTY = 0.3;

    private static Long previousUpdateTime;
    public static Double windDirection;
    private static Double windSpeed;
    private static Double speedMultiplier = 1d;

    private static Boolean customizationFlag; // dirty flag to tell if a player has customized their boat.
    private static Boolean playerHasLeftFlag;

    private static String hostIpAddress;
    private static List<Player> players;
    private static Map<Integer, ServerYacht> yachts;
    private static Boolean isRaceStarted;
    private static GameStages currentStage;
    private static MarkOrder markOrder;
    private static long startTime;
    private static Set<Mark> marks;
    private static List<Limit> courseLimit;
    private static Integer maxPlayers = 8;


    private static List<Token> allTokens;
    private static List<Token> tokensInPlay;

    private static List<NewMessageListener> newMessageListeners;

    private static Map<Player, String> playerStringMap = new HashMap<>();

    public GameState(String hostIpAddress) {
        windDirection = 180d;
        windSpeed = 10000d;
        yachts = new HashMap<>();
        tokensInPlay = new ArrayList<>();

        players = new ArrayList<>();
        GameState.hostIpAddress = hostIpAddress;
        customizationFlag = false;
        playerHasLeftFlag = false;
        speedMultiplier = 1.0;
        currentStage = GameStages.LOBBYING;
        isRaceStarted = false;
        //set this when game stage changes to prerace
        previousUpdateTime = System.currentTimeMillis();
        markOrder = new MarkOrder(); //This could be instantiated at some point with a select map?
        newMessageListeners = new ArrayList<>();
        allTokens = makeTokens();

        resetStartTime();

        new Thread(this, "GameState").start();   //Run the auto updates on the game state

        marks = new MarkOrder().getAllMarks();
        setCourseLimit("/server_config/race.xml");
    }

    private void setCourseLimit(String url) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder;
        Document document = null;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            document = documentBuilder.parse(new InputSource(getClass().getResourceAsStream(url)));
        } catch (Exception e) {
            // sorry, we have to catch general one, otherwise we have to catch five different exceptions.
            logger.trace("Failed to load course limit for boundary collision detection.", e);
        }
        courseLimit = XMLParser.parseRace(document).getCourseLimit();
    }


    /**
     * Make a pre defined set of tokensInPlay. //TODO wmu16 - Should read from some file for each
     * race ideally
     *
     * @return A list of possible tokensInPlay for this race
     */
    private ArrayList<Token> makeTokens() {
        Token token1 = new Token(TokenType.BOOST, 57.66946, 11.83154);
        Token token2 = new Token(TokenType.BOOST, 57.66877, 11.83382);
        Token token3 = new Token(TokenType.BOOST, 57.66914, 11.83965);
        Token token4 = new Token(TokenType.BOOST, 57.66684, 11.83214);
        return new ArrayList<>(Arrays.asList(token1, token2, token3, token4));
    }

    public static String getHostIpAddress() {
        return hostIpAddress;
    }

    public static Set<Mark> getMarks() {
        return Collections.unmodifiableSet(marks);
    }

    public static List<Player> getPlayers() {
        return players;
    }

    public static List<Token> getTokensInPlay() {
        return tokensInPlay;
    }

    public static void addPlayer(Player player) {
        players.add(player);
        String playerText = player.getYacht().getSourceId() + " " + player.getYacht().getBoatName()
            + " " + player.getYacht().getCountry();
        playerStringMap.put(player, playerText);
    }

    public static void removePlayer(Player player) {
        players.remove(player);
        playerStringMap.remove(player);
    }

    public static void addYacht(Integer sourceId, ServerYacht yacht) {
        yachts.put(sourceId, yacht);
    }

    public static void removeYacht(Integer yachtId) {
        yachts.remove(yachtId);
    }

    public static Boolean getIsRaceStarted() {
        return isRaceStarted;
    }

    public static GameStages getCurrentStage() {
        return currentStage;
    }

    public static void setCurrentStage(GameStages currentStage) {
        GameState.currentStage = currentStage;
    }

    public static MarkOrder getMarkOrder() {
        return markOrder;
    }

    public static long getStartTime() {
        return startTime;
    }

    public static void resetStartTime(){
        startTime = System.currentTimeMillis() + TIME_TILL_START;
    }

    public static Double getWindDirection() {
        return windDirection;
    }

    public static void setWindDirection(Double newWindDirection) {
        windDirection = newWindDirection;
    }

    public static void setWindSpeed(Double newWindSpeed) {
        windSpeed = newWindSpeed;
    }

    public static Double getWindSpeedMMS() {
        return windSpeed;
    }

    public static Double getWindSpeedKnots() {
        return GeoUtility.mmsToKnots(windSpeed); // TODO: 26/07/17 cir27 - remove magic numbers
    }

    public static Map<Integer, ServerYacht> getYachts() {
        return yachts;
    }


    /**
     * Generates a new ID based off the size of current players + 1
     *
     * @return a playerID to be allocated to a new connetion
     */
    public static Integer getUniquePlayerID() {
        // TODO: 22/07/17 wmu16 - This may not be robust enough and may have to be improved on.
        return yachts.size() + 1;
    }


    /**
     * A thread to have the game state update itself at certain intervals
     */
    @Override
    public void run() {

        while (currentStage != GameStages.FINISHED) {
            try {
                Thread.sleep(1000 / STATE_UPDATES_PER_SECOND);
            } catch (InterruptedException e) {
                System.out.println("[GameState] interrupted exception");
            }
            if (currentStage == GameStages.PRE_RACE || currentStage == GameStages.RACING) {
                update();
            }

            if (currentStage == GameStages.RACING) {
                update();
            }
        }
    }

    public static void updateBoat(Integer sourceId, BoatAction actionType) {
        ServerYacht playerYacht = yachts.get(sourceId);
        switch (actionType) {
            case VMG:
                playerYacht.turnToVMG();
                break;
            case SAILS_IN:
                playerYacht.toggleSailIn();
                break;
            case SAILS_OUT:
                playerYacht.toggleSailIn();
                break;
            case TACK_GYBE:
                playerYacht.tackGybe(windDirection);
                break;
            case UPWIND:
                playerYacht.turnUpwind();
                break;
            case DOWNWIND:
                playerYacht.turnDownwind();
                break;
        }
    }

    /**
     * Randomly select a subset of tokensInPlay from a pre defined superset
     * Broadasts a new race status message to show this update
     */
    public static void spawnNewToken() {
        Random random = new Random();
        tokensInPlay.clear();
        tokensInPlay.add(allTokens.get(random.nextInt(allTokens.size())));
    }

    /**
     * Called periodically in this GameState thread to update the GameState values.
     * -Updates yachts velocity
     * -Updates locations
     * -Checks for collisions
     * -Checks for progression
     *
     * -Also checks things like the end of the race and race start time etc
     */
    public void update() {
        Boolean raceFinished = true;

        Double timeInterval = (System.currentTimeMillis() - previousUpdateTime) / 1000000.0;
        previousUpdateTime = System.currentTimeMillis();
        if (System.currentTimeMillis() > startTime) {
            GameState.setCurrentStage(GameStages.RACING);
        }
        for (ServerYacht yacht : yachts.values()) {
            updateVelocity(yacht);
            checkPowerUpTimeout(yacht);
            yacht.runAutoPilot();
            yacht.updateLocation(timeInterval);
            checkCollision(yacht);
            if (yacht.getBoatStatus() != BoatStatus.FINISHED) {
                checkForLegProgression(yacht);
                raceFinished = false;
            }
        }

        if (raceFinished) {
            currentStage = GameStages.FINISHED;
        }
    }


    private void checkPowerUpTimeout(ServerYacht yacht) {
        if (yacht.getPowerUp() != null) {
            if (System.currentTimeMillis() - yacht.getPowerUpStartTime() > POWERUP_TIMEOUT_MS) {
                yacht.powerDown();
                sendServerMessage(yacht.getSourceId(), yacht.getBoatName() + "'s power-up token expired");
                logger.debug("Yacht: " + yacht.getShortName() + " powered down!");
            }
        }
    }


    /**
     * Check if the yacht has crossed the course limit
     *
     * @param yacht the yacht to be tested
     * @return a boolean value of if there is a boundary collision
     */
    private static Boolean checkBoundaryCollision(ServerYacht yacht) {
        for (int i = 0; i < courseLimit.size() - 1; i++) {
            if (GeoUtility.checkCrossedLine(courseLimit.get(i), courseLimit.get(i + 1),
                yacht.getLastLocation(), yacht.getLocation()) != 0) {
                return true;
            }
        }
        if (GeoUtility.checkCrossedLine(courseLimit.get(courseLimit.size() - 1), courseLimit.get(0),
            yacht.getLastLocation(), yacht.getLocation()) != 0) {
            return true;
        }
        return false;
    }

    /**
     * Checks all tokensInPlay to see if a yacht has picked one up
     * @return Token which was collided with
     * @param serverYacht The yacht to check for collision with a token
     */
    private static Token checkTokenPickUp(ServerYacht serverYacht) {
        for (Token token : tokensInPlay) {
            Double distance = GeoUtility.getDistance(token, serverYacht.getLocation());
            if (distance < YACHT_COLLISION_DISTANCE) {
                return token;
            }
        }

        return null;
    }


    /**
     * Checks for collision with other in game objects for the given serverYacht. To be called each
     * update. If there is a collision, Notifies the server to send the appropriate messages out.
     * Checks for these items in turn:
     * - Other yachts
     * - Marks
     * - Boundary
     * - Tokens
     *
     * @param serverYacht The server yacht to check collisions with
     */
    public static void checkCollision(ServerYacht serverYacht) {
        //Yacht Collision
        ServerYacht collidedYacht = checkYachtCollision(serverYacht);
        Mark collidedMark = checkMarkCollision(serverYacht);

        if (collidedYacht != null) {
            GeoPoint originalLocation = serverYacht.getLocation();
            serverYacht.setLocation(
                calculateBounceBack(serverYacht, originalLocation, BOUNCE_DISTANCE_YACHT)
            );
            serverYacht.setCurrentVelocity(
                serverYacht.getCurrentVelocity() * COLLISION_VELOCITY_PENALTY
            );
            collidedYacht.setLocation(
                calculateBounceBack(collidedYacht, originalLocation, BOUNCE_DISTANCE_YACHT)
            );
            collidedYacht.setCurrentVelocity(
                collidedYacht.getCurrentVelocity() * COLLISION_VELOCITY_PENALTY
            );
            notifyMessageListeners(
                new YachtEventCodeMessage(serverYacht.getSourceId(), YachtEventType.COLLISION)
            );
        }

        //Mark Collision
        else if (collidedMark != null) {
            serverYacht.setLocation(
                calculateBounceBack(serverYacht, collidedMark, BOUNCE_DISTANCE_MARK)
            );

            serverYacht.setCurrentVelocity(
                serverYacht.getCurrentVelocity() * COLLISION_VELOCITY_PENALTY
            );
            notifyMessageListeners(
                new YachtEventCodeMessage(serverYacht.getSourceId(), YachtEventType.COLLISION)
            );
        }

        //Boundary Collision
        else if (checkBoundaryCollision(serverYacht)) {
            serverYacht.setLocation(
                calculateBounceBack(serverYacht, serverYacht.getLocation(),
                    BOUNCE_DISTANCE_YACHT)
            );

            serverYacht.setCurrentVelocity(
                serverYacht.getCurrentVelocity() * COLLISION_VELOCITY_PENALTY
            );
            notifyMessageListeners(
                new YachtEventCodeMessage(serverYacht.getSourceId(), YachtEventType.COLLISION)
            );
        }

        //Token Collision
        Token collidedToken = checkTokenPickUp(serverYacht);
        if (collidedToken != null) {
            sendServerMessage(serverYacht.getSourceId(), serverYacht.getBoatName() + " has picked speed-up token");
            tokensInPlay.remove(collidedToken);
            serverYacht.powerUp(collidedToken.getTokenType());
            logger.debug("Yacht: " + serverYacht.getShortName() + " got powerup " + collidedToken
                .getTokenType());
            notifyMessageListeners(MessageFactory.getRaceXML());
            notifyMessageListeners(
                new YachtEventCodeMessage(serverYacht.getSourceId(), YachtEventType.TOKEN));
        }
    }


    private void updateVelocity(ServerYacht yacht) {
        Double trueWindAngle = Math.abs(windDirection - yacht.getHeading());
        Double boatSpeedInKnots = PolarTable.getBoatSpeed(getWindSpeedKnots(), trueWindAngle);
        Double maxBoatSpeed = GeoUtility.knotsToMMS(boatSpeedInKnots) * speedMultiplier * yacht.getMaxSpeedMultiplier();
        if (yacht.getPowerUp() != null) {
            if (yacht.getPowerUp().equals(TokenType.BOOST)) {
                // TODO: 11/09/17 wmu16 CHANGE THIS TO MAGIC NUMBER
                maxBoatSpeed *= 2;
            }
        }

        Double currentVelocity = yacht.getCurrentVelocity();
        // TODO: 15/08/17 remove magic numbers from these equations.
        if (yacht.getSailIn()) {
            if (currentVelocity < maxBoatSpeed - 500) {
                yacht.changeVelocity(maxBoatSpeed / 100);
            } else if (currentVelocity > maxBoatSpeed + 500) {
                yacht.changeVelocity(-currentVelocity / 200);
            } else {
                yacht.setCurrentVelocity(maxBoatSpeed);
            }
        } else {
            if (currentVelocity > 3000) {
                yacht.changeVelocity(-currentVelocity / 200);
            } else if (currentVelocity > 100) {
                yacht.changeVelocity(-currentVelocity / 50);
            } else if (currentVelocity <= 100) {
                yacht.setCurrentVelocity(0d);
            }
        }
    }


    /**
     * Calculates the distance to the next mark (closest of the two if a gate mark). For purposes of
     * mark rounding
     *
     * @return A distance in metres. Returns -1 if there is no next mark
     * @throws IndexOutOfBoundsException If the next mark is null (ie the last mark in the race)
     * Check first using {@link seng302.model.mark.MarkOrder#isLastMark(Integer)}
     */
    private Double calcDistanceToCurrentMark(ServerYacht yacht) throws IndexOutOfBoundsException {
        Integer currentMarkSeqID = yacht.getCurrentMarkSeqID();
        CompoundMark currentMark = markOrder.getCurrentMark(currentMarkSeqID);
        GeoPoint location = yacht.getLocation();

        if (currentMark.isGate()) {
            Mark sub1 = currentMark.getSubMark(1);
            Mark sub2 = currentMark.getSubMark(2);
            Double distance1 = GeoUtility.getDistance(location, sub1);
            Double distance2 = GeoUtility.getDistance(location, sub2);
            if (distance1 < distance2) {
                yacht.setClosestCurrentMark(sub1);
                return distance1;
            } else {
                yacht.setClosestCurrentMark(sub2);
                return distance2;
            }
        } else {
            yacht.setClosestCurrentMark(currentMark.getSubMark(1));
            return GeoUtility.getDistance(location, currentMark.getSubMark(1));
        }
    }


    /**        lobbyController.setPlayerListSource(clientLobbyList);
     * 4 Different cases of progression in the race 1 - Passing the start line 2 - Passing any
     * in-race Gate 3 - Passing any in-race Mark 4 - Passing the finish line
     *
     * @param yacht the current yacht to check for progression
     */
    private void checkForLegProgression(ServerYacht yacht) {
        Integer currentMarkSeqID = yacht.getCurrentMarkSeqID();
        CompoundMark currentMark = markOrder.getCurrentMark(currentMarkSeqID);

        Boolean hasProgressed;
        if (currentMarkSeqID == 0) {
            hasProgressed = checkStartLineCrossing(yacht);
        } else if (markOrder.isLastMark(currentMarkSeqID)) {
            hasProgressed = checkFinishLineCrossing(yacht);
        } else if (currentMark.isGate()) {
            hasProgressed = checkGateRounding(yacht);
        } else {
            hasProgressed = checkMarkRounding(yacht);
        }

        if (hasProgressed) {
            if (currentMarkSeqID != 0 && !markOrder.isLastMark(currentMarkSeqID)) {
                sendServerMessage(yacht.getSourceId(), yacht.getBoatName() + " passed leg " + yacht.getLegNumber());
            }
            yacht.incrementLegNumber();
            sendMarkRoundingMessage(yacht);
            logMarkRounding(yacht);
            yacht.setHasPassedLine(false);
            yacht.setHasEnteredRoundingZone(false);
            yacht.setHasPassedThroughGate(false);
            if (!markOrder.isLastMark(currentMarkSeqID)) {
                yacht.incrementMarkSeqID();
            }
        }
    }


    /**
     * If we pass the start line gate in the correct direction, progress
     *
     * @param yacht The current yacht to check for
     */
    private Boolean checkStartLineCrossing(ServerYacht yacht) {
        Integer currentMarkSeqID = yacht.getCurrentMarkSeqID();
        CompoundMark currentMark = markOrder.getCurrentMark(currentMarkSeqID);
        GeoPoint lastLocation = yacht.getLastLocation();
        GeoPoint location = yacht.getLocation();

        Mark mark1 = currentMark.getSubMark(1);
        Mark mark2 = currentMark.getSubMark(2);
        CompoundMark nextMark = markOrder.getNextMark(currentMarkSeqID);

        Integer crossedLine = GeoUtility.checkCrossedLine(mark1, mark2, lastLocation, location);
        if (crossedLine > 0) {
            Boolean isClockwiseCross = GeoUtility.isClockwise(mark1, mark2, nextMark.getMidPoint());
            if (crossedLine == 2 && isClockwiseCross || crossedLine == 1 && !isClockwiseCross) {
                yacht.setClosestCurrentMark(mark1);
                yacht.setBoatStatus(BoatStatus.RACING);
                sendServerMessage(yacht.getSourceId(), yacht.getBoatName() + " passed start line");
                return true;
            }
        }

        return false;
    }


    /**
     * This algorithm checks for mark rounding. And increments the currentMarSeqID number attribute
     * of the yacht if so. A visual representation of this algorithm can be seen on the Wiki under
     * 'mark passing algorithm'
     *
     * @param yacht The current yacht to check for
     */
    private Boolean checkMarkRounding(ServerYacht yacht) {
        Integer currentMarkSeqID = yacht.getCurrentMarkSeqID();
        CompoundMark currentMark = markOrder.getCurrentMark(currentMarkSeqID);
        GeoPoint lastLocation = yacht.getLastLocation();
        GeoPoint location = yacht.getLocation();
        GeoPoint nextPoint = markOrder.getNextMark(currentMarkSeqID).getMidPoint();
        GeoPoint prevPoint = markOrder.getPreviousMark(currentMarkSeqID).getMidPoint();
        GeoPoint midPoint = GeoUtility.getDirtyMidPoint(nextPoint, prevPoint);

        if (calcDistanceToCurrentMark(yacht) < ROUNDING_DISTANCE) {
            yacht.setHasEnteredRoundingZone(true);
        }

        //In case current mark is a gate, loop through all marks just in case
        for (Mark thisCurrentMark : currentMark.getMarks()) {
            if (GeoUtility.isPointInTriangle(lastLocation, location, midPoint, thisCurrentMark)) {
                yacht.setHasPassedLine(true);
            }
        }

        return yacht.hasPassedLine() && yacht.hasEnteredRoundingZone();
    }


    /**
     * Checks if a gate line has been crossed and in the correct direction
     *
     * @param yacht The current yacht to check for
     */
    private Boolean checkGateRounding(ServerYacht yacht) {
        Integer currentMarkSeqID = yacht.getCurrentMarkSeqID();
        CompoundMark currentMark = markOrder.getCurrentMark(currentMarkSeqID);
        GeoPoint lastLocation = yacht.getLastLocation();
        GeoPoint location = yacht.getLocation();

        Mark mark1 = currentMark.getSubMark(1);
        Mark mark2 = currentMark.getSubMark(2);
        CompoundMark prevMark = markOrder.getPreviousMark(currentMarkSeqID);
        CompoundMark nextMark = markOrder.getNextMark(currentMarkSeqID);

        Integer crossedLine = GeoUtility.checkCrossedLine(mark1, mark2, lastLocation, location);

        //We have crossed the line
        if (crossedLine > 0) {
            Boolean isClockwiseCross = GeoUtility.isClockwise(mark1, mark2, prevMark.getMidPoint());

            //Check we cross the line in the correct direction
            if (crossedLine == 1 && isClockwiseCross || crossedLine == 2 && !isClockwiseCross) {
                yacht.setHasPassedThroughGate(true);
            }
        }

        Boolean prevMarkSide = GeoUtility.isClockwise(mark1, mark2, prevMark.getMidPoint());
        Boolean nextMarkSide = GeoUtility.isClockwise(mark1, mark2, nextMark.getMidPoint());

        if (yacht.hasPassedThroughGate()) {
            //Check if we need to round this gate after passing through
            if (prevMarkSide == nextMarkSide) {
                return checkMarkRounding(yacht);
            } else {
                return true;
            }
        }

        return false;
    }

    /**
     * If we pass the finish gate in the correct direction
     *
     * @param yacht The current yacht to check for
     */
    private Boolean checkFinishLineCrossing(ServerYacht yacht) {
        Integer currentMarkSeqID = yacht.getCurrentMarkSeqID();
        CompoundMark currentMark = markOrder.getCurrentMark(currentMarkSeqID);
        GeoPoint lastLocation = yacht.getLastLocation();
        GeoPoint location = yacht.getLocation();

        Mark mark1 = currentMark.getSubMark(1);
        Mark mark2 = currentMark.getSubMark(2);
        CompoundMark prevMark = markOrder.getPreviousMark(currentMarkSeqID);

        Integer crossedLine = GeoUtility.checkCrossedLine(mark1, mark2, lastLocation, location);
        if (crossedLine > 0) {
            Boolean isClockwiseCross = GeoUtility.isClockwise(mark1, mark2, prevMark.getMidPoint());
            if (crossedLine == 1 && isClockwiseCross || crossedLine == 2 && !isClockwiseCross) {
                yacht.setClosestCurrentMark(mark1);
                yacht.setBoatStatus(BoatStatus.FINISHED);
                sendServerMessage(yacht.getSourceId(), yacht.getBoatName() + " passed finish line");
                return true;
            }
        }

        return false;
    }

    /**
     * Handles player customization.
     *
     * @param playerID The ID of the player being modified.
     * @param requestType the type of player customization the player wants
     * @param customizeData the data related to the customization (color, name, shape)
     */
    public static void customizePlayer(long playerID, CustomizeRequestType requestType,
        byte[] customizeData) {
        ServerYacht playerYacht = yachts.get((int) playerID);

        if (requestType.equals(CustomizeRequestType.NAME)) {
            String name = new String(customizeData);
            playerYacht.setBoatName(name);
        } else if (requestType.equals(CustomizeRequestType.COLOR)) {
            int red = customizeData[0] & 0xFF;
            int green = customizeData[1] & 0xFF;
            int blue = customizeData[2] & 0xFF;
            Color yachtColor = Color.rgb(red, green, blue);
            playerYacht.setBoatColor(yachtColor);
        } else if (requestType.equals(CustomizeRequestType.SHAPE)) {
            String type = new String(customizeData);
            playerYacht.setBoatType(type);
        }
    }

    private static Mark checkMarkCollision(ServerYacht yacht) {
        Set<Mark> marksInRace = GameState.getMarks();
        for (Mark mark : marksInRace) {
            if (GeoUtility.getDistance(yacht.getLocation(), mark)
                <= MARK_COLLISION_DISTANCE) {
                return mark;
            }
        }
        return null;
    }

    /**
     * Calculate the new position of the boat after it has had a collision
     *
     * @return The boats new position
     */
    private static GeoPoint calculateBounceBack(ServerYacht yacht, GeoPoint collidedWith,
        Double bounceDistance) {
        Double heading = GeoUtility.getBearing(yacht.getLastLocation(), collidedWith);
        // Invert heading
        heading -= 180;
        Integer newHeading = Math.floorMod(heading.intValue(), 360);
        return GeoUtility
            .getGeoCoordinate(yacht.getLocation(), newHeading.doubleValue(), bounceDistance);
    }

    /**
     * Collision detection which iterates through all the yachts and check if any yacht collided
     * with this yacht. Return collided yacht or null if no collision.
     *
     * @return yacht to compare to all other yachts.
     */
    private static ServerYacht checkYachtCollision(ServerYacht yacht) {

        for (ServerYacht otherYacht : GameState.getYachts().values()) {
            if (otherYacht != yacht) {
                Double distance = GeoUtility
                    .getDistance(otherYacht.getLocation(), yacht.getLocation());
                if (distance < YACHT_COLLISION_DISTANCE) {
                    return otherYacht;
                }
            }
        }
        return null;
    }

    private void sendMarkRoundingMessage(ServerYacht yacht) {
        Integer sourceID = yacht.getSourceId();
        Integer currentMarkSeqID = yacht.getCurrentMarkSeqID();
        CompoundMark currentMark = markOrder.getCurrentMark(currentMarkSeqID);
        MarkType markType = (currentMark.isGate()) ? MarkType.GATE : MarkType.ROUNDING_MARK;
        Mark roundingMark = yacht.getClosestCurrentMark();

        // TODO: 13/8/17 figure out the rounding side, rounded mark source ID and boat status.
        Message markRoundingMessage = new MarkRoundingMessage(0, 0,
            sourceID, RoundingBoatStatus.RACING, roundingMark.getRoundingSide(), markType,
            currentMarkSeqID + 1);

        notifyMessageListeners(markRoundingMessage);
    }

    private static void notifyMessageListeners(Message message) {
        for (NewMessageListener ml : newMessageListeners) {
            ml.notify(message);
        }
    }

    private void logMarkRounding(ServerYacht yacht) {
        Mark roundingMark = yacht.getClosestCurrentMark();
        logger.debug(
            String.format("Yacht srcID(%d) passed Mark srcID(%d)", yacht.getSourceId(),
                roundingMark.getSourceID()));
    }


    public static void sendServerMessage(Integer messageType, String message) {
        notifyMessageListeners(new ChatterMessage(
            messageType, "SERVER: " + message
        ));
    }

    public static void processChatter(ChatterMessage chatterMessage, boolean isHost) {
        String chatterText = chatterMessage.getMessage();
        String[] words = chatterText.split("\\s+");
        if (words.length > 2 && isHost) {
            switch (words[2].trim()) {
                case "/speed":
                    try {
                        setSpeedMultiplier(Double.valueOf(words[3]));
                        sendServerMessage(chatterMessage.getMessage_type(),
                                "Speed modifier set to x" + words[3]);
                    } catch (Exception e) {
                        Logger logger = LoggerFactory.getLogger(GameState.class);
                        logger.error("cannot parse >speed value");
                    }
                    return;
                case "/finish":
                    sendServerMessage(chatterMessage.getMessage_type(),
                            "Game will now finish");
                    endRace();
                    return;
            }
        }
        notifyMessageListeners(chatterMessage);
    }

    public static void addMessageEventListener(NewMessageListener listener) {
        newMessageListeners.add(listener);
    }

    public static void setCustomizationFlag() {
        customizationFlag = true;
    }

    public static Boolean getCustomizationFlag() {
        return customizationFlag;
    }

    public static void resetCustomizationFlag() {
        customizationFlag = false;
    }

    public static void setPlayerHasLeftFlag(Boolean flag) {
        playerHasLeftFlag = flag;
    }

    public static Boolean getPlayerHasLeftFlag() {
        return playerHasLeftFlag;
    }

    public static Integer getNumberOfPlayers(){
        Integer numPlayers = 1;

        for(Player p : getPlayers()){
            if(p.getSocket().isConnected()){
                numPlayers++;
            }
        }

        return numPlayers;
    }

    public static Integer getCapacity(){
        return maxPlayers;
    }

    public static void setMaxPlayers(Integer newMax){
        maxPlayers = newMax;
    }

    public static void endRace () {
        yachts.forEach((id, yacht) -> yacht.setBoatStatus(BoatStatus.FINISHED));
        currentStage = GameStages.FINISHED;
    }

    public static void setSpeedMultiplier (double multiplier) {
        speedMultiplier = multiplier;
    }

    public static double getSpeedMultiplier () {
        return speedMultiplier;
    }
}

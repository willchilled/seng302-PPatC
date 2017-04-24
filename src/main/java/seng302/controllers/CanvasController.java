package seng302.controllers;

import javafx.animation.*;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Pair;
import seng302.models.Boat;
import seng302.models.BoatPolygon;
import seng302.models.Colors;
import seng302.models.mark.GateMark;
import seng302.models.mark.Mark;
import seng302.models.mark.MarkType;
import seng302.models.mark.SingleMark;

import java.sql.Time;
import java.util.*;

/**
 * Created by ptg19 on 15/03/17.
 * Modified by Haoming Yin (hyi25) on 20/3/2017.
 */
public class CanvasController {

    @FXML
    private AnchorPane canvasPane;

    private RaceViewController raceViewController;
    private ResizableCanvas canvas;
    private Group group;
    private GraphicsContext gc;
    private List<BoatPolygon> boatPolygons = new ArrayList<>();

    private final int MARK_SIZE     = 10;
    private final int BUFFER_SIZE   = 25;
    private final int CANVAS_WIDTH  = 1000;
    private final int CANVAS_HEIGHT = 1000;
    private final int LHS_BUFFER    = BUFFER_SIZE;
    private final int RHS_BUFFER    = BUFFER_SIZE + MARK_SIZE / 2;
    private final int TOP_BUFFER    = BUFFER_SIZE;
    private final int BOT_BUFFER    = TOP_BUFFER + MARK_SIZE / 2;
    private final int FRAME_RATE    = 60;

    private double distanceScaleFactor;
    private ScaleDirection scaleDirection;
    private Mark minLatPoint;
    private Mark minLonPoint;
    private Mark maxLatPoint;
    private Mark maxLonPoint;
    private int referencePointX;
    private int referencePointY;
    private double metersToPixels;

    public AnimationTimer timer;

    private enum ScaleDirection {
        HORIZONTAL,
        VERTICAL
    }

    public void setup(RaceViewController raceViewController){
        this.raceViewController = raceViewController;
    }

    public void initialize() {
        raceViewController = new RaceViewController();
        canvas = new ResizableCanvas();
        group = new Group();

        canvasPane.getChildren().add(canvas);
        canvasPane.getChildren().add(group);
        // Bind canvas size to stack pane size.
        canvas.widthProperty().bind(new SimpleDoubleProperty(CANVAS_WIDTH));
        canvas.heightProperty().bind(new SimpleDoubleProperty(CANVAS_HEIGHT));
        group.minWidth(CANVAS_WIDTH);
        group.minHeight(CANVAS_HEIGHT);
    }

    public void initializeCanvas (){

        gc = canvas.getGraphicsContext2D();
        gc.save();
        gc.setFill(Color.SKYBLUE);
        gc.fillRect(0,0, CANVAS_WIDTH, CANVAS_HEIGHT);
        gc.restore();
        drawCourse();
        drawBoats();
//        drawFps(12);
//        // overriding the handle so that it can clean canvas and redraw boats and course marks
//        AnimationTimer timer = new AnimationTimer() {
//            private long lastUpdate = 0;
//            private long lastFpsUpdate = 0;
//            private int lastFpsCount = 0;
//            private int fpsCount = 0;
//            boolean done = true;
//
//            @Override
//            public void handle(long now) {
//                if (true){ //if statement for limiting refresh rate if needed
////                    gc.clearRect(0, 0, canvas.getWidth(),canvas.getHeight());
////                    gc.setFill(Color.SKYBLUE);
////                    gc.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
//
//
//                    // If race has started, draw the boats and play the timeline
//                    if (raceViewController.getRace().getRaceTime() > 1) {
//                        raceViewController.playTimelines();
//                    }
//                    // Race has not started, pause the timelines
//                    else {
//                        raceViewController.pauseTimelines();
//                    }
//                    lastUpdate = now;
//                    fpsCount ++;
//                    if (now - lastFpsUpdate >= 1000000000){
//                        lastFpsCount = fpsCount;
//                        fpsCount = 0;
//                        lastFpsUpdate = now;
//                    }
//                }
//            }
//        };
//        timer.start();
        //try {
        //    Thread.sleep(10000);
        //}catch (Exception e) {
        //    e.printStackTrace();
        //}
        timer = new AnimationTimer() {

            private int countdown = 60;
            private int[] currentRaceMarker = {1, 1, 1, 1, 1, 1};
            List<Mark> marks = raceViewController.getRace().getCourse();

            @Override
            public void handle(long now) {
                boolean raceFinished = true;
                boolean descending;
                int boatIndex = 0;
                Mark nextMark;
                if (countdown == 0) {
                    for (BoatPolygon bp : boatPolygons) {
                        if (currentRaceMarker[boatIndex] < marks.size()) {
                            if (currentRaceMarker[boatIndex] == 6) {
                                int debugLine = 4;
                            }
                            double xb4 = bp.getLayoutX();
                            double yb4 = bp.getLayoutY();
                            nextMark = marks.get(currentRaceMarker[boatIndex]);
                            if (nextMark.getY() > bp.getLayoutY())
                                descending = true;
                            else
                                descending = false;
                            bp.updatePosition(1000 / 60);
                            if (descending && nextMark.getY() < bp.getLayoutY()) {
                                currentRaceMarker[boatIndex]++;
                                bp.setDestination(
                                        marks.get(currentRaceMarker[boatIndex]).getX(), marks.get(currentRaceMarker[boatIndex]).getY()
                                );
                            } else if (!descending && nextMark.getY() > bp.getLayoutY()) {
                                currentRaceMarker[boatIndex]++;
                                bp.setDestination(
                                        marks.get(currentRaceMarker[boatIndex]).getX(), marks.get(currentRaceMarker[boatIndex]).getY()
                                );
                            }
                            double xnew = bp.getLayoutX();
                            double ynew = bp.getLayoutY();
                            double dx = xnew - xb4;
                            double dy = ynew -yb4;
                            raceFinished = false;
                            boatIndex++;
                        }
                    }
                    if (raceFinished) {
                        System.out.println("DONZEO LADS");
                        this.stop();
                    }
                } else {
                    countdown--;
                }
            }
        };
        for (Mark m : raceViewController.getRace().getCourse())
            System.out.println(m.getName());
        //timer.start();
    }

    class ResizableCanvas extends Canvas {

        public ResizableCanvas() {
            // Redraw canvas when size changes.
            widthProperty().addListener(evt -> draw());
            heightProperty().addListener(evt -> draw());
        }

        private void draw() {
            double width = getWidth();
            double height = getHeight();

            GraphicsContext gc = getGraphicsContext2D();
            gc.clearRect(0, 0, width, height);
        }

        @Override
        public boolean isResizable() {
            return true;
        }

        @Override
        public double prefWidth(double height) {
            return getWidth();
        }

        @Override
        public double prefHeight(double width) {
            return getHeight();
        }
    }

    private void drawFps(int fps){
        if (raceViewController.isDisplayFps()){
            gc.setFill(Color.BLACK);
            gc.setFont(new Font(14));
            gc.setLineWidth(3);
            gc.fillText(fps + " FPS", 5, 20);
        }
    }

    /**
     * Draws all the boats.
     */
    private void drawBoats() {
//        Map<Boat, TimelineInfo> timelineInfos = raceViewController.getTimelineInfos();
        List<Boat> boats  = raceViewController.getStartingBoats();
        List<Mark> marks  = raceViewController.getRace().getCourse();
        Double startingX  = (double) marks.get(0).getX();
        Double startingY  = (double) marks.get(0).getY();
        Double firstMarkX = (double) marks.get(1).getX();
        Double firstMarkY = (double) marks.get(1).getY();

        for (Boat boat : boats) {
            BoatPolygon bp = new BoatPolygon(boat, Colors.getColor());
            bp.moveBoatTo(startingX, startingY);
            bp.setDestination(firstMarkX, firstMarkY);
            group.getChildren().add(bp.getWake());
            group.getChildren().add(bp);
            group.getChildren().add(bp.getTeamNameObject());
            group.getChildren().add(bp.getVelocityObject());
            boatPolygons.add(bp);
//            drawBoat(boat.getLongitude(), boat.getLatitude(), boat.getColor(), boat.getShortName(), boat.getSpeedInKnots(), boat.getHeading());
        }
    }


    /**
     * Inner class for creating point so that you can rotate it around origin point.
     */
    class Point {

        double x, y;

        Point (double x, double y) {
            this.x = x;
            this.y = y;
        }

        void rotate(double angle) {
            double oldX = x;
            double oldY = y;
            this.x = oldX * Math.cos(angle) - oldY * Math.sin(angle);
            this.y = oldX * Math.sin(angle) + oldY * Math.cos(angle);

        }
    }

    /**
     * Draws the course.
     */
    private void drawCourse() {
        fitToCanvas();
        for (Mark mark : raceViewController.getRace().getCourse()) {
            if (mark.getMarkType() == MarkType.SINGLE_MARK) {
                drawSingleMark((SingleMark) mark, Color.BLACK);
            } else {
                drawGateMark((GateMark) mark);
            }
        }
    }

    /**
     * Draw a given mark on canvas
     *
     * @param singleMark
     */
    private void drawSingleMark(SingleMark singleMark, Color color) {
        gc.setFill(color);
        gc.fillOval(singleMark.getX(), singleMark.getY(),MARK_SIZE,MARK_SIZE);
    }

    /**
     * Draw a gate mark which contains two single marks
     *
     * @param gateMark
     */
    private void drawGateMark(GateMark gateMark) {
        Color color = Color.BLUE;

        if (gateMark.getName().equals("Start")){
            color = Color.GREEN;
        }

        if (gateMark.getName().equals("Finish")){
            color = Color.RED;
        }

        drawSingleMark(gateMark.getSingleMark1(), color);
        drawSingleMark(gateMark.getSingleMark2(), color);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.save();
        gc.setStroke(color);
        if (gateMark.getMarkType() == MarkType.OPEN_GATE)
            gc.setLineDashes(3, 5);

        gc.setLineWidth(2);
        gc.strokeLine(
                gateMark.getSingleMark1().getX() + MARK_SIZE / 2,
                gateMark.getSingleMark1().getY() + MARK_SIZE / 2,
                gateMark.getSingleMark2().getX() + MARK_SIZE / 2,
                gateMark.getSingleMark2().getY() + MARK_SIZE / 2
        );
        gc.restore();
    }

    /**
     * Calculates x and y location for every marker that fits it to the canvas the race will be drawn on.
     */
    private void fitToCanvas() {
        findMinMaxPoint();
        double minLonToMaxLon = scaleRaceExtremities();
        calculateReferencePointLocation(minLonToMaxLon);
        givePointsXY();
        findMetersToPixels();
    }


    /**
     * Sets the class variables minLatPoint, maxLatPoint, minLonPoint, maxLonPoint to the marker with the leftmost
     * marker, rightmost marker, southern most marker and northern most marker respectively.
     */
    private void findMinMaxPoint() {
        List<Mark> sortedPoints = new ArrayList<>();
        for (Mark mark : raceViewController.getRace().getCourse())
        {
            if (mark.getMarkType() == MarkType.SINGLE_MARK)
                sortedPoints.add(mark);
            else {
                sortedPoints.add(((GateMark) mark).getSingleMark1());
                sortedPoints.add(((GateMark) mark).getSingleMark2());
            }
        }
        sortedPoints.sort(Comparator.comparingDouble(Mark::getLatitude));
        minLatPoint = sortedPoints.get(0);
        maxLatPoint = sortedPoints.get(sortedPoints.size()-1);

        sortedPoints.sort(Comparator.comparingDouble(Mark::getLongitude));
        //If the course is on a point on the earth where longitudes wrap around.
        // TODO: 30/03/17 cir27 - Correctly account for longitude wrapping around.
        if (sortedPoints.get(sortedPoints.size()-1).getLongitude() - sortedPoints.get(0).getLongitude() > 180)
            Collections.reverse(sortedPoints);
        minLonPoint = sortedPoints.get(0);
        maxLonPoint = sortedPoints.get(sortedPoints.size()-1);
    }

    /**
     * Calculates the location of a reference point, this is always the point with minimum latitude, in relation to the
     * canvas.
     *
     * @param minLonToMaxLon The horizontal distance between the point of minimum longitude to maximum longitude.
     */
    private void calculateReferencePointLocation (double minLonToMaxLon) {
        Mark referencePoint = minLatPoint;
        double referenceAngle;
        double mapWidth = canvas.getWidth();
        double mapHeight = canvas.getHeight();

        if (scaleDirection == ScaleDirection.HORIZONTAL) {
            referenceAngle = Mark.calculateHeadingRad(referencePoint, minLonPoint) - (Math.PI * (3/4));
            referencePointX = LHS_BUFFER + (int) Math.round(distanceScaleFactor * Math.cos(referenceAngle) * Mark.calculateDistance(referencePoint, minLonPoint));

            referenceAngle = Mark.calculateHeadingRad(referencePoint, maxLatPoint);
            if (referenceAngle > (Math.PI / 2)) {
                referenceAngle = (Math.PI * 2) - referenceAngle;
            }
            referencePointY  = (int) Math.round(mapHeight - (TOP_BUFFER + BOT_BUFFER));
            referencePointY -= (int) Math.round(distanceScaleFactor * Math.cos(referenceAngle) * Mark.calculateDistance(referencePoint, maxLatPoint));
            referencePointY  = (int) Math.round(referencePointY / 2d);
            referencePointY += TOP_BUFFER;
            referencePointY += (int) Math.round(distanceScaleFactor * Math.cos(referenceAngle) * Mark.calculateDistance(referencePoint, maxLatPoint));
        } else {
            referencePointY = (int) Math.round(mapHeight - BOT_BUFFER);

            referenceAngle = (Math.PI * 2) - Mark.calculateHeadingRad(referencePoint, minLonPoint);

            referencePointX  = LHS_BUFFER;
            referencePointX += (int) Math.round(distanceScaleFactor * Math.sin(referenceAngle) * Mark.calculateDistance(referencePoint, minLonPoint));
            referencePointX += (int) Math.round(((mapWidth - (LHS_BUFFER + RHS_BUFFER)) - (minLonToMaxLon * distanceScaleFactor)) / 2);
        }
        referencePoint.setX(referencePointX);
        referencePoint.setY(referencePointY);
    }

    /**
     * Finds the scale factor necessary to fit all race markers within the onscreen map and assigns it to distanceScaleFactor
     * Returns the max horizontal distance of the map.
     */
    private double scaleRaceExtremities () {
        double vertAngle = Mark.calculateHeadingRad(minLatPoint, maxLatPoint);
        if (vertAngle > Math.PI)
            vertAngle = (2 * Math.PI) - vertAngle;
        double vertDistance = Math.cos(vertAngle) * Mark.calculateDistance(minLatPoint, maxLatPoint);

        double horiAngle = Mark.calculateHeadingRad(minLonPoint, maxLonPoint);
        if (horiAngle <= (Math.PI / 2))
            horiAngle = (Math.PI / 2) - horiAngle;
        else
            horiAngle = horiAngle - (Math.PI / 2);
        double horiDistance = Math.cos(horiAngle) * Mark.calculateDistance(minLonPoint, maxLonPoint);

        double vertScale = (canvas.getHeight() - (TOP_BUFFER + BOT_BUFFER)) / vertDistance;

        if ((horiDistance * vertScale) > (canvas.getWidth() - (RHS_BUFFER + LHS_BUFFER))) {
            distanceScaleFactor = (canvas.getWidth() - (RHS_BUFFER + LHS_BUFFER)) / horiDistance;
            scaleDirection = ScaleDirection.HORIZONTAL;
        } else {
            distanceScaleFactor = vertScale;
            scaleDirection = ScaleDirection.VERTICAL;
        }
        return horiDistance;
    }

    /**
     * Give all markers in the course an x,y location relative to a given reference with a known x,y location. Distances
     * are scaled according to the distanceScaleFactor variable.
     */
    private void givePointsXY() {
        Point2D canvasLocation;
        List<Mark> allPoints = new ArrayList<>(raceViewController.getRace().getCourse());

        for (Mark mark : allPoints) {
            if (mark.getMarkType() != MarkType.SINGLE_MARK) {
                GateMark gateMark = (GateMark) mark;

                canvasLocation = findScaledXY(gateMark.getSingleMark1());
                gateMark.getSingleMark1().setX((int) canvasLocation.getX());
                gateMark.getSingleMark1().setY((int) canvasLocation.getY());

                canvasLocation = findScaledXY(gateMark.getSingleMark2());
                gateMark.getSingleMark2().setX((int) canvasLocation.getX());
                gateMark.getSingleMark2().setY((int) canvasLocation.getY());
            }
            if (mark.getMarkType() == MarkType.CLOSED_GATE)
                ((GateMark) mark).assignXYCentered();
            else {
                canvasLocation = findScaledXY(mark);
                mark.setX((int) canvasLocation.getX());
                mark.setY((int) canvasLocation.getY());
            }
        }
    }

    private Point2D findScaledXY (Mark unscaled) {
        return findScaledXY (minLatPoint.getLatitude(), minLatPoint.getLongitude(),
                             unscaled.getLatitude(), unscaled.getLongitude());
    }

    private Point2D findScaledXY (double latA, double lonA, double latB, double lonB) {
        double distanceFromReference;
        double angleFromReference;
        int yAxisLocation;
        int xAxisLocation;

        angleFromReference = Mark.calculateHeadingRad(latA, lonA, latB, lonB);
        distanceFromReference = Mark.calculateDistance(latA, lonA, latB, lonB);

        if (angleFromReference > (Math.PI / 2)) {
            angleFromReference = (Math.PI * 2) - angleFromReference;
            xAxisLocation  = referencePointX;
            xAxisLocation -= (int) Math.round(distanceScaleFactor * Math.sin(angleFromReference) * distanceFromReference);
        } else {
            xAxisLocation  = referencePointX;
            xAxisLocation += (int) Math.round(distanceScaleFactor * Math.sin(angleFromReference) * distanceFromReference);
        }
        yAxisLocation  = referencePointY;
        yAxisLocation -= (int) Math.round(distanceScaleFactor * Math.cos(angleFromReference) * distanceFromReference);

        return new Point2D(xAxisLocation, yAxisLocation);
    }



    /**
     * Find the number of meters per pixel.
     */
    private void findMetersToPixels () {
        Double angularDistance;
        Double angle;
        Double straightLineDistance;
        if (scaleDirection == ScaleDirection.HORIZONTAL) {
            angularDistance = Mark.calculateDistance(minLonPoint, maxLonPoint);
            angle = Mark.calculateHeadingRad(minLonPoint, maxLonPoint);
            if (angle > Math.PI / 2) {
                straightLineDistance = Math.cos(angle - Math.PI) * angularDistance;
            } else {
                straightLineDistance = Math.cos(angle) * angularDistance;
            }
            metersToPixels = (CANVAS_WIDTH - RHS_BUFFER - LHS_BUFFER) / straightLineDistance;
        } else {
            angularDistance = Mark.calculateDistance(minLatPoint, maxLatPoint);
            angle = Mark.calculateHeadingRad(minLatPoint, maxLatPoint);
            if (angle < Math.PI / 2) {
                straightLineDistance = Math.cos(angle) * angularDistance;
            } else {
                straightLineDistance = Math.cos(-angle + Math.PI * 2) * angularDistance;
            }
            metersToPixels = (CANVAS_HEIGHT - TOP_BUFFER - BOT_BUFFER) / straightLineDistance;
        }
    }

    private Point2D latLonToXY (double latitude, double longitude) {
        return findScaledXY(minLatPoint.getLatitude(), minLatPoint.getLongitude(), latitude, longitude);
    }
}
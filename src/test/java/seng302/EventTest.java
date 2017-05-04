package seng302;

import org.junit.Test;
import seng302.models.Event;
import seng302.models.Yacht;
import seng302.models.mark.SingleMark;

import static org.junit.Assert.assertEquals;

/**
 * Test for Event class
 * Created by Haoming on 7/03/17.
 */
public class EventTest {

    @Test
    public void getTimeString() throws Exception {
        Yacht boat = new Yacht("testBoat");
        Event event = new Event(1231242.2, boat, new SingleMark("mark1"), new SingleMark("mark2"), 0);
        assertEquals("20:31:242", event.getTimeString());
    }

    @Test
    public void testBoatHeading() throws Exception {
        Yacht boat = new Yacht("testBoat");
        Event event = new Event(1231242.2, boat, new SingleMark("mark1", 142.5, 122.1, 1), new SingleMark("mark2", 121.9,99.2, 2), 0);

        assertEquals(event.getBoatHeading(), 228.0266137055349, 1e-15);
    }

    @Test
    public void testDistanceBetweenMarks() throws Exception {
        Yacht boat = new Yacht("testBoat");
        Event event = new Event(1231242.2, boat, new SingleMark("mark1", 142.5, 122.1, 1), new SingleMark("mark2", 121.9,99.2, 2), 0);

        assertEquals(event.getDistanceBetweenMarks(), 339059.653830461, 1e-15);
    }
}
package seng302.models.mark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import seng302.models.stream.XMLParser;
import seng302.models.xml.Race;
import seng302.models.xml.XMLGenerator;
import seng302.server.messages.XMLMessageSubType;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

/**
 * Class to hold the order of the marks in the race.
 */
public class MarkOrder {
    private List<Mark> raceMarkOrder;
    private Logger logger = LoggerFactory.getLogger(MarkOrder.class);

    public MarkOrder(){
        loadRaceProperties();
    }

    /**
     * @return An ordered list of marks in the race
     *         OR null if the mark order could not be loaded
     */
    public List<Mark> getMarkOrder(){
        if (raceMarkOrder == null){
            logger.warn("Race order accessed but not instantiated");
            return null;
        }

        return Collections.unmodifiableList(raceMarkOrder);
    }

    /**
     * Returns the mark in the race after the previous mark
     * @param previous The previous mark
     * @return the next mark
     *         OR null if there is no next mark
     */
    public Mark getNextMark(Mark previous){
        for (int i = 0; i < raceMarkOrder.size(); i++){
            Mark mark = raceMarkOrder.get(i);

            if (i + 1 >= raceMarkOrder.size()){
                return null;
            }

            if (mark.equals(previous)){
                return raceMarkOrder.get(i+1);
            }
        }

        return null;
    }

    /**
     * Loads the race order from an XML string
     * @param xml An AC35 RaceXML
     * @return An ordered list of marks in the race
     */
    private List<Mark> loadRaceOrderFromXML(String xml){
        XMLParser xmlParser = new XMLParser();
        XMLParser.RaceXMLObject raceXMLObject;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        Document doc;

        try {
            db = dbf.newDocumentBuilder();
            doc = db.parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | IOException | SAXException e) {
            logger.error("Failed to read generated race XML");
            return null;
        }

        xmlParser.constructXML(doc , XMLMessageSubType.RACE.getType());
        raceXMLObject = xmlParser.getRaceXML();

        if (raceXMLObject != null){
            logger.debug("Loaded RaceXML for mark order");
            return raceXMLObject.getNonDupCompoundMarks();
        }

        return null;
    }

    /**
     * Load the raceXML and mark order
     */
    private void loadRaceProperties(){
        XMLGenerator generator = new XMLGenerator();

        generator.setRace(new Race());

        String raceXML = generator.getRaceAsXml();

        if (raceXML == null){
            logger.error("Failed to generate raceXML (for race properties)");
            return;
        }

        raceMarkOrder = loadRaceOrderFromXML(raceXML);
    }
}

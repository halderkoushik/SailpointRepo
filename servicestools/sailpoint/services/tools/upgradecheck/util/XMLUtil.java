package sailpoint.services.tools.upgradecheck.util;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;

/**
 * Created by surya.nellepalli on 18/11/2017.
 */
public class XMLUtil {
    static final Logger log = Logger.getLogger(XMLUtil.class);

    static DocumentBuilderFactory dbf = null;
    static DocumentBuilder db;
    static SPEntityResolver entityResolver;


    public static Document getDocument(File inputFile, String dtdLocation)  {
        if (dbf == null) {
            dbf = DocumentBuilderFactory.newInstance();
            try {
                dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (ParserConfigurationException e) {
               log.warn("error in creating parser",e);
            }

        }
        if (db == null) {
            try {
                db = dbf.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                log.warn(e);
                return null;
            }


        } else {

            db.reset();

        }

        entityResolver = new SPEntityResolver();
        entityResolver.setDtdLocation(dtdLocation);
        db.setEntityResolver(entityResolver);

        Document document = null;
        try {
            document = db.parse(inputFile);
        } catch (Throwable ex) {
            log.warn("error reading " + inputFile.getAbsolutePath(), ex);
            return null;
        }
        return document;

    }

}

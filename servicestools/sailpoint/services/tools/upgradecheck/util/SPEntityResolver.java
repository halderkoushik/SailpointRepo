package sailpoint.services.tools.upgradecheck.util;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.FileNotFoundException;
import java.io.StringReader;

/**
 * Created by surya.nellepalli on 21/09/2017.
 */
public class SPEntityResolver implements EntityResolver {


    private static String _dtd;

    public String getDtdLocation() {
        return _dtdLocation;
    }


    public void setDtdLocation(String _dtdLocation) {
        this._dtdLocation = _dtdLocation;
    }

    private String _dtdLocation;

    private InputSource inputSource;

    public static String getDTD(String dtdLocation) {

        if (_dtd == null) {
            //XMLObjectFactory f = XMLObjectFactory.getInstance();
            //_dtd = f.getDTD();
           // System.out.println("reading from " + dtdLocation);
            _dtd = FileUtils.readFromFile(dtdLocation);


        }

        return _dtd;
    }


    public InputSource resolveEntity(String publicId, String systemId) throws FileNotFoundException {
        //System.out.println("publicId==" + publicId + " , systemId" + systemId);
        if ("sailpoint.dtd".equals(publicId)) {
            // return a special input source

            return getInputSourceForDtd();
        } else {
            // use the default behaviour
            return new InputSource();
        }
    }

    InputSource getInputSourceForDtd() throws FileNotFoundException {
        if (inputSource == null) {
            inputSource = new InputSource(new StringReader(getDTD(_dtdLocation)));
        }
        return inputSource;


    }
}
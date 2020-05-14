package sailpoint.services.tools.upgradecheck.scanner;

import bsh.ParseException;
import org.xml.sax.SAXException;
import sailpoint.services.tools.upgradecheck.util.FileUtils;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * Created by surya.nellepalli on 23/10/2017.
 */
public class Scanner {

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, ParseException {
        ScannerParams params = ScannerParams.getParamsFromFile(FileUtils.getResourcePath("deprecationcheck.properties"));
        if (args == null || args.length == 0 || "both".equalsIgnoreCase(args[0])) {
            DeprecationStringScanner dss = new DeprecationStringScanner(params);
            dss.runDeprecationScanner();
            BSHDeprecationScanner bds = new BSHDeprecationScanner(params);
            bds.runDeprecationScanner();

        } else if ("bsh".equalsIgnoreCase(args[0])) {
            BSHDeprecationScanner bds = new BSHDeprecationScanner(params);
            bds.runDeprecationScanner();
        } else {
            DeprecationStringScanner dss = new DeprecationStringScanner(params);
            dss.runDeprecationScanner();
        }

    }
}

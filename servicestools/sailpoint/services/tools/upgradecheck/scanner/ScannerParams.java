package sailpoint.services.tools.upgradecheck.scanner;

import org.apache.log4j.Logger;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created by surya.nellepalli on 28/09/2017.
 */
public class ScannerParams {

    static final Logger log = Logger.getLogger(ScannerParams.class);


    public enum ScannerParamsEnum {

        ARG_FOLDER_TO_SCAN("folderToScan"),
        ARG_DTD_LOCATION("dtdLocation"),
        ARG_HELPER_LIST("helperList"),
        ARG_RPT_LOCATION("reportLocation"),
        ARG_PKGS_TO_SCAN("packagesToScan"),
        ARG_FOLDERS_TO_IGNORE("foldersToIgnore"),
        ARG_SCAN_NON_PUBLIC("scanNonPublic");

        String _arg;

        ScannerParamsEnum(String arg) {
            _arg = arg;
        }

        @Override
        public String toString() {
            return _arg;
        }
    }


    private HashMap<ScannerParams.ScannerParamsEnum, Object> _params = new HashMap<ScannerParams.ScannerParamsEnum, Object>();

    public void addParam(ScannerParams.ScannerParamsEnum name, Object val) {
        _params.put(name, val);

    }

    public Object getParam(ScannerParams.ScannerParamsEnum name) {
        return _params.get(name);
    }

    public Object getParam(String name) {
        return _params.get(name);
    }


    public String getStringParam(ScannerParams.ScannerParamsEnum name) {
        return (String) getParam(name);
    }

    public static ScannerParams getParamsFromFile(String fileName) {
        ScannerParams _p = new ScannerParams();

        Properties properties = new Properties();
        try {
            FileReader reader = new FileReader(fileName);
            properties.load(reader);
        } catch (Exception e) {
            log.error(e);
        }
        for (ScannerParamsEnum arg : ScannerParamsEnum.values()) {
            _p.addParam(arg, properties.getProperty(arg.toString()));
        }

        return _p;

    }


    public static void main(String args[]) {

        for (ScannerParamsEnum arg : ScannerParamsEnum.values()) {
            System.out.println(arg);
        }
    }

}

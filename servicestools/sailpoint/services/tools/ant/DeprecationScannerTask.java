package sailpoint.services.tools.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import sailpoint.services.tools.upgradecheck.scanner.BSHDeprecationScanner;
import sailpoint.services.tools.upgradecheck.scanner.DeprecationStringScanner;
import sailpoint.services.tools.upgradecheck.scanner.ScannerParams;

import java.io.File;


/**
 * Created by surya.nellepalli on 02/11/2017.
 */
public class DeprecationScannerTask extends Task {


    private String _ignoreFolderList;
    // The setter for the "IgnoreFolderList" attribute
    public void setIgnoreFolderList(String ignoreList) {
        this._ignoreFolderList = ignoreList;
    }
    public String getIgnoreFolderList( ) {
        return this._ignoreFolderList ;
    }


    File _folderToScan;

    public File getFolderToScan() {
        return _folderToScan;
    }

    public void setFolderToScan(File _folderToScan) {
        this._folderToScan = _folderToScan;
    }

    public String getHelperList() {
        return _helperList;
    }

    public void setHelperList(String _helperList) {
        this._helperList = _helperList;
    }

    public File getReportLocation() {
        return _reportLocation;
    }

    public void setReportLocation(File _reportLocation) {
        this._reportLocation = _reportLocation;
    }

    public String getPackagesToScan() {
        return _packagesToScan;
    }

    public void setPackagesToScan(String _packagesToScan) {
        this._packagesToScan = _packagesToScan;
    }

    String _helperList;
    File _reportLocation;
    String _packagesToScan;

    public String getDtdLocation() {
        return _dtdLocation;
    }

    public void setDtdLocation(String _dtdLocation) {
        this._dtdLocation = _dtdLocation;
    }

    String _dtdLocation;


    public String getScannerToRun() {
        return _scannerToRun;
    }

    public void setScannerToRun(String _scannerToRun) {
        this._scannerToRun = _scannerToRun;
    }

    String _scannerToRun;


    public void execute() throws BuildException {

        ScannerParams scannerParams=new ScannerParams();



        scannerParams.addParam(ScannerParams.ScannerParamsEnum.ARG_HELPER_LIST,getHelperList());
        scannerParams.addParam(ScannerParams.ScannerParamsEnum.ARG_FOLDER_TO_SCAN,getFolderToScan().getAbsolutePath());
        scannerParams.addParam(ScannerParams.ScannerParamsEnum.ARG_RPT_LOCATION,getReportLocation().getAbsolutePath());
        scannerParams.addParam(ScannerParams.ScannerParamsEnum.ARG_PKGS_TO_SCAN,getPackagesToScan());
        scannerParams.addParam(ScannerParams.ScannerParamsEnum.ARG_DTD_LOCATION,getDtdLocation());
        scannerParams.addParam(ScannerParams.ScannerParamsEnum.ARG_FOLDERS_TO_IGNORE,getIgnoreFolderList());


        if( "both".equalsIgnoreCase(getScannerToRun()))
        {
            //log("*****Stage2");
            DeprecationStringScanner dss=new DeprecationStringScanner(scannerParams);
            try {
              //  log("*****Stage3");
                dss.runDeprecationScanner();
                //log("*****Stage4");
            } catch (Exception e) {
                e.printStackTrace();
                log(e, 0);
            }
            //log("*****Stage5");
            BSHDeprecationScanner bds = new BSHDeprecationScanner(scannerParams);
            try {
                //log("*****Stage6");
                bds.runDeprecationScanner();
            } catch (Exception e) {
                e.printStackTrace();
                log(e,0);
            }

        }
        else if ("bsh".equalsIgnoreCase(getScannerToRun())){
            //log("*****Stage7");
            BSHDeprecationScanner bds = new BSHDeprecationScanner(scannerParams);
            try {
                bds.runDeprecationScanner();
            } catch (Exception e) {
                e.printStackTrace();
                log(e,0);
            }
        }
        else {
            DeprecationStringScanner dss=new DeprecationStringScanner(scannerParams);
            try {
                dss.runDeprecationScanner();
            } catch (Exception e) {
                e.printStackTrace();
                log(e, 0);
            }
        }

    }
}

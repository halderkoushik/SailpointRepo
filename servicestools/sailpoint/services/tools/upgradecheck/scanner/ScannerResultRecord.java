package sailpoint.services.tools.upgradecheck.scanner;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by surya.nellepalli on 28/09/2017.
 *
 * Represents one item of information indicating where deprecations were found
 *
 */
public class ScannerResultRecord {

    public ScannerResultRecord()
    {
        deprecatedMethods=new ArrayList<String>();
    }

    public ScannerResultRecord(String fname, String oName){
        setFileName(fname);
        setObjectName(oName);
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    int line;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    String fileName;

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    String objectName;

    public List<String> getDeprecatedMethods() {
        return deprecatedMethods;
    }

    public void setDeprecatedMethods(List<String> deprecatedMethods) {
        this.deprecatedMethods = deprecatedMethods;
    }

    List<String>  deprecatedMethods;


    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    String text;





}

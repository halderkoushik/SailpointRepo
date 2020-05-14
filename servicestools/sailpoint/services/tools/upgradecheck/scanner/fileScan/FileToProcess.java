package sailpoint.services.tools.upgradecheck.scanner.fileScan;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by surya.nellepalli on 28/09/2017.
 * <p>
 * We process One XML File at a Time
 * in each XML File we process single/multiple SailPoint Objects
 * each SailPoint Object could have variables and one or more BeanShell Scripts
 * each Script can have multiple Blocks and deprecations!
 */
public class FileToProcess {
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    String fileName;

    public List<SPObject> getSpObjects() {
        return spObjects;
    }

    public void setSpObjects(List<SPObject> spObjects) {
        this.spObjects = spObjects;
    }

    public void addSpObjects(SPObject spObject) {
        this.spObjects.add(spObject);
    }

    private List<SPObject> spObjects = new ArrayList<SPObject>();


}

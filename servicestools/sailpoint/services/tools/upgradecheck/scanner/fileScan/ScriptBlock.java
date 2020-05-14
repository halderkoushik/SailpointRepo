package sailpoint.services.tools.upgradecheck.scanner.fileScan;

import sailpoint.services.tools.upgradecheck.scanner.ScannerResultRecord;

import java.util.List;

/**
 * Created by surya.nellepalli on 28/09/2017.
 */
public class ScriptBlock {
    public String getBeanShellScript() {
        return beanShellScript;
    }

    public void setBeanShellScript(String beanShellScript) {
        this.beanShellScript = beanShellScript;
    }

    String beanShellScript;

    public List<ScannerResultRecord> getScanResultList() {
        return _scanResultList;
    }

    public void setScanResultList(List<ScannerResultRecord> scanResultList) {
        this._scanResultList = scanResultList;
    }

    List<ScannerResultRecord> _scanResultList;

}

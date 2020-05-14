package sailpoint.services.tools.upgradecheck.scanner.fileScan;

/**
 * Created by surya.nellepalli on 28/09/2017.
 */
public class SpBshVariable {
    public String getVarName() {
        return varName;
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }

    String varName;

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    String dataType;

    public static SpBshVariable buildSpBshVariable(String varName, String dataType) {
        SpBshVariable var = new SpBshVariable();
        var.setDataType(dataType);
        var.setVarName(varName);
        return var;
    }
}

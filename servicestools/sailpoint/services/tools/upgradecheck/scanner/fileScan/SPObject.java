package sailpoint.services.tools.upgradecheck.scanner.fileScan;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by surya.nellepalli on 28/09/2017.
 */
public class SPObject {
    public List<ScriptBlock> getScriptBlocks() {
        return scriptBlocks;
    }

    public void setScriptBlocks(List<ScriptBlock> scriptBlocks) {
        this.scriptBlocks = scriptBlocks;
    }

    public void addScriptBlocks(ScriptBlock scriptBlock) {
        this.scriptBlocks.add(scriptBlock);
    }

    private List<ScriptBlock> scriptBlocks = new ArrayList<ScriptBlock>();

    public List<SpBshVariable> getVars() {
        return vars;
    }

    public void setVars(List<SpBshVariable> vars) {
        this.vars = vars;
    }

    private List<SpBshVariable> vars = new ArrayList<SpBshVariable>();

    public String getObjectType() {
        return _objectType;
    }

    public void setObjectType(String _objectType) {
        this._objectType = _objectType;
    }

    private String _objectType;


    public String getObjectName() {
        return _objectName;
    }

    public void setObjectName(String _objectName) {
        this._objectName = _objectName;
    }

    private String _objectName;

    public String prettyPrintVars() {
        StringBuffer sb = new StringBuffer();
        if (vars != null) {
            for (SpBshVariable var : vars) {
                sb.append(var.getVarName()).append("==").append(var.getDataType()).append("\n");
            }
        } else {
            sb.append("*** No Variables found\n");
        }
        return sb.toString();
    }


}

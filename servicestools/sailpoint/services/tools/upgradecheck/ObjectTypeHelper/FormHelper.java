package sailpoint.services.tools.upgradecheck.ObjectTypeHelper;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sailpoint.services.tools.upgradecheck.scanner.fileScan.ScriptBlock;
import sailpoint.services.tools.upgradecheck.scanner.fileScan.SpBshVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by surya.nellepalli on 19/11/2017.
 */
public class FormHelper extends AbstractObjectTypeHelper {

    static final Logger log = Logger.getLogger(FormHelper.class);

    static final Map<String, String> standardFormTypes = new HashMap<String, String>();

    static {
        //Valid values are: string, int, long, boolean, date, secret, and SailPoint object types (Identity, Bundle, Permission, Rule, Application, etc

        standardFormTypes.put("string", "java.lang.String");
        standardFormTypes.put("secret", "java.lang.String");
        standardFormTypes.put("int", "int");
        standardFormTypes.put("long", "long");
        standardFormTypes.put("boolean", "boolean");
        standardFormTypes.put("date", "java.util.Date");
        SPObjectHelperManager.registerHelper(new FormHelper());


    }


    @Override
    public boolean excludeNode(Document document, String rootElement) {
        return false;
    }

    @Override
    public boolean excludeNode(Node document) {
        return false;
    }

    @Override
    public List<SpBshVariable> getPrefinedVars() {
        List<SpBshVariable> vars = super.getPrefinedVars();
        vars.add(SpBshVariable.buildSpBshVariable("log", "org.apache.commons.logging.Log"));
        return vars;
    }

    @Override
    public String objectHandled() {
        return "Form";
    }

    @Override
    public String getVariableTagName() {
        return null;
    }

    @Override
    public List<SpBshVariable> getVariables(Element xmlElement) {
        List<SpBshVariable> bshVariables = new ArrayList<SpBshVariable>();
        NodeList formFields = xmlElement.getElementsByTagName("Field");
        if (formFields != null) {
            int nFields = formFields.getLength();
            if (log.isDebugEnabled()) log.debug("found " + nFields + " fields");
            for (int currentField = 0; currentField < nFields; currentField++) {

                Element field = (Element) formFields.item(currentField);
                String name = field.getAttribute("name");
                if (name == null || "".equals(name))
                    name = field.getAttribute("displayName");
                if (name == null || "".equals(name)) {
                    log.warn("unable to find name/displayName for field"+field.toString());
                    continue;
                }
                SpBshVariable variable = new SpBshVariable();
                variable.setVarName(name);
                String dataType = field.getAttribute("type");

                if (dataType != null && !"".equals(dataType)) {
                    // conversions
                    dataType = dataType.toLowerCase();
                    String standard = standardFormTypes.get(dataType);
                    if (standard == null)
                        dataType = "sailpoint.object." + dataType;
                    variable.setDataType(dataType);
                }

                bshVariables.add(variable);

            }


        }
        return bshVariables;

    }

    @Override
    public List<ScriptBlock> buildScriptList(Element element) {
        return super.buildScriptList(element);
    }
}

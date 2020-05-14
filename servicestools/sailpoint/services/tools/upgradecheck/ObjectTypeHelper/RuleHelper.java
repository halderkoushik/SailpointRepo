package sailpoint.services.tools.upgradecheck.ObjectTypeHelper;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sailpoint.services.tools.upgradecheck.scanner.fileScan.ScriptBlock;
import sailpoint.services.tools.upgradecheck.scanner.fileScan.SpBshVariable;

import java.util.*;

/**
 * Created by surya.nellepalli on 28/09/2017.
 */
public class RuleHelper extends AbstractObjectTypeHelper {

    static final Logger log = Logger.getLogger(RuleHelper.class);

    static final Map<String, String> standardRuleTypes = new HashMap<String, String>();
    static final Map<String, String> fuzzyRuleTypes = new HashMap<String, String>();

    static private HashSet<String> preDefinedVarNames=new HashSet<String>();

    private static List ruleTypesToExclude = new ArrayList();

    static {
        //Valid values are: string, int, long, boolean, date, secret, and SailPoint object types (Identity, Bundle, Permission, Rule, Application, etc

        standardRuleTypes.put("map", "java.util.Map");
        standardRuleTypes.put("secret", "java.lang.String");
        standardRuleTypes.put("int", "int");
        standardRuleTypes.put("long", "long");
        standardRuleTypes.put("boolean", "boolean");
        standardRuleTypes.put("date", "java.util.Date");
        standardRuleTypes.put("object", "java.lang.Object");
        standardRuleTypes.put("link", "sailpoint.object.Link");
        standardRuleTypes.put("identity", "sailpoint.object.Identity");
        standardRuleTypes.put("application", "sailpoint.object.Application");
        standardRuleTypes.put("bundle", "sailpoint.object.Bundle");
        standardRuleTypes.put("managedattribute", "sailpoint.object.ManagedAttribute");
        standardRuleTypes.put("provisioningproject", "sailpoint.object.ProvisioningProject");
        standardRuleTypes.put("provisioningplan", "sailpoint.object.ProvisioningPlan");
        standardRuleTypes.put("provisioningplan.accountrequest", "sailpoint.object.ProvisioningPlan.AccountRequest");
        standardRuleTypes.put("provisioningplan.objectrequest", "sailpoint.object.ProvisioningPlan.ObjectRequest");
        standardRuleTypes.put("provisioningplan.objectrequest", "sailpoint.object.ProvisioningPlan.ObjectRequest");
        standardRuleTypes.put("template", "sailpoint.object.Template");
        standardRuleTypes.put("field", "sailpoint.object.Field");

        preDefinedVarNames.add("log");


        ruleTypesToExclude.add("connectoraftercreate");
        ruleTypesToExclude.add("connectoraftermodify");
        ruleTypesToExclude.add("connectorafterdelete");
        ruleTypesToExclude.add("connectorbeforecreate");
        ruleTypesToExclude.add("connectorbeforemodify");
        ruleTypesToExclude.add("connectorbeforedelete");
        // System.out.println("%%%%REGISTERED RULE%%%%");

        fuzzyRuleTypes.put("identity", "sailpoint.object.Identity");
        fuzzyRuleTypes.put("previousIdentity", "sailpoint.object.Identity");
        fuzzyRuleTypes.put("newIdentity", "sailpoint.object.Identity");

        fuzzyRuleTypes.put("identityName", "java.lang.String");
        fuzzyRuleTypes.put("role", "sailpoint.object.Bundle");
        fuzzyRuleTypes.put("handler", "sailpoint.workflow.WorkflowHandler");


        fuzzyRuleTypes.put("plan", "sailpoint.object.ProvisioningPlan");
        fuzzyRuleTypes.put("project", "sailpoint.object.ProvisioningProject");
        fuzzyRuleTypes.put("event", "sailpoint.object.IdentityChangeEvent");
        fuzzyRuleTypes.put("trigger", "sailpoint.object.IdentityTrigger");
        fuzzyRuleTypes.put("link", "sailpoint.object.Link");
        fuzzyRuleTypes.put("context", "sailpoint.api.SailPointContext");
        fuzzyRuleTypes.put("application", "sailpoint.object.Application");
        fuzzyRuleTypes.put("app", "sailpoint.object.Application");
        fuzzyRuleTypes.put("connector", "sailpoint.object.Connector");
        fuzzyRuleTypes.put("violation", "sailpoint.object.PolicyViolation");
        fuzzyRuleTypes.put("workflow", "sailpoint.object.Workflow");
        fuzzyRuleTypes.put("policy", "sailpoint.object.Policy");


        fuzzyRuleTypes.put("wfcontext", "sailpoint.workflow.WorkflowContext");
        fuzzyRuleTypes.put("resourceObject", "sailpoint.object.ResourceObject");
        fuzzyRuleTypes.put("approval", "sailpoint.object.Workflow.Approval");
        fuzzyRuleTypes.put("identityAttributeName", "java.lang.String");
        fuzzyRuleTypes.put("identityAttributeValue", "java.lang.Object");






        SPObjectHelperManager.registerHelper(new RuleHelper());
    }

    public boolean excludeNode(Document document, String rootElement) {

        String elmType = document.getDocumentElement().getAttribute("type");
        if (elmType == null)
            elmType = "";

        return ruleTypesToExclude.contains(elmType.toLowerCase());

    }

    public boolean excludeNode(Node node) {


        String elmType = ((Element) node).getAttribute("type");
        if(log.isDebugEnabled()) log.debug("Checking if "+elmType+" should be excluded");
        if(elmType!=null)
            return ruleTypesToExclude.contains(elmType.toLowerCase());
        return false;

    }

    @Override
    public String objectHandled() {
        return "Rule";
    }

    @Override
    public List<SpBshVariable> getPrefinedVars() {
        List<SpBshVariable> vars = super.getPrefinedVars();
        vars.add(SpBshVariable.buildSpBshVariable("log", "org.apache.commons.logging.Log"));
        return vars;
    }

    @Override
    public List<ScriptBlock> buildScriptList(Element element) {
        return super.buildScriptList(element);
    }

    @Override
    public String getVariableTagName() {
        return "Argument";
    }

    @Override
    public List<SpBshVariable> getVariables(Element xmlElement) {
        List<SpBshVariable> bshVariables = new ArrayList<SpBshVariable>();
        NodeList ruleArgs = xmlElement.getElementsByTagName(getVariableTagName());
        if (ruleArgs != null) {
            int nFields = ruleArgs.getLength();
            if (log.isDebugEnabled()) log.debug("found " + nFields + " Args");
            for (int currentField = 0; currentField < nFields; currentField++) {

                Element field = (Element) ruleArgs.item(currentField);
                String name = field.getAttribute("name");

                if (name == null || "".equals(name)) {
                    log.warn("unable to find name for Argument");
                    continue;
                }
                if ("context".equals(name))
                    continue;
                SpBshVariable variable = new SpBshVariable();
                variable.setVarName(name);
                String dataType = field.getAttribute("type");
                if(preDefinedVarNames.contains(name) && (dataType==null || "".equals(dataType)))
                    continue;

                if (dataType == null || "".equals(dataType)) {

                    dataType = fuzzyRuleTypes.get(name);

                }
                if (dataType != null && !"".equals(dataType)) {
                    // conversions
                    String dataTypeLower = dataType.toLowerCase();
                    String standard = standardRuleTypes.get(dataTypeLower);

                    if (standard != null)
                        variable.setDataType(standard);
                    else
                        variable.setDataType(dataType);
                }
               // System.out.println("var name:"+variable.getVarName()+"=="+variable.getDataType());
                bshVariables.add(variable);

            }


        }
        return bshVariables;
    }


}

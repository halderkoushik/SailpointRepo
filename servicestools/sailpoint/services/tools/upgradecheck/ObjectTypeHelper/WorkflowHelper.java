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
public class WorkflowHelper extends AbstractObjectTypeHelper {

    static final Logger log = Logger.getLogger(WorkflowHelper.class);


    static final Map<String, String> standardWorkflowTypes = new HashMap<String, String>();
    static final Map<String, String> fuzzyWorkflowTypes = new HashMap<String, String>();
    static private HashSet<String> preDefinedVarNames=new HashSet<String>();


    static {
        //Valid values are: string, int, long, boolean, date, secret, and SailPoint object types (Identity, Bundle, Permission, Rule, Application, etc

        standardWorkflowTypes.put("string", "java.lang.String");

        standardWorkflowTypes.put("int", "int");
        standardWorkflowTypes.put("long", "long");
        standardWorkflowTypes.put("boolean", "boolean");
        standardWorkflowTypes.put("date", "java.util.Date");

        fuzzyWorkflowTypes.put("identity", "sailpoint.object.Identity");
        fuzzyWorkflowTypes.put("identityName", "java.lang.String");
        fuzzyWorkflowTypes.put("identityDisplayName", "java.lang.String");
        fuzzyWorkflowTypes.put("workflowName", "java.lang.String");
        fuzzyWorkflowTypes.put("trace", "java.lang.Boolean");
        fuzzyWorkflowTypes.put("launcher", "java.lang.String");
        fuzzyWorkflowTypes.put("quickLinkIdentityId", "java.lang.String");
        fuzzyWorkflowTypes.put("approvalScheme", "java.lang.String");
        fuzzyWorkflowTypes.put("fallbackApprover", "java.lang.String");
        fuzzyWorkflowTypes.put("source", "java.lang.String");
        fuzzyWorkflowTypes.put("approvalSet", "sailpoint.object.ApprovalSet");
        fuzzyWorkflowTypes.put("plan", "sailpoint.object.ProvisioningPlan");
        fuzzyWorkflowTypes.put("project", "sailpoint.object.ProvisioningProject");
        fuzzyWorkflowTypes.put("event", "sailpoint.object.IdentityChangeEvent");
        fuzzyWorkflowTypes.put("trigger", "sailpoint.object.IdentityTrigger");

        preDefinedVarNames.add("approval");
        preDefinedVarNames.add("workflow");preDefinedVarNames.add("step");preDefinedVarNames.add("item");
        preDefinedVarNames.add("wfcontext");preDefinedVarNames.add("handler");preDefinedVarNames.add("wfcase");


        SPObjectHelperManager.registerHelper(new WorkflowHelper());
    }


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
        vars.add(SpBshVariable.buildSpBshVariable("wfcontext", "sailpoint.workflow.WorkflowContext"));
        vars.add(SpBshVariable.buildSpBshVariable("handler", "sailpoint.workflow.WorkflowHandler"));
        vars.add(SpBshVariable.buildSpBshVariable("wfcase", "sailpoint.workflow.WorkflowHandler"));
        vars.add(SpBshVariable.buildSpBshVariable("workflow", "sailpoint.object.Workflow"));
        vars.add(SpBshVariable.buildSpBshVariable("step", "sailpoint.object.Workflow.Step"));
        vars.add(SpBshVariable.buildSpBshVariable("item", "sailpoint.object.WorkItem"));
        vars.add(SpBshVariable.buildSpBshVariable("approval", "sailpoint.object.Workflow.Approval"));

        /*
         args.put("wfcontext", wfc);

        // get most stuff out of the context since we usually need them
        args.put("handler", wfc.getWorkflowHandler());
        args.put("wfcase", wfc.getWorkflowCase());
        args.put("workflow", wfc.getWorkflow());
        args.put("step", wfc.getStep());
        args.put("approval", wfc.getApproval());
        args.put("item", wfc.getWorkItem());
         */
        return vars;
    }

    @Override
    public List<ScriptBlock> buildScriptList(Element element) {
        return super.buildScriptList(element);
    }

    @Override
    public String objectHandled() {
        return "Workflow";
    }

    @Override
    public String getVariableTagName() {
        return "Variable";
    }

    @Override
    public List<SpBshVariable> getVariables(Element xmlElement) {
        List<SpBshVariable> bshVariables = new ArrayList<SpBshVariable>();
        NodeList workflowVars = xmlElement.getElementsByTagName("Variable");
        if (workflowVars != null) {
            int nFields = workflowVars.getLength();
            if (log.isDebugEnabled()) log.debug("found " + nFields + " Variables");
            for (int currentField = 0; currentField < nFields; currentField++) {

                Element field = (Element) workflowVars.item(currentField);
                String name = field.getAttribute("name");

                if (name == null || "".equals(name)) {
                    log.warn("unable to find name/displayName for Variable");
                    continue;
                }
                SpBshVariable variable = new SpBshVariable();
                variable.setVarName(name);
                String dataType = field.getAttribute("type");
                if(preDefinedVarNames.contains(name) && (dataType==null || "".equals(dataType)))
                    continue;

                if (dataType == null || "".equals(dataType)) {

                    dataType = fuzzyWorkflowTypes.get(name);
                }
                if (dataType != null && !"".equals(dataType)) {
                    // conversions
                    String dataTypeLower = dataType.toLowerCase();
                    String standard = standardWorkflowTypes.get(dataTypeLower);
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

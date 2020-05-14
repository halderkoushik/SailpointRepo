package sailpoint.services.tools.upgradecheck.ObjectTypeHelper;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sailpoint.services.tools.upgradecheck.scanner.fileScan.SPObject;
import sailpoint.services.tools.upgradecheck.scanner.fileScan.ScriptBlock;
import sailpoint.services.tools.upgradecheck.scanner.fileScan.SpBshVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by surya.nellepalli on 28/09/2017.
 */
public class SPObjectHelperManager {
    static final Logger log = Logger.getLogger(SPObjectHelperManager.class);


    private static HashMap<String, AbstractObjectTypeHelper> helperMap = new HashMap<String, AbstractObjectTypeHelper>();


    public static boolean registerHelper(AbstractObjectTypeHelper helper) {

        helperMap.put(helper.objectHandled(), helper);
        return true;
    }

    public static boolean excludeNode(Document document, String objectType) {

        AbstractObjectTypeHelper helper = helperMap.get(objectType);
        if (helper != null)
            return helper.excludeNode(document, objectType);
        return false;
    }

    public static boolean excludeNode(Node node, String objectType) {

        AbstractObjectTypeHelper helper = helperMap.get(objectType);
        if (helper != null)
            return helper.excludeNode(node);
        return false;
    }

    public static boolean canHandleObjectType(String objectType) {

        if (helperMap.get(objectType) != null)
            return true;
        return false;
    }

    public static List<SpBshVariable> getPredefinedVars(String objectType) {
        AbstractObjectTypeHelper helper = helperMap.get(objectType);
        return helper.getPrefinedVars();
    }

    public static Set<String> getAllSupportedObject() {

        return helperMap.keySet();
    }

    public static String getVariableTagName(String objectType) {
        AbstractObjectTypeHelper helper = helperMap.get(objectType);
        if (helper != null)
            return helper.getVariableTagName();
        return "";
    }


    /*
    Input is one document which can contain one or more objects of interest like workflow
    rule
     */
    public static List<SPObject> buildScriptList(Document document) {


        List<SPObject> spObjects = new ArrayList<SPObject>();
        Set<String> supportedObjects = getAllSupportedObject();

        if (supportedObjects == null || supportedObjects.size() == 0) {
            log.warn("Helpers not set");
            return null;
        }
        // try to find each object we support
        for (String supportedObject : supportedObjects) {
            if (log.isDebugEnabled()) {
                log.debug(" checking for " + supportedObject);
            }

            NodeList spObjectList = document.getElementsByTagName(supportedObject);


            if (spObjectList == null) {
                if (log.isDebugEnabled()) {
                    log.debug("no nodes found of type " + supportedObject);
                }
                continue;

            }


            int numOfNodes = spObjectList.getLength();
            for (int nodeCnt = 0; nodeCnt < numOfNodes; nodeCnt++) {
                //check if needs to be excluded
                Node currentNode = spObjectList.item(nodeCnt);
                String objectName = ((Element) currentNode).getAttribute("name");
                if (log.isDebugEnabled()) {
                    log.debug("XML Found Object==" + objectName);
                }
                if (excludeNode(currentNode, supportedObject)) {
                    if (log.isDebugEnabled()) {
                        log.debug("XML Object " + objectName + " excluded from Deprecation Analysis");
                    }
                    continue;

                }


                SPObject spObject = new SPObject();
                spObject.setObjectName(objectName);
                spObject.setObjectType(supportedObject);


                AbstractObjectTypeHelper helper = helperMap.get(supportedObject);
                List<ScriptBlock> scripts = helper.buildScriptList((Element) currentNode);

                spObject.setScriptBlocks(scripts);
                List<SpBshVariable> vars = helper.getPrefinedVars();
                List<SpBshVariable> vars2 = helper.getVariables((Element) currentNode);
                if (vars2 != null)
                    vars.addAll(vars2);

                spObject.setVars(vars);

                spObjects.add(spObject);


            }


        }
        return spObjects;
    }


}

package sailpoint.services.tools.upgradecheck.ObjectTypeHelper;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sailpoint.services.tools.upgradecheck.scanner.fileScan.ScriptBlock;
import sailpoint.services.tools.upgradecheck.scanner.fileScan.SpBshVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by surya.nellepalli on 28/09/2017.
 */
public abstract class AbstractObjectTypeHelper {

    static final Logger log = Logger.getLogger(AbstractObjectTypeHelper.class);


    public abstract boolean excludeNode(Document document, String rootElement);

    public abstract boolean excludeNode(Node document);

    public abstract String objectHandled();

    public abstract String getVariableTagName();

    public abstract List<SpBshVariable> getVariables(Element xmlElement);

    private List<SpBshVariable> _predefinedVars = new ArrayList<SpBshVariable>();

    public List<SpBshVariable> getPrefinedVars() {

        _predefinedVars.add(SpBshVariable.buildSpBshVariable("context", "sailpoint.api.SailPointContext"));
        return _predefinedVars;
    }

    public List<ScriptBlock> buildScriptList(Element element) {
        List<ScriptBlock> scriptBlocks = new ArrayList<ScriptBlock>();
        Node type = element.getAttributes().getNamedItem("type");
        Node name = element.getAttributes().getNamedItem("name");

        if (log.isDebugEnabled()) {
            log.debug("buildScriptList called ");
        }

        NodeList nListScript = element.getElementsByTagName("Source");


        if (nListScript == null) nListScript = element.getElementsByTagName("source");
        if (nListScript == null) nListScript = element.getElementsByTagName("SOURCE");

        if (nListScript != null && nListScript.getLength() > 0) {
            if (log.isDebugEnabled()) {
                log.debug("got source items ");
            }

            for (int temp = 0; temp < nListScript.getLength(); temp++) {
                Node nNode = nListScript.item(temp);
                if (log.isDebugEnabled()) {
                    log.debug("\nCurrent Element :" + nNode.getNodeName());
                }

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if (log.isDebugEnabled()) {
                        log.debug("Script : " + eElement.getTextContent());
                    }

                    String script = eElement.getTextContent();
                    ScriptBlock scriptBlock = new ScriptBlock();

                    scriptBlock.setBeanShellScript(script);
                    scriptBlocks.add(scriptBlock);


                }
            }
        }


        return scriptBlocks;
    }


}

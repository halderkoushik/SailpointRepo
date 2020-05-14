package sailpoint.services.tools.upgradecheck.scanner;


import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import sailpoint.services.tools.upgradecheck.ObjectTypeHelper.SPObjectHelperManager;
import sailpoint.services.tools.upgradecheck.scanner.fileScan.FileToProcess;
import sailpoint.services.tools.upgradecheck.scanner.fileScan.SPObject;
import sailpoint.services.tools.upgradecheck.util.ClassUtils;
import sailpoint.services.tools.upgradecheck.util.XMLUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by surya.nellepalli on 02/10/2017.
 */
public class DeprecationScanner {


    static final Logger log = Logger.getLogger(DeprecationScanner.class);
    private static String _dtd = null;
    protected List<File> listFilesToProcess;
    ScannerParams _scannerParams = new ScannerParams();


    public DeprecationScanner(ScannerParams scannerParams) {
        _scannerParams = scannerParams;
    }


    protected ScannerParams getScannerParams() {
        return _scannerParams;
    }

    public void loadHelpers() {
        // load Helper class which help compute BSH variables for certain SailPoint Objects like Workflow,Rules etc
        if (getScannerParams().getStringParam(ScannerParams.ScannerParamsEnum.ARG_HELPER_LIST) != null) {
            String[] helpers = getScannerParams().getStringParam(ScannerParams.ScannerParamsEnum.ARG_HELPER_LIST).split(",");

            for (String helper : helpers) {
                //System.out.println(helper);
                ClassUtils.isClass(helper);
            }
        }
    }

    protected FileToProcess buildFileToProcess(File inputFile) throws IOException {

        String dtdLocation = _scannerParams.getStringParam(ScannerParams.ScannerParamsEnum.ARG_DTD_LOCATION);

        Document document = XMLUtil.getDocument(inputFile, dtdLocation);

        FileToProcess fileToProcess = new FileToProcess();
        fileToProcess.setFileName(inputFile.getAbsolutePath());
        if (log.isDebugEnabled()) {
            log.debug(" Building " + inputFile.getAbsolutePath());
        }
        if (document == null)
            return fileToProcess;

        document.getDocumentElement().normalize();


        List<SPObject> spObjects = SPObjectHelperManager.buildScriptList(document);
        fileToProcess.setSpObjects(spObjects);

       /* if (SPObjectHelperManager.excludeNode(document, rootElement)) return null;

        // TODO change to support multiple objects per file

        if (SPObjectHelperManager.canHandleObjectType(rootElement)) {

            NodeList nListArgs = null;

            SPObject spObject = new SPObject();
            spObject.setObjectType(rootElement);
            spObject.setObjectName(objectName);


            // extract Variables from this XML Block and associate with current SP Object
            List<SpBshVariable> vars=extractVariables(document, rootElement, SPObjectHelperManager.getVariableTagName(rootElement));
            if(vars==null)
                vars=new ArrayList<SpBshVariable>();
            vars.addAll(SPObjectHelperManager.getPredefinedVars(spObject.getObjectType()));
            spObject.setVars(vars);


            // get list of Scripts

            NodeList nListScript = document.getElementsByTagName("Source");

            if (nListScript == null) nListScript = document.getElementsByTagName("source");
            if (nListScript == null) nListScript = document.getElementsByTagName("SOURCE");

            if (nListScript != null && nListScript.getLength() > 0) {


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
                        ScriptBlock scriptBlock = new ScriptBlock();
                        String script = eElement.getTextContent();
                        scriptBlock.setBeanShellScript(script);
                        spObject.addScriptBlocks(scriptBlock);


                    }
                }
            }
            fileToProcess.addSpObjects(spObject);
        }else
        {
            if(log.isDebugEnabled()){
                log.debug("not handling "+rootElement);
            }
        }

        return fileToProcess;
        */
        return fileToProcess;
    }



    /*
        Extracts variables

    private List<SpBshVariable> extractVariables(Document document, String rootElement, String variableArgumentName) {

        List<SpBshVariable> variables = new ArrayList<SpBshVariable>();

        if (variableArgumentName == null || "".equals(variableArgumentName))
            return variables;

        NodeList nListArgs = document.getElementsByTagName(variableArgumentName);
        if (nListArgs != null && nListArgs.getLength() > 0) {
            for (int temp = 0; temp < nListArgs.getLength(); temp++) {
                Node nNode = nListArgs.item(temp);
                if (log.isDebugEnabled()) {
                    log.debug("\nCurrent Element :" + nNode.getNodeName());
                }
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if (log.isDebugEnabled()) {
                        log.debug("Argument name : " + eElement.getAttribute("name"));
                    }
                    String name = eElement.getAttribute("name");
                    if (name != null) {
                        SpBshVariable variable = new SpBshVariable();
                        variable.setVarName(name);
                        variable.setDataType("Ambiguous");
                        variables.add(variable);
                    }

                }
            }
        }
        return variables;
    }
    */

    protected List<File> getListOfFilesToProcess() {
        return listFilesToProcess;
    }


    protected void createListOfFilesToProcess(File fileFolder) {
        if (listFilesToProcess == null) listFilesToProcess = new ArrayList<File>();

        if (fileFolder.isFile()) {
            if (fileFolder.getName().toLowerCase().endsWith(".xml")) {
                if (log.isDebugEnabled()) {
                    log.debug(" XML file to be processed :" + fileFolder.getPath());
                }
                listFilesToProcess.add(fileFolder);
                return;
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(" Folder to be processed :" + fileFolder.getPath());
            }
            String ignoreFolderList=getScannerParams().getStringParam(ScannerParams.ScannerParamsEnum.ARG_FOLDERS_TO_IGNORE);
            if(ignoreFolderList==null ||
                    ignoreFolderList.indexOf(fileFolder.getName())<0) {
                File[] filesInFolder = fileFolder.listFiles();
                for (int i = 0; i < filesInFolder.length; i++) {
                    createListOfFilesToProcess(new File(filesInFolder[i].getPath()));
                }
            }
        }
    }


    public void setFile(String inputFileName) {
        //log.setLevel(Level.DEBUG);

        if (listFilesToProcess == null) listFilesToProcess = new ArrayList<File>();
        createListOfFilesToProcess(new File(inputFileName));
    }
}

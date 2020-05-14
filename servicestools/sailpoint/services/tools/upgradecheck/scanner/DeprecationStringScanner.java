package sailpoint.services.tools.upgradecheck.scanner;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import sailpoint.services.tools.upgradecheck.helper.DeprecationManager;
import sailpoint.services.tools.upgradecheck.scanner.ScannerParams.ScannerParamsEnum;
import sailpoint.services.tools.upgradecheck.scanner.fileScan.FileToProcess;
import sailpoint.services.tools.upgradecheck.scanner.fileScan.SPObject;
import sailpoint.services.tools.upgradecheck.scanner.fileScan.ScriptBlock;
import sailpoint.services.tools.upgradecheck.util.FileUtils;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by surya.nellepalli on 27/09/2017.
 * <p>
 * This class does a simple String search for deprecations.
 * This is useful as in custom rules/workflows we might not be able to compute
 * class type of args
 */
public class DeprecationStringScanner extends DeprecationScanner {

    static final Logger log = Logger.getLogger(DeprecationStringScanner.class);
    static final String RPT_NAME = "RegexDeprecationMatch.log";
    public static final String _version = "Regex Deprecation Scanner V1.5";

    // refactor code from BSHDeprecationScanner and build parent class

    //  build file list
    // check for imports and for them build deprecation list
    // do string match and build report


    public DeprecationStringScanner(ScannerParams sparams) {
        super(sparams);
    }


    public void runDeprecationScanner() throws ParserConfigurationException, SAXException, IOException {

        loadHelpers();

        StringBuffer reportSB = new StringBuffer();

        reportSB.append(_version).append("\n");
        reportSB.append("*****************************\n");

        Date now = new Date();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("E, y-M-d 'at' h:m:s a z");


        reportSB.append("Report Date:").append(dateFormatter.format(now)).append("\n");

        reportSB.append("Please note this is a string based check and can contain false positives\n");
        reportSB.append("------------------------------------------------------------------------\n");


        // build list of global deprecations

        String pkgsToScan = getScannerParams().getStringParam(ScannerParamsEnum.ARG_PKGS_TO_SCAN);
        String pkgArray[] = pkgsToScan.split(",");
        for (String pkgToScan : pkgArray) {
            if (log.isDebugEnabled()) {
                log.debug("Building deprecations for " + pkgToScan);
            }
            DeprecationManager.scanAndAddDeprecation(pkgToScan);
        }

        //get list of files to process

        // Compute list of files to process
        createListOfFilesToProcess(new File(getScannerParams().getStringParam(ScannerParamsEnum.ARG_FOLDER_TO_SCAN)));


        // For each File extract XML, Check What SP Objects are available and for each extract Script block

        HashMap<Pattern, HashSet<String>> regexMap = DeprecationManager.getRegexMap();
        Set<Pattern> allRegex = regexMap.keySet();
        for (File rawFile : getListOfFilesToProcess()) {
            FileToProcess fileToProcess = buildFileToProcess(rawFile);
            if (fileToProcess == null)
                continue;
            if (log.isDebugEnabled()) {
                log.debug("checking File " + fileToProcess.getFileName());
            }
            for (SPObject spObject : fileToProcess.getSpObjects()) {
                if (log.isDebugEnabled()) {
                    log.debug(" SPObject " + spObject.getObjectType() + " " + spObject.getObjectName());
                }
                for (ScriptBlock scriptBlock : spObject.getScriptBlocks()) {

                    if (log.isDebugEnabled()) {
                        log.debug("Has script blocks");
                    }

                    String beanshellScript = scriptBlock.getBeanShellScript();
                    if (beanshellScript.indexOf("<![CDATA[") >= 0) {
                        beanshellScript = beanshellScript.substring(beanshellScript.indexOf("<![CDATA[") + "<![CDATA[".length(), beanshellScript.indexOf("]]>"));

                    }
                    log.trace("beany scriptt" + beanshellScript);
                    for (Pattern p : allRegex) {
                        if (log.isTraceEnabled()) {
                            log.trace("checking for " + p.toString());
                        }
                        Matcher matcher = p.matcher(beanshellScript);

                        if (matcher.matches()) {
                            reportSB.append(fileToProcess.getFileName()).append("\n").
                                    append("\t\t").append(spObject.getObjectName()).
                                    append(spObject.getObjectType()).append("\n");
                            reportSB.append("Deprecated method(s) invoked: \t \n");
                            reportSB.append(regexMap.get(p)).append("\n");


                            if (log.isDebugEnabled()) {
                                log.debug("Deprecated method being invoked " + regexMap.get(p));
                            }
                        }

                    }

                }

            }
        }

        String rpt_FileName = getScannerParams().getStringParam(ScannerParamsEnum.ARG_RPT_LOCATION);
        rpt_FileName = rpt_FileName + File.separator + RPT_NAME;
        FileUtils.WriteToFile(rpt_FileName, reportSB.toString(), false);

    }

    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException, ClassNotFoundException {


        ScannerParams params = ScannerParams.getParamsFromFile(FileUtils.getResourcePath("deprecationcheck.properties"));
        /*params.addParam(ScannerParams.ScannerParamsEnum.ARG_FOLDER_TO_SCAN, "/Users/surya.nellepalli/surya/SP/clients/sailpoint/SSD/Upgrade/BeanShellDeprecation/config/Rule/Ruler");
        params.addParam(ScannerParams.ScannerParamsEnum.ARG_DTD_LOCATION, "/Users/surya.nellepalli/surya/SP/clients/sailpoint/SSD/Upgrade/BeanShellDeprecation/config/sailpoint.dtd");
        params.addParam(ScannerParams.ScannerParamsEnum.ARG_HELPER_LIST, "RuleHelper,WorkflowHelper");
        params.addParam(ScannerParams.ScannerParamsEnum.ARG_PKGS_TO_SCAN, "sailpoint.object.,sailpoint.api.,sailpoint.server.,sailpoint.util.");
        */


        DeprecationStringScanner dss = new DeprecationStringScanner(params);
        dss.runDeprecationScanner();

       /*
       Map<Class,List<Method>>  deprecatedMethods=DeprecationManager.getAllDeprecation();
       for( Class cl: deprecatedMethods.keySet()){
           System.out.println(cl.getName());
          for(Method method: deprecatedMethods.get(cl)){
              String regex=getRegex(method);
              System.out.println(regex);


          }
       }*/


    }

    public static void main2(String args[]) {

        StringBuffer javaCode = new StringBuffer();
        javaCode.append("import sailpoint.object.*;\n");
        javaCode.append("import sailpoint.api.ManagedAttributer;\n");
        javaCode.append("\t\n");
        javaCode.append("\t\n");
        javaCode.append("String name=\"MA_APP_Value1\";\n");
        javaCode.append("ManagedAttribute ma=ManagedAttributer.get(context  ,\n  name,\n false, \"MA_APP_Value1\" ,\n val );\n");
        javaCode.append("System.out.println(ma.toXml();\n");


        String javaCodeStr = javaCode.toString();
        System.out.println(javaCodeStr);

        String regex = "\\A.*\\.get\\s*\\(.*,.*,.*,.*,.*\\).*\\z";

        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);

        if (pattern.matcher(javaCodeStr).matches())
            System.out.println("Deprecated method  called ");

        else
            System.out.println(regex + " not found");


    }


}

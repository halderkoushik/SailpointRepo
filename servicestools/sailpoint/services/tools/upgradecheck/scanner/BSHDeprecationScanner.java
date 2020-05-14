package sailpoint.services.tools.upgradecheck.scanner;

import bsh.ParseException;
import bsh.SailPointBSHDeprecationParser;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import sailpoint.services.tools.upgradecheck.scanner.fileScan.FileToProcess;
import sailpoint.services.tools.upgradecheck.scanner.fileScan.SPObject;
import sailpoint.services.tools.upgradecheck.scanner.fileScan.ScriptBlock;
import sailpoint.services.tools.upgradecheck.util.FileUtils;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Created by surya.nellepalli on 28/09/2017.
 */
public class BSHDeprecationScanner extends DeprecationScanner {

    static final Logger log = Logger.getLogger(BSHDeprecationScanner.class);
    static final String RPT_NAME = "BSHDeprecationCheck.log";

    public static final String _version = "BSH Deprecation Scanner V1.8";


    public BSHDeprecationScanner(ScannerParams scannerParams) {
        super(scannerParams);
    }

    public void runDeprecationScanner() throws IOException {

        //System.out.println("running BSHDeprecationScanner ");

        loadHelpers();

        // Compute list of files to process
        createListOfFilesToProcess(new File(_scannerParams.getStringParam(ScannerParams.ScannerParamsEnum.ARG_FOLDER_TO_SCAN)));

        //System.out.println(" BSHDeprecationScanner createListOfFilesToProcess");


        // For each File extract XML, Check What SP Objects are available and for each extract Script block

        boolean append = false;

        writeHeaderToLog();
        HashSet classesNotFound=new HashSet();
        for (File rawFile : listFilesToProcess) {


            FileToProcess fileToProcess = buildFileToProcess(rawFile);
            //System.out.println("file to process "+fileToProcess.getFileName());
            // error reading
            if (fileToProcess != null && fileToProcess.getSpObjects() != null) {
                for (SPObject spObject : fileToProcess.getSpObjects()) {
                   // System.out.println("obj name=="+spObject.getObjectName()+":: obj type=="+spObject.getObjectType());
                    for (ScriptBlock scriptBlock : spObject.getScriptBlocks()) {
                        //System.out.println("processing script block");
                        String beanshellScript = scriptBlock.getBeanShellScript();
                        if (beanshellScript.indexOf("<![CDATA[") >= 0) {
                            beanshellScript = beanshellScript.substring(beanshellScript.indexOf("<![CDATA[") + "<![CDATA[".length(), beanshellScript.indexOf("]]>"));
                        }
                        beanshellScript = beanshellScript.replaceAll("&lt;", "<");
                        beanshellScript = beanshellScript.replaceAll("&amp;", "&");
                        SailPointBSHDeprecationParser bsdp = new SailPointBSHDeprecationParser(_scannerParams);
                        if (log.isDebugEnabled()) {
                            log.debug(spObject.prettyPrintVars());
                        }
                        //System.out.println(fileToProcess.getFileName());
                        List<ScannerResultRecord> result = bsdp.parseBeanShell(beanshellScript, spObject.getVars());
                       // System.out.println("got results "+result.size());
                        for (ScannerResultRecord record : result) {
                            if (log.isDebugEnabled()) {
                                log.debug(record.getDeprecatedMethods());

                            }
                            record.setObjectName(spObject.getObjectName());
                            record.setFileName(fileToProcess.getFileName());

                        }

                        writeDeprecationLog(result);
                        if(bsdp.getImportErrors()!=null && bsdp.getImportErrors().size()>0)
                        {
                            classesNotFound.addAll(bsdp.getImportErrors());
                        }


                        scriptBlock.setScanResultList(result);
                    }
                }
            }

        }

       // writeToLog("Classes not found in class path: "+classesNotFound.toString()+"\n", true);

        // show list of deprecations


    }



    public void writeToLog(String message, boolean append){
        String rpt_FileName = getScannerParams().getStringParam(ScannerParams.ScannerParamsEnum.ARG_RPT_LOCATION);
        rpt_FileName = rpt_FileName + File.separator + RPT_NAME;
        FileUtils.WriteToFile(rpt_FileName, message,append);
    }

    public void writeHeaderToLog(){


        //System.out.println("writing to " + rpt_FileName);
        StringBuffer reportSB = new StringBuffer();

            // first time invoke so place header
            reportSB.append("\n------------------------------------------------------------").append("\n");
            reportSB.append(_version).append("\n");
            Date now = new Date();

            SimpleDateFormat dateFormatter = new SimpleDateFormat("E, y-M-d 'at' h:m:s a z");
            reportSB.append("Report Date:").append(dateFormatter.format(now)).append("\n");
            reportSB.append("Scanner run against:"+getScannerParams().getStringParam(ScannerParams.ScannerParamsEnum.ARG_FOLDER_TO_SCAN));
            reportSB.append("\n------------------------------------------------------------").append("\n\n");

            writeToLog(reportSB.toString(),false);


    }

    public void writeDeprecationLog(List<ScannerResultRecord> result) {

        StringBuffer reportSB = new StringBuffer();

        for (ScannerResultRecord record : result) {
            if (record.getDeprecatedMethods() != null) {
                reportSB.append("File Name: ").append(record.getFileName()).append("\n");
                reportSB.append("SailPoint Object: ").append(record.getObjectName()).append("\n");
                reportSB.append("Line Number:").append(record.getLine()).append("\n");
                reportSB.append("Source Text:").append(record.getText()).append("\n");
                reportSB.append("Deprecations:").append("\n");
                reportSB.append(record.getDeprecatedMethods());
                reportSB.append("\n\n");
            }

        }
        writeToLog(reportSB.toString(),true);

    }

    /**
     * @param args
     * @throws ParseException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static void main(String[] args) throws ParseException, IOException, ParserConfigurationException, SAXException {


        String path = FileUtils.getResourcePath("deprecationcheck.properties");
        System.setProperty("log4j.configuration", "/Users/surya.nellepalli/surya/SP/Projects/BSHScannerSSD/out/production/BSHScannerSSD/log4j.properties" );


        System.out.println("reading properties from " + path);

        ScannerParams params = ScannerParams.getParamsFromFile(path);


        BSHDeprecationScanner bds = new BSHDeprecationScanner(params);
        bds.runDeprecationScanner();
        System.out.println("report generated at: "+params.getStringParam(ScannerParams.ScannerParamsEnum.ARG_RPT_LOCATION));

    }


}

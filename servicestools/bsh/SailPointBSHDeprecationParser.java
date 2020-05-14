package bsh;

import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeElementsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import sailpoint.services.tools.upgradecheck.helper.DeprecationManager;
import sailpoint.services.tools.upgradecheck.helper.ReflectionsHelper;
import sailpoint.services.tools.upgradecheck.scanner.ScannerParams;
import sailpoint.services.tools.upgradecheck.scanner.ScannerResultRecord;
import sailpoint.services.tools.upgradecheck.scanner.fileScan.SpBshVariable;
import sailpoint.services.tools.upgradecheck.util.ClassUtils;
import sailpoint.services.tools.upgradecheck.util.Util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.*;




/*


  @author Surya Nellepalli:
  SSDV5: Refactor code for recursive scanning of AST nodes,
  Significant improvement handling of variable declarations,scopes, loosely typed variables
   method invocations, deprecation checks and minor bug fixes
  SSDV6: Add support for invocations of non-public/non-existent method.

   @author Nihar Druva:
        SSDv5 Improvements in first version
   @author Christian Cairney :
            SSDV5First version and design to work with BSH AST classes



   This class has a dependepncy on BSH Parser which is available only under bsh package and hence this poor
   class resides under bsh package.

	 TODO explore options to use  bsh.Capabilities.setAccessibility(true) and remove dependency on bsh package

 */

public class SailPointBSHDeprecationParser {


    static final Logger log = Logger.getLogger(SailPointBSHDeprecationParser.class);
    private static final String __GLOBAL_SCOPE = "__sp_global_scope";
    private static HashSet<MethodInvokedInformation> publicMII=new HashSet<MethodInvokedInformation>();
    private static Class UNKNOWN_CLASS = UnknownClass.class;
    private String _currentScope = __GLOBAL_SCOPE;
    private long _scopeCounter=1;
    private final String __SCOPE_SEPARATOR ="::";
    private String __JAVA_LANG_PKG_NAME = "java.lang.";
    private List<MethodInvokedInformation> _currentInvocations = null;
    private Stack<MethodInvokedInformation> _currentInvocationsStack = null;
    private ScannerParams _scannerParams = null;

    private List<ScannerResultRecord> _scanResult = new ArrayList<ScannerResultRecord>();

    private List<Class> imports = new ArrayList<Class>();



    private List<String> importErrors = new ArrayList<String>();
    private Map<Integer, List<String>> comments = new HashMap<Integer, List<String>>();


    /*

        Map of all variables found together with the Type.
        This is further scoped. Scope can be null (meaning global) or by method name
     */


    private Map<String, Map<String, Class>> variableMap = new HashMap<String, Map<String, Class>>();


    // internal methods and return types
    private Map<String, String> internalMethods = new HashMap<String, String>();


    public SailPointBSHDeprecationParser(ScannerParams params) {


        _scannerParams = params;


    }

    private Class findClass(String className, List<Class> imports) {

        Class cls = ClassUtils.findClass(className, imports);

        if (cls == null && className != null) {
            List<String> defaultPackage = new ArrayList<String>();
            defaultPackage.add("java.lang.");
            defaultPackage.add("java.lang.");
            defaultPackage.add("java.util.");
            defaultPackage.add("java.net.");

            for (String pkgName : defaultPackage) {
                if (!className.startsWith(pkgName)) {

                    cls = ClassUtils.findClass(pkgName + className, imports);
                    if (cls != null) break;
                }
            }

        }
        if(cls!=null){
           DeprecationManager.addDeprecation(cls);
        }

        if (cls == null)
            return UNKNOWN_CLASS;

        return cls;

    }

    public List<ScannerResultRecord> parseBeanShell(String beanShellScript, List<SpBshVariable> originalVars) {


        List<ScannerResultRecord> scanResults = new ArrayList<ScannerResultRecord>();
        variableMap=new HashMap<String, Map<String, Class>>();

        if (originalVars != null) {
            for (SpBshVariable var : originalVars) {
                String varName = var.getVarName();

                Class cls = findClass(var.getDataType(), null);

                setVariableForScope(__GLOBAL_SCOPE, varName, cls);
            }
        }


        //addEntirePackageToImports("java.lang.");
        InputStream stream = new ByteArrayInputStream(beanShellScript.getBytes(Charset.defaultCharset()));
        Parser parser = new Parser(stream);

        int lastLineProcessed = 0;
        boolean skipLines = false;
        String _previousLine="";
        try {
            while (!(parser.Line())) {
                _currentScope = __GLOBAL_SCOPE;
                //System.out.println("scope reset to GLOBAL in parser read line");
                SimpleNode node = parser.popNode();
                if (node == null) {
                    if (log.isTraceEnabled()) {
                        log.trace("node is null.Skipping");
                    }
                    continue;
                } else if (node.getText() == null) continue;
                else if (node.getText().trim().equalsIgnoreCase("")) continue;
                if (node.getText().startsWith("/**")) {
                    skipLines = true;
                }
                if (node.getText().contains("*/")) {
                    skipLines = false;
                    log.debug("end of comments skipping this line");
                    continue;
                }

                if (skipLines) {
                    if (log.isTraceEnabled()) log.trace("skipping " + node.getLineNumber());
                    continue;
                }

                lastLineProcessed = node.getLineNumber();

                if (log.isDebugEnabled())
                    log.debug("Read: " + node.getLineNumber() + " " + node.getText() + " " + node.getClass().getName());
                _currentInvocations = new ArrayList<MethodInvokedInformation>();
                _currentInvocationsStack = new Stack<MethodInvokedInformation>();

                processNode(node);
                _previousLine=node.getText();
                // traverseNode(node);



                for (MethodInvokedInformation mii : _currentInvocations) {
                    ScannerResultRecord scannerRecord = new ScannerResultRecord();
                   // System.out.println("MII>>"+mii.toString());
                    if (findDeprecation(mii, scannerRecord)) {
                        scanResults.add(scannerRecord);
                    }
                    if( scanNonPublic(_scannerParams) && detectNonPublicInvoke(mii,scannerRecord)){

                        scanResults.add(scannerRecord);
                    }

                }


            }
        } catch (ParseException pe) {
            //if(log.isDebugEnabled())
           // pe.printStackTrace();
            if(log.isDebugEnabled()) {
                pe.printStackTrace();
                log.warn("Parsing exception "+pe.getMessage());
            }
            ScannerResultRecord scannerRecord = new ScannerResultRecord();
            scannerRecord.setLine(lastLineProcessed);
            scannerRecord.setText(_previousLine);
            List<String> deprecatedMethods = scannerRecord.getDeprecatedMethods();
            if (deprecatedMethods == null)
                deprecatedMethods = new ArrayList<String>();
            deprecatedMethods.add("ERROR: Parse Exception " + pe.getMessage());
            scannerRecord.setDeprecatedMethods(deprecatedMethods);

            scanResults.add(scannerRecord);

        }


        if (log.isDebugEnabled()) {
            log.debug("Classes imported: " + imports.toString());
            log.debug("Classes with errors on import: " + importErrors.toString());
            log.debug("Warnings: " + comments.toString());

            Set<String> scopes = variableMap.keySet();
            for (String currentScope : scopes) {
                log.debug("Variables in scope  " + currentScope + variableMap.get(currentScope));
            }
            log.debug("internal methods " + internalMethods.toString());
            log.debug("Deprecated methods: " + DeprecationManager.getAllDeprecation());
        }
        return scanResults;
    }

    private boolean findDeprecation(MethodInvokedInformation mii, ScannerResultRecord scannerRecord){
        boolean foundDeprecations = false;

        Class clsName = mii.getMemberClassName();
        if (clsName == null) {
            if (log.isDebugEnabled()) {
                log.debug("skipping as its an internal method");
            }

            return foundDeprecations;
        }

        boolean keepCheckingForDeprecations = true;
        List<Method> deprecatedMethods = null;

        HashMap<Class, List<Method>> allDeprecations = DeprecationManager.getAllDeprecation();
        Iterator<Class> allDeprecationsIterator = null;

        if (clsName.equals(UNKNOWN_CLASS) && allDeprecations != null) {
            allDeprecationsIterator = allDeprecations.keySet().iterator();
        } else {
            deprecatedMethods = DeprecationManager.getDeprecation(clsName);
        }

        if (clsName.equals(UNKNOWN_CLASS)) {
            log.debug("checking for all deprecations");
            if (allDeprecationsIterator.hasNext()) {
                Class t = allDeprecationsIterator.next();
                deprecatedMethods = DeprecationManager.getDeprecation(t);
                if (deprecatedMethods == null) return false;
            } else {
                keepCheckingForDeprecations = false;
            }
        }

        while (keepCheckingForDeprecations) {

            if (deprecatedMethods != null) //now check if we have a match
                for (Method deprecatedMethod : deprecatedMethods) {
                    String deprecatedMethodName = deprecatedMethod.getName();
                    if (deprecatedMethodName != null && deprecatedMethodName.equals(mii.getMethodName())) {
                        if (log.isDebugEnabled())
                            log.debug("Class: " + mii.getMemberClassName() + " Method:" + mii.getMethodName() + " near match ");

                        boolean isSignatureMatch = false;
                        if (mii.getNumOfArgs() == deprecatedMethod.getParameterCount()) {
                            isSignatureMatch = true;
                            for (int param = 0; param < mii.getNumOfArgs(); param++) {
                                Class methodClass = deprecatedMethod.getParameterTypes()[param];
                                Class invokedClass = mii.getArguments().get(param);
                                if (UNKNOWN_CLASS.equals(invokedClass))
                                    if (!doesClassMatchForInvoke(invokedClass, methodClass)) {
                                        isSignatureMatch = false;
                                        break;
                                    }

                            }
                            if (isSignatureMatch) {
                                log.debug("found deprecation!");
                                foundDeprecations = true;
                                List<String> deprecatedInfo = scannerRecord.getDeprecatedMethods();
                                if (deprecatedInfo == null)
                                    deprecatedInfo = new ArrayList<String>();
                                StringBuffer deprecatedInfoSB = new StringBuffer();

                                if (clsName.equals(UNKNOWN_CLASS)) {
                                    deprecatedInfo.add("possible deprecated method invoked: " + deprecatedMethod.toString());
                                } else
                                    deprecatedInfo.add(deprecatedMethod.toString() + " invoked on " + mii.getMemberClassName().getName());
                                scannerRecord.setDeprecatedMethods(deprecatedInfo);
                                scannerRecord.setLine(mii.getLine());
                                scannerRecord.setText(mii.getText());


                            }

                        }


                    }

                }
            if (!clsName.equals(UNKNOWN_CLASS)) {
                keepCheckingForDeprecations = false;
            }
            if (clsName.equals(UNKNOWN_CLASS)) {
                log.debug("checking for all deprecations");
                if (allDeprecationsIterator.hasNext()) {
                    Class t = allDeprecationsIterator.next();
                    deprecatedMethods = DeprecationManager.getDeprecation(t);
                    if (deprecatedMethods == null) continue;
                } else {
                    keepCheckingForDeprecations = false;
                }
            }
        }

        return foundDeprecations;

    }

    private boolean detectNonPublicInvoke(MethodInvokedInformation mii, ScannerResultRecord scannerRecord){


        Class clsName = mii.getMemberClassName();
        if (clsName == null) {
            if (log.isDebugEnabled()) {
                log.debug("skipping as its an internal method");
            }

            return false;
        }

        boolean keepChecking = true;
        List<Method> deprecatedMethods = null;

        HashMap<Class, List<Method>> allDeprecations = DeprecationManager.getAllDeprecation();
        Iterator<Class> allDeprecationsIterator = null;

        if (clsName.equals(UNKNOWN_CLASS))  {
            return false;
        }

        if(!publicMII.contains(mii)) {
            Method method = findPublicMethod(mii.getMemberClassName(), mii.getMethodName(), mii.getNumOfArgs(), mii.getArguments());
            if (method == null) {
                List<String> deprecatedInfo = scannerRecord.getDeprecatedMethods();
                deprecatedInfo.add(mii.toString() + " is not public method or does not exist");
                scannerRecord.setDeprecatedMethods(deprecatedInfo);
                scannerRecord.setLine(mii.getLine());
                scannerRecord.setText(mii.getText());
                return true;
            }else
            {
                publicMII.add(mii);
            }
        }
    return false;

    }






    private void processNode(SimpleNode node) {


        String blockType = null;
        ArrayList<BSHBlock> blocks = new ArrayList<BSHBlock>();
        if ((node instanceof BSHIfStatement) ||
                (node instanceof BSHWhileStatement) ||
                (node instanceof BSHForStatement) ||
                (node instanceof BSHEnhancedForStatement) ||
                (node instanceof BSHTryStatement)

                ) {
            blockType = "_" + node.getClass().getTypeName();

            getSpecificNode(node, BSHBlock.class, blocks);
        }

        if (blocks != null && blocks.size() > 0) {
            String previousScope = _currentScope;
            blockType = blockType.replaceAll("bsh.BSH", "");
            blockType = blockType.replaceAll("Statement", "");
            blockType = blockType.toUpperCase();
            _currentScope = _currentScope + __SCOPE_SEPARATOR +blockType+_scopeCounter;
            _scopeCounter++;
            //System.out.println("Scope changed to "+_currentScope+" in before block");
            if(node instanceof BSHEnhancedForStatement){
                checkVariables(node);

            }
            else if(node instanceof BSHIfStatement){

                if(node.jjtGetNumChildren()>0 && !(node.jjtGetChild(0) instanceof BSHBlock)){
                    processNode(node.getChild(0));
                }
                if(node.jjtGetNumChildren()>1 && !(node.jjtGetChild(1) instanceof BSHBlock)){

                    processNode(node.getChild(1));
                }
                if(node.jjtGetNumChildren()>2 && !(node.jjtGetChild(2) instanceof BSHBlock)){
                    processNode(node.getChild(2));
                }
            }else if(node instanceof BSHWhileStatement){
                if( ((BSHWhileStatement)node).isDoStatement)
                {
                    if(node.jjtGetNumChildren()>1)
                        processNode(node.getChild(1));
                }else{
                    if(node.jjtGetNumChildren()>0)
                    processNode(node.getChild(0));
                }

            }else if(node instanceof BSHForStatement){

                SimpleNode forInit;
                SimpleNode expression;
                SimpleNode forUpdate;
                BSHForStatement forStmt=(BSHForStatement)node;
                int i = 0;
                if(forStmt.hasForInit) {
                    forInit = ((SimpleNode) node.jjtGetChild(i++));
                    processNode(forInit);
                }
                if(forStmt.hasExpression) {
                    expression = ((SimpleNode) node.jjtGetChild(i++));
                    processNode(expression);
                }
                if(forStmt.hasForUpdate) {
                    forUpdate = ((SimpleNode) node.jjtGetChild(i++));
                    processNode(forUpdate);
                }

            }
            BSHBlock block = blocks.get(0);


            for (int i = 0; i < block.jjtGetNumChildren(); i++) {
                //System.out.println(">>"+block.getChild(i).getText());
                processNode(block.getChild(i));
            }


            //processNode(block.jjtGetChild(0));
            _currentScope = previousScope;
            //System.out.println("Scope changed to "+_currentScope+" after block");
            return;
        }

        checkMethodCreation(node);

        checkImportStatement(node);
        checkVariables(node);

        checkMethodInvokes(node);
    }

    private void traverseNode(SimpleNode node) {


        StringBuffer sb = new StringBuffer();

        sb.append("\n************START AST DEBUG ****************\n");
        sb.append(" Source Line number:").append(node.getLineNumber()).append("\n");
        sb.append(" Node Class: ").append(node.getClass().getName()).append("\t");
        sb.append(" Num children:  ").append(node.jjtGetNumChildren()).append("\n");
        sb.append(" Node Parent: ").append((node.jjtGetParent() != null ? node.jjtGetParent().getClass().getName() : " No Parent Found")).append("\n");


        Token token = node.firstToken;
        while (token != null) {
            sb.append(" Token:: ").append(token.toString()).append(" ").append(token.kind).append("\t::");
            token = token.next;

        }
        sb.append("\n");


        sb.append(" Text=").append(node.getText()).append("\n");


        sb.append("\n************END AST DEBUG ****************\n");
        log.debug(sb.toString());


        for (int childCounter = 0; childCounter < node.jjtGetNumChildren(); childCounter++) {
            traverseNode(node.getChild(childCounter));
        }


    }


    // rudimentary method to find out whats inside and build a Map of whats what.
    // most important find if there is a MethodInvocation

    private void getSpecificNode(Node node, Class cls, List result) {

        if (log.isTraceEnabled()) {
            log.trace("trying to get " + cls.getName());
        }

        if (cls.isInstance(node)) {
            result.add(node);
            return;
        }
        for (int childCounter = 0; childCounter < node.jjtGetNumChildren(); childCounter++) {
            getSpecificNode(node.jjtGetChild(childCounter), cls, result);
        }


    }

    private void getSpecificNodeFromChild(Node node, Class cls, List result) {

        if (log.isTraceEnabled()) {
            log.trace("trying to get " + cls.getName());
        }

        if (cls.isInstance(node)) {
            result.add(node);
            return;
        }
        for (int childCounter = 0; childCounter < node.jjtGetNumChildren(); childCounter++) {
            if (cls.isInstance(node)) {
                result.add(node);
                return;
            }
        }


    }

    private void checkMethodInvokes(SimpleNode node) {

        // find if method invokes are present
        ArrayList<SimpleNode> methods = new ArrayList<SimpleNode>();

        if(node instanceof BSHBlock || node instanceof  BSHMethodDeclaration){

            //System.out.println(" complex block not processing for method invoke");
            return;

        }

        getSpecificNode (node, BSHMethodInvocation.class, methods);
        if (methods.size() > 0) {

            for (SimpleNode method : methods) {
                if (log.isDebugEnabled()) {
                    log.debug("*** AST method invocation found " + method.getText());
                }
                BSHMethodInvocation currentMethodInvoke = (BSHMethodInvocation) method;

                /* check if this is an invoke from current BeanShell context like methods defined in this
                   beanshell or on a variable or a static invoke
                */

                Token firstToken = currentMethodInvoke.firstToken;
                Token secondToken = null;
                if (firstToken != null)
                    secondToken = firstToken.next;

                String variableName = null;
                String methodName = null;

                if (firstToken != null && secondToken != null) {
                    if (secondToken.toString().equals(".")) {

                        variableName = firstToken.toString();
                        methodName = secondToken.next.toString();
                    } else if (secondToken.toString().equals("(")) {
                        methodName = firstToken.toString();
                    }

                }

                //System.out.println("$$$ Variable \"" + variableName + "\" Method invoked  \"" + methodName + "\""+" Node class "+node.getClass());
                Class clsOfMethod = null;
                if (log.isDebugEnabled()) {
                    log.debug("$$$ Variable \"" + variableName + "\" Method invoked  \"" + methodName + "\"");

                }
                if (variableName != null) {
                    clsOfMethod = getVariableType(variableName);
                    if (clsOfMethod == null)
                        clsOfMethod = UNKNOWN_CLASS;
                    log.debug("class for method " + methodName + " is " + clsOfMethod);
                }


                MethodInvokedInformation mii = new MethodInvokedInformation();
                mii.setMethodName(methodName);
                mii.setNumOfArgs(0);
                mii.setMemberClassName(clsOfMethod);
                mii.setText(currentMethodInvoke.getText());
                mii.setLine(currentMethodInvoke.getLineNumber());

                int numChild = currentMethodInvoke.jjtGetNumChildren();
                BSHArguments args = null;
                for (int i = 0; i < numChild; i++) {
                    SimpleNode childOfMethod = currentMethodInvoke.getChild(i);
                    if (childOfMethod instanceof BSHArguments) {
                        args = (BSHArguments) childOfMethod;
                        break;
                    }

                }

                _currentInvocationsStack.push(mii);
                if (args != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("number of args " + args.jjtGetNumChildren());
                    }
                        mii.setNumOfArgs(args.jjtGetNumChildren());
                       // log.debug("MII "+mii.toString());

                        for (int i = 0; i < args.jjtGetNumChildren(); i++) {
                            log.debug(args.getChild(i).getClass().getName());
                            SimpleNode argNode = args.getChild(i);
                            Class argClass = UNKNOWN_CLASS;
                            if (argNode instanceof BSHLiteral)
                                argClass = checkLiteralType((BSHLiteral) argNode);
                            else if (argNode instanceof BSHCastExpression) {
                                SimpleNode checkChild = argNode.getChild(0);
                                if (checkChild != null && checkChild instanceof BSHType) {

                                    String className = checkChild.getText().trim();
                                    argClass = findClass(className, imports);
                                }
                                //TODO: We could be casting a method invoke
                            } else if (argNode instanceof BSHPrimaryExpression) {
                                SimpleNode checkChild = argNode.getChild(0);
                                log.debug("Argument " + (i + 1) + " BSHPrimaryExpression child class:" + checkChild.getClass().getName());
                                if (checkChild instanceof BSHLiteral) {
                                    argClass = checkLiteralType((BSHLiteral) checkChild);
                                } else if (checkChild instanceof BSHAmbiguousName) {
                                    String checkVarName = checkChild.firstToken.toString();
                                    argClass = getVariableType(checkVarName);
                                } else if (checkChild instanceof BSHMethodInvocation) {
                                    checkMethodInvokes(checkChild);
                                }
                            } else if (argNode instanceof BSHBinaryExpression) {
                               /* we can find type of this argument only by evaluating the expression,
                               * hence argClass is BSHAmbiguousName
                               * But probe further to check if there are any invocations within this expression
                               * ex: (1 > someClass.SomeMethod())
                               * */
                                SimpleNode lsh = (SimpleNode) argNode.jjtGetChild(0);
                                SimpleNode rhs = (SimpleNode) argNode.jjtGetChild(1);
                                checkMethodInvokes(lsh);
                                checkMethodInvokes(rhs);

                            }
                            log.debug("arg " + (i + 1) + " class is " + argClass);
                            mii.getArguments().add(argClass);

                        }

                }
                mii = _currentInvocationsStack.pop();

                _currentInvocations.add(mii);


            }
        }


    }

    private void checkMethodCreation(SimpleNode node) {

        if (node instanceof BSHMethodDeclaration) {

            String methodName = ((BSHMethodDeclaration) node).name;


            _currentScope = __GLOBAL_SCOPE+ __SCOPE_SEPARATOR + methodName;
            //System.out.println("scope changed to "+_currentScope+" in method creation");
            String type = null;

            if (log.isDebugEnabled())
                log.debug(" Method  " + methodName + " , line number" + node.getLineNumber() + " " + node.getClass().getName());

            for (int i = 0; i < node.jjtGetNumChildren(); i++) {

                SimpleNode childNodeOfMethod = node.getChild(i);

                if (log.isDebugEnabled())
                    log.debug("  Line: " + childNodeOfMethod.getLineNumber() + ", class: " + childNodeOfMethod.getClass().getName());

                if (childNodeOfMethod instanceof BSHFormalParameters) {
                    for (int paramCnt = 0; paramCnt < childNodeOfMethod.jjtGetNumChildren(); paramCnt++) {
                        Node paramNodeChild = childNodeOfMethod.jjtGetChild(paramCnt);
                        if (paramNodeChild instanceof BSHFormalParameter) {
                            try {

                                BSHFormalParameter formalParam = (BSHFormalParameter) paramNodeChild;

                                String paramName = null;
                                String paramType = null;

                                if (formalParam.jjtGetNumChildren() == 0) {
                                    paramType = "BSHAmbiguousName";
                                    paramName = formalParam.firstToken.toString();
                                } else {
                                    Token token = formalParam.firstToken;
                                    if (token != null) {
                                        paramType = token.toString();
                                        token = token.next;
                                    }
                                    if (paramType != null && token != null)
                                        paramName = token.toString();
                                }

                                if (paramName != null && paramType != null) {
                                    setVariableInCurrentScope(paramName, findClass(paramType, imports));
                                }

                            } catch (Exception ex) {
                                //  ex.printStackTrace();
                                log.warn("expected paramType and ParamName", ex);
                            }

                        }
                    }
                }

                if (childNodeOfMethod instanceof BSHReturnType) {
                    type = childNodeOfMethod.getText();
                    internalMethods.put(methodName, type);
                }

                if (childNodeOfMethod instanceof BSHBlock) {
                   // System.out.println("processing blocks.. within methods");
                    for (int i2 = 0; i2 < childNodeOfMethod.jjtGetNumChildren(); i2++) {
                        SimpleNode blockNode = childNodeOfMethod.getChild(i2);
                        //System.out.println(" Method Creation processing block of type "+blockNode.getClass());
                        processNode(blockNode);
                    }
                }
            }
        }
    }

    private void addEntirePackageToImports(String packageName) {
        try {
            List<ClassLoader> classLoadersList = new LinkedList<ClassLoader>();
            classLoadersList.add(ClasspathHelper.contextClassLoader());
            classLoadersList.add(ClasspathHelper.staticClassLoader());

            ConfigurationBuilder builder = new ConfigurationBuilder();
            //builder.addUrls(ClasspathHelper.forPackage(""));
            builder.setScanners(new SubTypesScanner(false /* don't exclude Object.class */), new ResourcesScanner(), new TypeElementsScanner())
                    .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(packageName)))
                    .setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])));

            ReflectionsHelper.registerUrlTypes();
            Reflections reflections = new Reflections(builder);

            // Surya: instead of using reflectionsHelper can set Reflections.log to null !!


            Set<String> typeSet = reflections.getStore().get("TypeElementsScanner").keySet();
            HashSet<Class<? extends Object>> classes = Sets.newHashSet(ReflectionUtils.forNames(typeSet, reflections
                    .getConfiguration().getClassLoaders()));

            for (Class subClass : classes) {
                //System.out.println("adding "+subClass.getName());
                DeprecationManager.addDeprecation(subClass);
                imports.add(subClass);
            }
        }catch(Exception ex){
            log.error("error in "+packageName, ex);
        }
    }

    /**
     * Get a list of the classes the import statements will import.
     * <p>
     * Supports wild card imports
     *
     * @param node
     */

    private void checkImportStatement(SimpleNode node) {

        if (node instanceof BSHImportDeclaration) {

                String className = "";
                Token token = node.firstToken.next;

                // Nested classes have to be addressed in the class.forName method as "$" separated and not
                // ".".  So, this method will detect a class, and it's nested classes.

                boolean nested = false;
                boolean reachedSemiColon=false;
                if (token != null) {
                    while (token != null && !reachedSemiColon) {

                        String ns = token.toString();

                        if (";".equals(ns.trim())) {
                            ns = null;
                            reachedSemiColon=true;
                        }

                        if (ns != null) {
                            ns=ns.trim();
                            if("".equals(ns)){
                                token=token.next;
                                continue;
                            }

                            if (ns.equals("*")) {

                                String nsToFetch = className.substring(0, className.length() - 1);
                                // Add a comment to inform that wildcard imports are bad
                                addComment(node.getLineNumber(), "Warning: Wild card imports are not recommended", token, node);

                                addEntirePackageToImports(nsToFetch);


                            } else if (ns.equals(".") && nested) {
                                ns = "$";
                            } else if ( !"".equals(ns) && Character.isUpperCase(ns.charAt(0)) && !nested && ClassUtils.isClass(className + ns)) {
                                nested = true;
                            }

                            if (!ns.equals("*")) {
                                className = className + ns;
                                //System.out.println("classname =="+className);
                            } else {
                                className = null;
                                break;
                            }
                        }

                        token = token.next;
                    }
                }

                if (className != null && className.length() > 0) {
                    Class<?> cls = null;
                    try {

                        cls = Class.forName(className, false, this.getClass().getClassLoader());
                        DeprecationManager.addDeprecation(cls);


                    } catch (ClassNotFoundException e) {
                        //Hmmm, cannot find the class... we should log this as an error
                        importErrors.add(className);
                    }
                    if (cls != null) imports.add(cls);
                }
            }

    }

    /**
     * Check to see if the node represents an addignment or variable declaration then
     * process the node
     *
     * @param node
     */

    private void checkVariables(SimpleNode node) {

        String type = null;
        String variableName = null;

        if (node instanceof BSHTypedVariableDeclaration && (node.parent == null || node.parent instanceof BSHBlock)) {


            Token token = node.firstToken;
            if (token != null)
                type = token.toString();
            if (token != null)
                token = token.next;
            if (type != null)
                variableName = token.toString();
            if ("<".equals(variableName)) {
                variableName = null;
                List<BSHVariableDeclarator> variableDeclarators = new ArrayList<BSHVariableDeclarator>();
                getSpecificNode(node, BSHVariableDeclarator.class, variableDeclarators);
                if (variableDeclarators.size() > 0)
                    variableName = variableDeclarators.get(0).firstToken.toString();
            }

        } else if (node instanceof BSHAssignment && (node.parent == null || node.parent instanceof BSHBlock)) {

            List<BSHAllocationExpression> allocs = new ArrayList<BSHAllocationExpression>();
            getSpecificNode(node, BSHAllocationExpression.class, allocs);
            if (allocs.size() > 0) {
                log.debug("got allocation");
                BSHAllocationExpression alloc = allocs.get(0);
                type = alloc.getChild(0).getText();
                if (type != null) {
                    type = type.trim();
                }

            }

            if (node.firstToken != null)
                variableName = node.firstToken.toString();


            /*
             TODO
            * if right side contains only method invokes and we can determine the method return type we might be able to
            * computue this variables type
            */
        }else if(node instanceof BSHEnhancedForStatement){
            BSHEnhancedForStatement enhFor=(BSHEnhancedForStatement)node;
            if(enhFor.jjtGetNumChildren()>0 && enhFor.jjtGetChild(0) instanceof BSHType){
                Token first=((BSHType) enhFor.jjtGetChild(0)).firstToken;
                if(first.next!=null){
                    type=first.toString();
                    variableName=first.next.toString();
                }
            }
        }


        if (type == null) {
            type = "BSHAmbiguousName";
        }

        if (variableName != null) {
            if("BSHAmbiguousName".equals(type)){
                //find if this was defined earlier
                Class cls=getVariableType(variableName);
                if(cls!=null){
                    return;
                }
            }
            setVariableInCurrentScope(variableName, findClass(type, getImports()));

        }

        //node.dump("  Dump:" );
        if (log.isDebugEnabled()) log.debug("  Type: " + type + ", variable name: " + variableName+ " in scope "+_currentScope);


    }

    /**
     * Variables are scoped to class or method,get the scoped variables
     * here.
     *
     * @param scopeName
     * @return
     */

    private Map<String, Class> getVariablesForScope(String scopeName) {

        Map<String, Class> variables = variableMap.get(scopeName);
        if (variables == null) {
            variables = new HashMap<String, Class>();
            variableMap.put(scopeName, variables);
        }
        return variables;

    }

    private Class getVariableType(String varName) {

        String scope=_currentScope;

      if(log.isDebugEnabled()){
          log.debug("finding var type for "+varName + " in scope "+scope);
      }
        Class cls =null;


        while(scope!=null  && cls==null){
            if(log.isDebugEnabled()){
                log.debug(" checking for vars in scope "+scope);
            }
            Map<String, Class> map = getVariablesForScope(scope);
            /*if("item".equals(varName)){
                System.out.println(scope);
                System.out.println(map);
            }*/
            if(map!=null)
                cls = map.get(varName);
            if(__GLOBAL_SCOPE.equals(scope))
                break;
            int lastIdx=scope.lastIndexOf(__SCOPE_SEPARATOR);
            if(lastIdx>-1)
                scope=scope.substring(0,lastIdx);

        }


        // we might be working with Static method, check if this is a class Name
        if (cls == null) {
            cls = findClass(varName, imports);
        }
        return cls;
    }

    private Map<String, Class> getVariablesForCurrentScope() {
        return getVariablesForScope(_currentScope);
    }

    private void setVariableInCurrentScope(String varName, Class cls) {
        if(log.isTraceEnabled()){
            log.trace(" setting var "+varName + " as class "+cls+ " in scope "+_currentScope);
        }
        setVariableForScope(_currentScope, varName, cls);

    }

    private void setVariableForScope(String scopeName, String varName, Class cls) {
        Map<String, Class> map = getVariablesForScope(scopeName);
        if (map == null) {
            map = new HashMap<String, Class>();
            variableMap.put(scopeName, map);
        }
        map.put(varName, cls);

    }

    /* TODO Might be a better way to do this */
    private Class checkLiteralType(BSHLiteral literal) {
        if (log.isDebugEnabled()) {
            log.debug("checking for literal:" + ((literal != null) ? literal.getText() : "null"));
        }
        if (literal.getText() == null)
            return null;
        String literalText = literal.getText().trim();
        if (literalText.startsWith("\"") && literalText.endsWith("\"")) {
            return String.class;
        }
        if (literalText.startsWith("'") && literalText.endsWith("'"))
            return char.class;
        if ("true".equals(literalText) || "false".equals(literalText))
            return boolean.class;

        char[] iHaveLotOfCharacter = literal.getText().toCharArray();
        if (Character.isDigit(iHaveLotOfCharacter[0]))
            return int.class;

        return BSHAmbiguousName.class;

    }

    /**
     * Just add a comment to the comments map, with the line number
     * so it can be use in output to the end user
     *
     * @param lineNumber
     * @param message
     * @param token
     * @param node
     */

    private void addComment(Integer lineNumber, String message, Token token, SimpleNode node) {


        List<String> lineComments = comments.get(lineNumber);
        if (lineComments == null) {
            lineComments = new ArrayList<String>();
            comments.put(lineNumber, lineComments);
        }
        lineComments.add(message + " at column: " + String.valueOf(token.beginColumn) + " with node value: " + node.getText());
    }

    public List<Class> getImports() {
        return imports;
    }

    public List<String> getImportErrors() {
        return importErrors;
    }

    public Map<Integer, List<String>> getComments() {
        return comments;
    }

    public Map<String, Map<String, Class>> getVariableMap() {
        return variableMap;
    }

    private String readFile(File file, Charset encoding) throws IOException {

        Scanner scanner = new Scanner(file, encoding.toString());
        String text = scanner.useDelimiter("\\A").next();
        scanner.close();
        return text;
    }

    public void addVariable(Object object, String name) {

        Map variables = getVariablesForScope(null);
        variables.put(name, object.getClass());
    }

    /**
     * Get all the deprectaed methods of the current beanshell variables
     *
     * @return
     */
    public void buildDeprecatedMethods() {

        Map<Class, List<Method>> deprecatedMethods = new HashMap<Class, List<Method>>();

        for (String key : variableMap.keySet()) {
            if (log.isDebugEnabled()) log.debug("Getting variables for " + (key == null ? "Main script body" : key));

            Map<String, Class> vars = variableMap.get(key);
            for (String var : vars.keySet()) {

                Class clazz = vars.get(var);
                DeprecationManager.addDeprecation(clazz);
            }
        }


    }

    class UnknownClass {

    }

    class MethodInvokedInformation {
        Class memberClassName; // this method is member of this class
        String methodName;
        int numOfArgs;
        ArrayList<Class> arguments = new ArrayList<Class>();
        Class returnType; //return type of this method
        int argBeingProcessed;//TODO might not require this

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        String text;

        public int getLine() {
            return line;
        }

        public void setLine(int line) {
            this.line = line;
        }

        int line;

        public Class getMemberClassName() {
            return memberClassName;
        }

        public void setMemberClassName(Class memberClassName) {
            this.memberClassName = memberClassName;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public ArrayList<Class> getArguments() {
            return arguments;
        }

        public void setArguments(ArrayList<Class> arguments) {
            this.arguments = arguments;
        }

        public Class getReturnType() {
            return returnType;
        }

        public void setReturnType(Class returnType) {
            this.returnType = returnType;
        }

        public int getArgBeingProcessed() {
            return argBeingProcessed;
        }

        public void setArgBeingProcessed(int argBeingProcessed) {
            this.argBeingProcessed = argBeingProcessed;
        }

        public int getNumOfArgs() {
            return numOfArgs;
        }

        public void setNumOfArgs(int numOfArgs) {
            this.numOfArgs = numOfArgs;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(getMemberClassName() + "").append(".").append(getMethodName() + "");
            sb.append("(");
            for (int i = 0; i < getNumOfArgs(); i++) {
                sb.append(getArguments().get(i).getName() + "").append(",");
            }
            sb.append(")");
            return sb.toString();
        }

        public int hashcode(){
            int hash=7;
            hash=hash *31 + (memberClassName!=null ? 0:  memberClassName.hashCode());
            hash= hash *31 + (methodName!=null ? 0 : methodName.hashCode());
            hash=hash *31 + numOfArgs;
            hash=hash*31 ;
            if(arguments!=null){
                for(Class xx : arguments){
                    if(xx!=null)
                        hash=hash+xx.hashCode();
                }
            }
            hash=hash*31;
            if(returnType!=null)
            {
                hash=hash+(returnType.hashCode());
            }
            return hash;
        }

        public boolean equals(Object obj){
            if (this == obj) return true;
            if (obj == null) return false;
            if (this.getClass() != obj.getClass()) return false;
            MethodInvokedInformation miiOther=(MethodInvokedInformation)obj;
            if((! Util.nullSafeEq(getMethodName(),miiOther.getMethodName())) || (!Util.nullSafeObjectEq(getMemberClassName(),miiOther.getMemberClassName()))  )
                return false;
            if(getNumOfArgs()!=miiOther.getNumOfArgs())
                return false;
            boolean matched=true;
            if(getArguments()!=null && miiOther.getArguments()!=null && ( getArguments().size()==miiOther.getArguments().size())){
                return true;
            }

            return false;

        }

    }

    private static boolean scanNonPublic(ScannerParams scannerParams){

        String scanNonPublic=scannerParams.getStringParam(ScannerParams.ScannerParamsEnum.ARG_SCAN_NON_PUBLIC);
        if(scanNonPublic!=null && "true".equalsIgnoreCase(scanNonPublic))
            return true;
        return false;
    }

    public Method findPublicMethod(Class clsName,String methodName,int numArgs,ArrayList<Class> arguments){



        if(clsName!=null){
            ArrayList<Method> methods=new ArrayList<Method>();
            ClassUtils.findMethodInHierarchy(clsName,methodName,methods);
            ClassUtils.findMethodInHierarchy(Object.class,methodName,methods);
            if(methods!=null ){
                for(Method meth: methods){

                    if(Modifier.isPublic(meth.getModifiers())) {

                        if (meth.getParameterCount() != numArgs) {
                           // System.out.println(methodName+" no of args dont match");
                            continue;
                        }

                        Class[] methTypes = meth.getParameterTypes();
                        boolean matched = true;
                        for (int cnt = 0; cnt < numArgs; cnt++) {
                            if (!doesClassMatchForInvoke(arguments.get(cnt),methTypes[cnt] )) {


                               log.debug("arg type does not match "+methTypes[cnt]+"::"+arguments.get(cnt));
                                matched = false;
                                break;

                            }
                            matched = true;
                        }
                        if (matched) {
                            return meth;
                        }
                    }
                }
            }
        }

        return null;

    }


    boolean doesClassMatchForInvoke(Class clsOne, Class clsInvoke) {
        if (clsOne == null || clsInvoke == null)
            return false;
        if (clsOne.equals(UNKNOWN_CLASS))
            return true;
        if(clsOne.equals(Object.class))
            return true;
        if (clsOne.equals(clsInvoke))
            return true;
        if (ClassUtils.canAutoBox(clsOne, clsInvoke))
            return true;
        if (ClassUtils.canUnBox(clsOne, clsInvoke))
            return true;
        if (ClassUtils.canWiden(clsOne, clsInvoke))
            return true;

        return false;

    }


}

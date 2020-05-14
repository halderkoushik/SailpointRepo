package sailpoint.services.tools.upgradecheck.helper;

import org.apache.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import sailpoint.services.tools.upgradecheck.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by surya.nellepalli on 27/09/2017.
 */
public class DeprecationManager {

    static final Logger log = Logger.getLogger(DeprecationManager.class);

    private static HashMap<Class, List<Method>> _deprecations = new HashMap<Class, List<Method>>();
    private static HashSet<String> _classesChecked = new HashSet<String>();
    private static HashSet<String> _deprecatedMethodNames = new HashSet<String>();
    private static HashMap<Pattern, HashSet<String>> regexMap = new HashMap<Pattern, HashSet<String>>();

    public static HashMap<Pattern, HashSet<String>> getRegexMap() {
        return regexMap;
    }

    public static void addDeprecation(String clsName) throws ClassNotFoundException {

        Class clazz = Class.forName(clsName, false, DeprecationManager.class.getClassLoader());
        addDeprecation(clazz);


    }


    public static void addDeprecation(Class clazz) {
        if (clazz == null)
            return;

        if (checkedForDeprecation(clazz.getName()))
            return;
        _classesChecked.add(clazz.getName());

        try {


            if (log.isDebugEnabled()) log.debug("Checking for deprecated methods in class : " + clazz.getName());

            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(Deprecated.class)) {

                    List<Method> methodList = _deprecations.get(clazz);
                    if (methodList == null) {
                        methodList = new ArrayList<Method>();
                        _deprecations.put(clazz, methodList);
                    }
                    if (!methodList.contains(method)) methodList.add(method);
                }
            }
            List<Method> deprecatedMethods = _deprecations.get(clazz);
            if (deprecatedMethods != null) {
                for (Method m : deprecatedMethods) {
                    _deprecatedMethodNames.add(m.getName());
                    String regex = getRegex(m);
                    Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
                    HashSet<String> currentDepForThisRegex = regexMap.get(pattern);
                    if (currentDepForThisRegex == null) {
                        currentDepForThisRegex = new HashSet<String>();
                    }
                    currentDepForThisRegex.add("Class: " + clazz.getName() + " Method: " + m.toString());
                    regexMap.put(pattern, currentDepForThisRegex);

                }
            }
        } catch (Exception ex) {
            log.warn("Error checking " + clazz.getName() + " >>" + ex.getMessage());
        }


    }


    public static boolean isDeprecatedMethod(String methodName) {

        return _deprecatedMethodNames.contains(methodName);
    }

    public static String getRegex(Method m) {

        // System.out.println(m.toString());
        m.getModifiers();
        StringBuffer regex = new StringBuffer();
        m.getName();
        regex.append("\\A.*\\.");

        regex.append(m.getName());
        regex.append("\\s*\\(");

        regex.append(".*");
        if (m.getParameterCount() > 0) {
            for (int i = 1; i < m.getParameterCount(); i++) {
                regex.append(",.*");
            }

        }

        regex.append("\\).*\\z");


        return regex.toString();
    }


    public static boolean checkedForDeprecation(String className) {
        if (className == null)
            return true;
        return _classesChecked.contains(className);
    }

    public static List<Method> getDeprecation(Class cls) {
        return _deprecations.get(cls);
    }

    public static HashMap<Class, List<Method>> getAllDeprecation() {
        return _deprecations;
    }

    public static void scanAndAddDeprecation(String _pkg) {

        ConfigurationBuilder builder = new ConfigurationBuilder();

        List<ClassLoader> classLoadersList = new LinkedList<ClassLoader>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());


        builder.setScanners(new SubTypesScanner(false), new ResourcesScanner())
                .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(_pkg)))
                .setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
                .addUrls(ClasspathHelper.forJavaClassPath()).addUrls(ClasspathHelper.forManifest());

        ReflectionsHelper.registerUrlTypes();
        Reflections reflections = new Reflections(builder);

        // instead of using reflectionsHelper can set Reflections.log to null !!


        Set<Class<? extends Object>> subClasses = reflections.getSubTypesOf(Object.class);

        for (Class s : subClasses) {
            addDeprecation(s);
        }


    }


    //rebuild deprecations
    public void tearDown() {
        _deprecations = new HashMap<Class, List<Method>>();

    }



}

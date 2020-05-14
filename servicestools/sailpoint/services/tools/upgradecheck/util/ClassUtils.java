package sailpoint.services.tools.upgradecheck.util;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by surya.nellepalli on 28/09/2017.
 */
public class ClassUtils {

    private static Map<String, Class> primitivesMap = new HashMap<String, Class>();
    private static Map<Class, Class> autoBoxMap = new HashMap<Class, Class>();
    private static Map<Class, Class> autoUnBoxMap = new HashMap<Class, Class>();
    private static Map<Class, HashSet> wideningMap = new HashMap<Class, HashSet>();

    static {
// BSH supports auto boxing


        primitivesMap.put("byte", byte.class);
        primitivesMap.put("short", short.class);
        primitivesMap.put("int", int.class);
        primitivesMap.put("long", long.class);
        primitivesMap.put("float", float.class);
        primitivesMap.put("double", double.class);
        primitivesMap.put("boolean", boolean.class);
        primitivesMap.put("char", char.class);


        autoBoxMap.put(byte.class, Byte.class);
        autoBoxMap.put(short.class, Short.class);
        autoBoxMap.put(int.class, Integer.class);
        autoBoxMap.put(long.class, Long.class);
        autoBoxMap.put(float.class, Float.class);
        autoBoxMap.put(double.class, Double.class);
        autoBoxMap.put(boolean.class, Boolean.class);
        autoBoxMap.put(char.class, Character.class);


        autoUnBoxMap.put(Byte.class, byte.class);
        autoUnBoxMap.put(Short.class, short.class);
        autoUnBoxMap.put(Integer.class, int.class);
        autoUnBoxMap.put(Long.class, long.class);
        autoUnBoxMap.put(Float.class, float.class);
        autoUnBoxMap.put(Double.class, double.class);
        autoUnBoxMap.put(Boolean.class, boolean.class);
        autoUnBoxMap.put(Character.class, char.class);

        HashSet byteWideningSet = new HashSet();
        byteWideningSet.add(short.class);
        byteWideningSet.add(char.class);
        byteWideningSet.add(int.class);
        byteWideningSet.add(long.class);
        byteWideningSet.add(float.class);
        byteWideningSet.add(double.class);
        wideningMap.put(byte.class, byteWideningSet);


        HashSet shortWideningSet = new HashSet();
        shortWideningSet.add(int.class);
        shortWideningSet.add(long.class);
        shortWideningSet.add(float.class);
        shortWideningSet.add(double.class);
        wideningMap.put(short.class, shortWideningSet);


        HashSet charWideningSet = new HashSet();
        charWideningSet.add(int.class);
        charWideningSet.add(long.class);
        charWideningSet.add(float.class);
        charWideningSet.add(double.class);
        wideningMap.put(char.class, charWideningSet);


        HashSet intWideningSet = new HashSet();
        intWideningSet.add(long.class);
        intWideningSet.add(float.class);
        intWideningSet.add(double.class);
        wideningMap.put(int.class, intWideningSet);


        HashSet longWideningSet = new HashSet();
        charWideningSet.add(float.class);
        charWideningSet.add(double.class);
        wideningMap.put(long.class, longWideningSet);

        HashSet floatWideningSet = new HashSet();
        charWideningSet.add(double.class);
        wideningMap.put(float.class, floatWideningSet);

        // TODO Reference types can be unboxed too
        // TODO check set approach for autoboxing , unbox and widening(combining all into one)


    }

    // can autoBox clsOne to clsTwo
    public static boolean canAutoBox(Class clsOne, Class clsTwo) {
        if (clsOne == null || clsTwo == null)
            return false;
        if (clsTwo.isAssignableFrom(clsOne))
            return true;
        if (clsTwo.equals(Object.class))
            return true;
        Class chk = autoBoxMap.get(clsOne);
        if (chk != null && chk.equals(clsTwo))
            return true;
        return false;
    }

    // can autoUnBox clsOne to clsTwo
    public static boolean canUnBox(Class clsOne, Class clsTwo) {
        if (clsOne == null || clsTwo == null)
            return false;
        Class chk = autoUnBoxMap.get(clsOne);
        if (chk != null && chk.equals(clsTwo))
            return true;
        return false;
    }

    public static boolean canWiden(Class clsOne, Class clsTwo) {
        if (clsOne == null || clsTwo == null)
            return false;
        HashSet chk = wideningMap.get(clsOne);
        if (chk != null && chk.contains(clsTwo))
            return true;
        return false;
    }

    public static boolean isPrimitive(String name) {
        return primitivesMap.get(name) != null;
    }

    /**
     * Check to see if a string parameter can be a class.
     *
     * @param className
     * @return
     */

    public static boolean isClass(String className) {

        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            // e.printStackTrace();
            return false;
        }
    }

    /**
     * find the class from the class name, it could be a simple name or complex.
     *
     * @param className
     * @return
     */

    public static Class findClass(String className, List<Class> imports) {

        if (className == null) return null;


        if (isPrimitive(className))
            return primitivesMap.get(className);

        Class check = null;
        try {
            check = Class.forName(className);
        } catch (ClassNotFoundException e) {
            // Could not find the class, move along
        }

        if (check != null) return check;

        if (imports != null) {
            for (Class clazz : imports) {

                if (clazz.getName().equals(className) || clazz.getSimpleName().equals(className)) return clazz;
            }
        }
        return null;
    }

    public static void findMethodInHierarchy(Class clazz, String methodName,ArrayList<Method> methods){

        if(clazz==null)
            return;
        if(methodName==null)
            return;
        Method[] methodArr=clazz.getDeclaredMethods();
        for(Method meth: methodArr){
            if(meth.getName().equals(methodName))
                methods.add(meth);
        }

        findMethodInHierarchy(clazz.getSuperclass(),methodName,methods);
        Class[] interfaces=clazz.getInterfaces();
        for(Class interfarce: interfaces){

            findMethodInHierarchy(interfarce,methodName,methods);
        }


    }


}

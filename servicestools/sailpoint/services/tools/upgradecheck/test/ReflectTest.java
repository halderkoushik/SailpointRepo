package sailpoint.services.tools.upgradecheck.test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Created by surya.nellepalli on 18/10/2017.
 */
public class ReflectTest {
    public String method1(String arg1, int arg2, boolean arg3,Boolean arg4){
        return null;
    }

    public static void main1(String args[]){

        Method[] allMethods= ReflectTest.class.getDeclaredMethods();

        StringBuffer sb=new StringBuffer();
        for(Method m : allMethods){

            sb.append(m.getName()).append("\t").append(m.getParameterCount());
            sb.append("\n");
            Parameter[] parr=m.getParameters();
            for(Parameter p: parr){
                sb.append(p.getType()).append("\t");
            }
            sb.append("\n");
        }
        Integer i=new Integer(10);
        i.intValue();

        System.out.println(sb.toString());



    }

    public static void main(String[] args) {
        String s="\"surya\"";
        System.out.println(s);
        if(s.startsWith("\"") && s.endsWith("\""))
            System.out.println("this is string");
    }
}

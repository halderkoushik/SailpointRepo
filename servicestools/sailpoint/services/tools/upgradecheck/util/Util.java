package sailpoint.services.tools.upgradecheck.util;

/**
 * Cloned by surya.nellepalli on 07/11/2017.
 */
public class Util {

    static public int atoi(String a) {

        int i = 0;
        if (a != null && a.length() > 0) {
            try {
                int dotIndex = a.indexOf('.');
                if (dotIndex > 0)
                    a = a.substring(0, dotIndex);

                i = Integer.parseInt(a);
            } catch (NumberFormatException e) {
                // ignore, return 0
            }
        }

        return i;
    }
    public static boolean nullSafeEq(String o1, String o2) {
        if (null == o1 && null == o2)
            return true;

        return (null != o1) ? o1.equals(o2) : false;
    }
    public static boolean nullSafeObjectEq(Object o1, Object o2) {
        if (null == o1 && null == o2)
            return true;

        return (null != o1) ? o1.equals(o2) : false;
    }
}

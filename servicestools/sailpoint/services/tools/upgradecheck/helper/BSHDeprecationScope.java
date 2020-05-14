package sailpoint.services.tools.upgradecheck.helper;

import java.util.HashMap;
import java.util.Stack;

/**
 * Created by surya.nellepalli on 22/09/2017.
 */
public class BSHDeprecationScope {

    Stack<HashMap<String, Class>> scopedType = new Stack();
    HashMap<String, Class> currentTypes = new HashMap<String, Class>();
    HashMap<String, Class> globalTypes = new HashMap<String, Class>();

    public HashMap<String, Class> getCurrentScopeTypes() {
        return currentTypes;
    }

    public void setCurrentScopeTypes() {
        scopedType.push(getCurrentScopeTypes());
        currentTypes = new HashMap<String, Class>();
    }

    public HashMap<String, Class> pop() {
        return scopedType.pop();
    }

    public void addToCurrentScopeTypes(HashMap<String, Class> currentTypes) {
        this.currentTypes = currentTypes;
    }


    public HashMap<String, Class> getGlobalScopeTypes() {
        return globalTypes;
    }

    public void setGlobalScopeTypes(HashMap<String, Class> globalTypes) {
        this.globalTypes = globalTypes;
    }

    public void addToGlobalScopeTypes(String varName, Class cls) {
        globalTypes.put(varName, cls);
    }


}

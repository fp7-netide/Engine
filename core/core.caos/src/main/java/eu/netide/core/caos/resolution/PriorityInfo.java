package eu.netide.core.caos.resolution;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.caos.composition.ModuleCall;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Created by timvi on 24.08.2015.
 */
public class PriorityInfo {

    private int defaultPriority;
    private Dictionary<Integer, Integer> priorities = new Hashtable<>();

    public PriorityInfo() {
        this(0);
    }

    public PriorityInfo(int defaultPriority) {
        this.defaultPriority = defaultPriority;
    }

    public void addInfo(Integer moduleId, int priority) {
        priorities.put(moduleId, priority);
    }

    public int getPriority(Integer moduleId) {
        return priorities.get(moduleId) == null ? this.defaultPriority : priorities.get(moduleId);
    }

    public static PriorityInfo fromModuleCalls(Iterable<ModuleCall> moduleCalls, IBackendManager backendManager) {
        PriorityInfo pi = new PriorityInfo(0);
        for (ModuleCall mc : moduleCalls) {
            pi.addInfo(backendManager.getModuleId(mc.getModule().getId()), mc.getPriority());
        }
        return pi;
    }
}

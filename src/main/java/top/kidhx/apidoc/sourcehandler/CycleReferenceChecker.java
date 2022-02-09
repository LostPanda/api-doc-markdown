package top.kidhx.apidoc.sourcehandler;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author HX
 * @date 2022/1/31
 */
public class CycleReferenceChecker {

    public final Map<String, Set<String>> dependenciesMap;
    private final Set<Class<?>> scannedClasses;

    CycleReferenceChecker() {
        scannedClasses = new HashSet<>();
        dependenciesMap = Maps.newHashMap();
    }

    boolean isCycled(Class<?> owner, Class<?> dependency) throws ClassNotFoundException {
        if (dependency.getName().equalsIgnoreCase(owner.getName())) {
            return true;
        }
        addDependencies(owner, dependency);
        if (!scannedClasses.contains(owner)) {
            return false;
        }
        return isReferenced(owner, dependency, Sets.newHashSet());
    }

    private boolean isReferenced(Class<?> owner, Class<?> dependency, Set<String> checkedClassSet) throws ClassNotFoundException {
        Set<String> dependencies = dependenciesMap.get(dependency.getName());

        if (CollectionUtils.isEmpty(dependencies)) {
            checkedClassSet.add(dependency.getName());
            return false;
        }
        if (dependencies.contains(owner.getName()) || owner == dependency) {
            return true;
        }
        if (checkedClassSet.contains(dependency.getName())) {
            return false;
        }

        checkedClassSet.add(dependency.getName());
        for (String historyDependency : dependencies) {
            if (isReferenced(owner, Class.forName(historyDependency), checkedClassSet)) {
                return true;
            }
        }
        return false;
    }

    private void addDependencies(Class<?> rootClass, Class<?> dependencyClass) {
        dependenciesMap.putIfAbsent(rootClass.getName(), Sets.newHashSet());
        final Set<String> dependencies = dependenciesMap.get(rootClass.getName());
        dependencies.add(dependencyClass.getName());
        dependenciesMap.put(rootClass.getName(), dependencies);
        scannedClasses.add(dependencyClass);
        scannedClasses.add(rootClass);
    }


}

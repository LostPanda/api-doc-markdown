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

    boolean isCycled(Class<?> aClass, Class<?> dependencyClass) throws ClassNotFoundException {
        if (dependencyClass.getName().equalsIgnoreCase(aClass.getName())) {
            return true;
        }
        addDependencies(aClass, dependencyClass);
        if (!scannedClasses.contains(aClass)) {
            return false;
        }
        return isReferenced(aClass, dependencyClass, Sets.newHashSet());
    }

    private boolean isReferenced(Class<?> aClass, Class<?> dependencyClass, Set<String> testedClasses) throws ClassNotFoundException {
        Set<String> dependencies = dependenciesMap.get(dependencyClass.getName());

        if (CollectionUtils.isEmpty(dependencies)) {
            testedClasses.add(dependencyClass.getName());
            return false;
        }
        if (dependencies.contains(aClass.getName()) || aClass == dependencyClass) {
            return false;
        }
        if (testedClasses.contains(dependencyClass.getName())) {
            return false;
        }

        testedClasses.add(dependencyClass.getName());
        for (String dependency : dependencies) {
            if (isReferenced(aClass, Class.forName(dependency), testedClasses)) {
                return true;
            }
        }
        return false;
    }

    private void addDependencies(Class<?> rootClass, Class<?> dependencyClass) {
        dependenciesMap.putIfAbsent(rootClass.getName(), Sets.newHashSet());
        final Set<String> dependencies = dependenciesMap.get(rootClass.getName());
        dependencies.add(dependencyClass.getName());
        scannedClasses.add(dependencyClass);
        scannedClasses.add(rootClass);
    }


}

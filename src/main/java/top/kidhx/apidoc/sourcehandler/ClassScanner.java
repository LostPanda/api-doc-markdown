package top.kidhx.apidoc.sourcehandler;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * @author HX
 * @date 2022/1/31
 */
@Component
public class ClassScanner {

    private static final String CLASS_POSTFIX = ".class";

    private static final String DOT = ".";

    private static final String SEPARATOR = File.separator;
    public static final String SLASH_REGEXP = "[\\\\/]";

    private final Log log;

    public ClassScanner(Log log) {
        this.log = log;
    }

    public Set<Class<?>> loadClass(String outputDir, String packageName, MavenProject mavenProject, URLClassLoader classLoader) throws MalformedURLException, ClassNotFoundException, NoSuchMethodException {
        Set<Class<?>> result = Sets.newHashSet();
        File file = new File(outputDir + "/classes");
        if (!file.exists() || !file.isDirectory()) {
            throw new IllegalStateException("invalid outputDir:" + outputDir);
        }
        result.addAll(loadProjectClasses(file, packageName, mavenProject, classLoader));
        return result;
    }

    private Collection<Class<?>> loadProjectClasses(File file, String packagePrefix, MavenProject mavenProject, URLClassLoader classLoader) throws MalformedURLException, NoSuchMethodException, ClassNotFoundException {
        Set<String> classFullNames = Sets.newHashSet();
        getClassFullNames(file, packagePrefix, classFullNames);

        HashSet<Class<?>> classes = Sets.newHashSet();
        List<URL> dependencyURLs = getDependencyURLs(mavenProject);

        dependencyURLs.add(file.toURI().toURL());

        Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addURL.setAccessible(true);
        dependencyURLs.forEach(URL -> {
            try {
                addURL.invoke(classLoader, URL);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });

        for (String classFullName : classFullNames) {
            if (classFullName.startsWith(packagePrefix.replaceAll(SLASH_REGEXP, "."))) {
                final Class<?> aClass = classLoader.loadClass(classFullName);
                classes.add(aClass);
            }
        }
        return classes;
    }

    private List<URL> getDependencyURLs(MavenProject mavenProject) {
        final Set<Artifact> artifacts = mavenProject.getArtifacts();
        final ArrayList<URL> urls = Lists.newArrayList();
        for (Artifact artifact : artifacts) {
            try {
                urls.add(artifact.getFile().toURI().toURL());
            } catch (MalformedURLException e) {
                log.warn("artifact:" + artifact.getArtifactId() + "not found");
            }
        }
        return urls;
    }

    private void getClassFullNames(File dir, String packagePrefix, Set<String> classFullNames) {
        if (dir.isFile()) {
            if (dir.getName().endsWith(CLASS_POSTFIX) && dir.getAbsolutePath().contains(packagePrefix)) {
                classFullNames.add((getClassFullName(dir, packagePrefix)));
            }
            return;
        }

        final File[] files = dir.listFiles();
        if (ArrayUtils.isEmpty(files)) {
            return;
        }
        for (File file : files) {
            getClassFullNames(file, packagePrefix, classFullNames);
        }
    }

    private String getClassFullName(File file, String packagePrefix) {
        return file.getAbsolutePath().substring(file.getAbsolutePath().indexOf(packagePrefix),
                file.getAbsolutePath().length() - CLASS_POSTFIX.length()).replace(SEPARATOR, DOT);
    }
}

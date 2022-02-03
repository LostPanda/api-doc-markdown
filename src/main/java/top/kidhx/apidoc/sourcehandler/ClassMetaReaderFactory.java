package top.kidhx.apidoc.sourcehandler;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import top.kidhx.apidoc.bo.enums.ClassInfoReaderType;

/**
 * @author HX
 * @date 2022/1/31
 */
public class ClassMetaReaderFactory {

    public static AbstractClassMetaReader createClassReader(ClassInfoReaderType type, MavenProject mavenProject, Log log, ClassLoader classLoader) {
        switch (type) {
            case SPRING_CONTROLLER:
                return new WebClassMetaReader(log, mavenProject, classLoader);
            case INTERFACE:
                return new InterfaceClassMetaReader(log, mavenProject, classLoader);
            default:
                return new InterfaceClassMetaReader(log, mavenProject, classLoader);
        }
    }
}

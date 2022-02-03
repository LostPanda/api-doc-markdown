package top.kidhx.apidoc.sourcehandler;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import top.kidhx.apidoc.bo.ClassMeta;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @author HX
 * @date 2022/1/31
 */
public class InterfaceClassMetaReader extends AbstractClassMetaReader {

    public InterfaceClassMetaReader(Log log, MavenProject mavenProject, ClassLoader classLoader) {
        super(log, mavenProject, classLoader);
    }

    @Override
    protected ClassMeta retrieveClassMeta(Class<?> aClass, String sourceFolder) throws Exception {
        return doRetrieveClassMeta(aClass, sourceFolder, true, true, null);
    }

    @Override
    protected boolean needRetrieve(Class<?> aClass) {
        return aClass.isInterface();
    }

    @Override
    protected String getApiName(Method method) {
        return method.getName();
    }

    @Override
    protected String getParameterName(Parameter parameter) {
        return parameter.getName();
    }
}

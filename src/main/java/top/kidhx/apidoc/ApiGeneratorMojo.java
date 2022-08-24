package top.kidhx.apidoc;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.springframework.util.CollectionUtils;
import top.kidhx.apidoc.apiwriter.ApiDocGenerator;
import top.kidhx.apidoc.bo.ClassMeta;
import top.kidhx.apidoc.bo.ExporterDefinition;
import top.kidhx.apidoc.bo.enums.ClassInfoReaderType;
import top.kidhx.apidoc.sourcehandler.AbstractClassMetaReader;
import top.kidhx.apidoc.sourcehandler.ClassMetaReaderFactory;
import top.kidhx.apidoc.sourcehandler.ClassScanner;

import java.io.File;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;

/**
 * @Author camushe
 * @Date 2022/1/30 20:32
 */
@Mojo(name = "generate-api-doc", requiresDependencyResolution = ResolutionScope.TEST, defaultPhase = LifecyclePhase.DEPLOY)
@Execute(phase = LifecyclePhase.COMPILE)
public class ApiGeneratorMojo extends AbstractMojo {

    URLClassLoader urlClassLoader;
    private ClassScanner classScanner;
    private AbstractClassMetaReader classMetaReader;
    private ApiDocGenerator apiDocGenerator;
    @Parameter(property = "project.build.directory", readonly = true)
    private File outputDirectory;
    @Parameter(property = "project.build.sourceDirectory", readonly = true)
    private File sourceDirectory;
    @Parameter
    private String packageName;
    @Parameter(defaultValue = "INTERFACE")
    private String readerType;
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;
    @Parameter
    private List<ExporterDefinition> exporters;
    private Log log;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        init();
        log.info("start generate api doc!");
        try {
            final Set<Class<?>> classes = classScanner.loadClass(outputDirectory.getAbsolutePath(), packageName, mavenProject, urlClassLoader);
            log.info("project classes found success:" + classes);

            final List<ClassMeta> classMetas = classMetaReader.getClassMetas(Lists.newArrayList(classes), sourceDirectory.getAbsolutePath(), urlClassLoader);
            log.info("apiMeta retrieved:" + classMetas);

            apiDocGenerator = new ApiDocGenerator(log, exporters, urlClassLoader);
            apiDocGenerator.generateApiDoc(classMetas);
            log.info("markdown api export success, pls check:" + outputDirectory);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void init() throws MojoExecutionException {
        this.log = getLog();
        classScanner = new ClassScanner(log);
        urlClassLoader = (URLClassLoader) this.getClass().getClassLoader();
        classMetaReader = ClassMetaReaderFactory.createClassReader(getReaderType(), mavenProject, log, urlClassLoader);
        if (CollectionUtils.isEmpty(exporters)) {
            exporters = Lists.newArrayList(
                    new ExporterDefinition()
                            .setExporterName("file")
                            .setParameters(new ImmutableMap.Builder<String, String>()
                                    .put("output", outputDirectory.getAbsolutePath() + File.separator + "apiDoc" + File.separator)
                                    .build()));
        }
        {
            //replace dot to file separator
            this.packageName = String.join(File.separator,packageName.split("\\."));
        }
    }

    private ClassInfoReaderType getReaderType() throws MojoExecutionException {
        try {
            return ClassInfoReaderType.valueOf(readerType);
        } catch (IllegalArgumentException e) {
            log.error("传入的解读类型错误");
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}

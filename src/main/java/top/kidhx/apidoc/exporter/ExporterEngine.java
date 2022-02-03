package top.kidhx.apidoc.exporter;

import com.google.common.collect.Maps;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.maven.plugin.logging.Log;
import top.kidhx.apidoc.bo.ExporterDefinition;
import top.kidhx.apidoc.bo.MarkDownApi;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/**
 * @author HX
 * @date 2022/1/31
 */
@Data
@Accessors(chain = true)
public class ExporterEngine {

    public static final String FILE_EXPORTER_NAME = "file";
    private static final Map<String, Class<? extends DocExporter>> exporterMap = Maps.newHashMap();
    private Log log;

    public ExporterEngine(Log log) {
        this.log = log;
    }

    public void invoke(ExporterDefinition exporterDefinition, List<MarkDownApi> markDownApis) {
        DocExporter docExporter = getExporter(exporterDefinition);
        docExporter.exportApiDoc(markDownApis);
    }

    private DocExporter getExporter(ExporterDefinition exporterDefinition) {
        try {
            Class<? extends DocExporter> exporterClass = exporterMap.get(exporterDefinition.getExporterName());
            Constructor<? extends DocExporter> declaredConstructor = exporterClass.getDeclaredConstructor(Log.class);
            DocExporter docExporter = declaredConstructor.newInstance(log);
            Map<String, String> parameters = exporterDefinition.getParameters();
            parameters.forEach((k, v) -> {
                Field declaredField = null;
                try {
                    declaredField = exporterClass.getDeclaredField(k);
                    declaredField.setAccessible(true);
                    declaredField.set(docExporter, v);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new IllegalStateException(exporterDefinition + "配置错误:" + e.getMessage());
                }
            });
            return docExporter;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(exporterDefinition + "配置错误:" + e.getMessage());
        }
    }
}

package top.kidhx.apidoc.apiwriter;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import top.kidhx.apidoc.bo.*;
import top.kidhx.apidoc.exporter.ExporterEngine;
import top.kidhx.apidoc.utils.StreamUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author HX
 * @date 2022/1/31
 */
public class ApiDocGenerator {

    private MarkdownWriter writer;

    private Mocker mocker;

    private Log log;

    private List<ExporterDefinition> exporters;

    private ExporterEngine exporterEngine;

    public ApiDocGenerator(Log log, List<ExporterDefinition> exporters, ClassLoader loader) {
        this.log = log;
        this.exporters = exporters;
        writer = new MarkdownWriter(log);
        mocker = new Mocker(loader);
        exporterEngine = new ExporterEngine(log);
    }

    public void generateApiDoc(List<ClassMeta> targetClasses) throws Exception {
        List<MarkDownApi> markDownApis = Lists.newArrayList();

        for (ClassMeta targetClass : targetClasses) {
            log.info("generate api doc for " + targetClass.getClassName() + "...");
            markDownApis.add(preGenerateApi(targetClass, Sets.newHashSet()));
        }
        exportApi(markDownApis);
    }

    private void exportApi(List<MarkDownApi> markDownApis) {
        log.info("start export api!");
        if (CollectionUtils.isEmpty(markDownApis)) {
            log.warn("empty api found, finish!");
            return;
        }
        exporters.forEach(exporterDefinition -> exporterEngine.invoke(exporterDefinition, markDownApis));
    }

    private MarkDownApi preGenerateApi(ClassMeta classMeta, Set<String> writedClasses) throws Exception {
        writedClasses.add(classMeta.getClassName());

        final StringBuilder builder = new StringBuilder();
        builder.append(writer.h(classMeta.getClassName(), 1));
        builder.append("\n");
        if (StringUtils.isNotBlank(classMeta.getDesc())) {
            builder.append(writer.b("类描述: " + classMeta.getDesc()));
        }

        final List<Api> apis = classMeta.getApis();
        for (Api api : apis) {
            doGenerateApi(api, builder);
        }

        return new MarkDownApi()
                .setName(StringUtils.isNotBlank(classMeta.getDesc()) ? classMeta.getDesc() : classMeta.getClassName())
                .setMd5(generateMd5(classMeta.getClassName()))
                .setContent(builder.toString());
    }

    private String generateMd5(String text) {
        return DigestUtils.md5DigestAsHex(text.getBytes());
    }

    private void doGenerateApi(Api api, StringBuilder builder) throws Exception {
        builder.append(writer.h(api.getDesc() != null ? api.getDesc() : api.getName(), 2));
        builder.append("\n");
        builder.append(writer.h("接口名：", 3));
        builder.append(api.getName());
        builder.append("\n");


    }

    private void generateReturnValue(FieldMeta returnValue, StringBuilder builder) throws Exception {
        builder.append("\n");
        builder.append(writer.h("返回结果示例", 3));
        builder.append("```JSON\n");
        builder.append(mocker.mock(returnValue));
        builder.append("```\n");
        builder.append(writer.h("返回结果说明", 3));
        builder.append("\n");
        doGenerateParameter(Lists.newArrayList(returnValue), builder, "returnValue");
    }

    private void generateParameter(List<FieldMeta> parameters, StringBuilder builder) {
        builder.append(writer.h("参数：", 3));
        builder.append("\n");
        if (CollectionUtils.isEmpty(parameters)) {
            builder.append("无");
            return;
        }

        doGenerateParameter(parameters, builder, "param");
    }

    private void doGenerateParameter(List<FieldMeta> parameters, StringBuilder builder, String type) {
        List<ClassMeta> classMetas = Lists.newArrayList();

        parameters = parameters.stream().filter(a -> Objects.nonNull(a.getType())).collect(Collectors.toList());

        for (FieldMeta fieldMeta : parameters) {
            if (fieldMeta.getType() != null) {
                if (isCustomType(fieldMeta.getType().getClassName())) {
                    classMetas.add(fieldMeta.getType());
                }
                retrieveClassInfo(fieldMeta.getType(), classMetas);
            }
        }

        tableHeader(builder, type);
        for (FieldMeta parameter : parameters) {
            handleFieldMeta(parameter, builder, classMetas, type);
        }

        classMetas = classMetas.stream().filter(StreamUtils.distinctByKey(ClassMeta::getClassName)).collect(Collectors.toList());
        appendCustomFields(classMetas, builder, type);
    }

    private void appendCustomFields(List<ClassMeta> classMetas, StringBuilder builder, String type) {
        if (CollectionUtils.isEmpty(classMetas)) {
            return;
        }
        classMetas = classMetas.stream().filter(a -> isCustomType(a.getClassName())).collect(Collectors.toList());
        for (ClassMeta classMeta : classMetas) {
            generateCustomClassTable(classMeta, builder, type);
        }
    }

    private void generateCustomClassTable(ClassMeta classMeta, StringBuilder builder, String type) {
        if (!isCustomType(classMeta.getClassName())) {
            return;
        }

        builder.append(writer.b(classMeta.getClassName()));
        builder.append("\n");

        for (FieldMeta classFieldMeta : classMeta.getClassFieldMetas()) {
            builder.append(writer.tableCell(classFieldMeta.getName(), "head"));
            builder.append(writer.tableCell(classFieldMeta.getTypeName(), null));
            builder.append(writer.tableCell(Optional.ofNullable(classFieldMeta.getDesc()).orElse("暂无"), null));
            if ("param".equalsIgnoreCase(type)) {
                builder.append(writer.tableCell(classFieldMeta.getRestriction(), null));
            }
            builder.append("\n");
        }

    }

    private void handleFieldMeta(FieldMeta parameter, StringBuilder builder, List<ClassMeta> classMetas, String type) {
        builder.append(writer.tableCell(parameter.getName(), "head"));
        builder.append(writer.tableCell(parameter.getTypeName(), null));
        builder.append(writer.tableCell(parameter.getDesc(), null));
        if ("param".equalsIgnoreCase(type)) {
            builder.append(writer.tableCell(parameter.getRestriction(), null));
        }
    }

    private void tableHeader(StringBuilder builder, String type) {
        builder.append("\n");
        builder.append(writer.tableCell("名称", "head"));
        builder.append(writer.tableCell("类型", null));
        builder.append(writer.tableCell("说明", null));
        if ("param".equalsIgnoreCase(type)) {
            builder.append(writer.tableCell("约束", null));
        }
        builder.append("\n");
        builder.append("| ------ | ------ | ------ | ------ |");
        if ("param".equalsIgnoreCase(type)) {
            builder.append(" ------ |");
        }
        builder.append("\n");
    }

    private void retrieveClassInfo(ClassMeta classMeta, List<ClassMeta> classMetas) {
        if (classMeta == null) {
            return;
        }
        if (!CollectionUtils.isEmpty(classMeta.getGenericTypes())) {
            for (ClassMeta genericType : classMeta.getGenericTypes()) {
                if (!isCustomType(genericType.getClassName())) {
                    classMetas.add(genericType);
                }
                retrieveClassInfo(genericType, classMetas);
            }
        }

        if (!CollectionUtils.isEmpty(classMeta.getClassFieldMetas())) {
            for (FieldMeta classFieldMeta : classMeta.getClassFieldMetas()) {
                if (classFieldMeta.getType() != null) {
                    if (isCustomType(classFieldMeta.getType().getClassName())) {
                        retrieveClassInfo(classFieldMeta.getType(), classMetas);
                    }
                }
            }
        }
    }

    private boolean isCustomType(String className) {
        if (StringUtils.isBlank(className)) {
            return false;
        }

        return !className.startsWith("java") &&
                !className.startsWith("com.apache") &&
                !className.startsWith("org.spring") &&
                !className.contains("fastjson") &&
                !"int".equalsIgnoreCase(className) &&
                !"char".equalsIgnoreCase(className) &&
                !"double".equalsIgnoreCase(className) &&
                !"long".equalsIgnoreCase(className) &&
                !"short".equalsIgnoreCase(className) &&
                !"byte".equalsIgnoreCase(className) &&
                !"float".equalsIgnoreCase(className) &&
                !"boolean".equalsIgnoreCase(className);
    }
}

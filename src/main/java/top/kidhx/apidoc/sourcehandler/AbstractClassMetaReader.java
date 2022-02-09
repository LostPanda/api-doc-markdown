package top.kidhx.apidoc.sourcehandler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import top.kidhx.apidoc.bo.*;
import top.kidhx.apidoc.bo.enums.CommentType;

import javax.validation.constraints.*;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static top.kidhx.apidoc.sourcehandler.ClassScanner.SLASH_REGEXP;

/**
 * @author HX
 * @date 2022/1/31
 */
public abstract class AbstractClassMetaReader {

    public static Map<String, ClassMeta> foundClasses;
    protected Log log;
    protected ClassLoader classLoader;
    private final CycleReferenceChecker cycleReferenceChecker;
    private URLClassLoader urlClassLoader;
    private final SourceCodeReader sourceCodeReader;
    private List<Class<?>> classes;
    private final MavenProject mavenProject;

    public AbstractClassMetaReader(Log log, MavenProject mavenProject, ClassLoader classLoader) {
        cycleReferenceChecker = new CycleReferenceChecker();
        foundClasses = Maps.newHashMap();
        sourceCodeReader = new SourceCodeReader();
        this.log = log;
        this.mavenProject = mavenProject;
        this.classLoader = classLoader;
    }

    public final List<ClassMeta> getClassMetas(List<Class<?>> classes, String sourceFolder, URLClassLoader urlClassLoader) throws Exception {
        log.info("start scan api!");
        this.classes = classes;
        this.urlClassLoader = urlClassLoader;

        if (CollectionUtils.isEmpty(classes)) {
            return Lists.newArrayList();
        }
        List<ClassMeta> result = Lists.newArrayList();
        for (Class<?> aClass : classes) {
            if (needRetrieve(aClass)) {
                log.info("reading class:" + aClass.getName() + "...");
                final ClassMeta classMeta = retrieveClassMeta(aClass, sourceFolder);
                if (classMeta != null) {
                    result.add(classMeta);
                }
            }
        }
        return result;
    }

    protected abstract ClassMeta retrieveClassMeta(Class<?> aClass, String sourceFolder) throws Exception;

    protected abstract boolean needRetrieve(Class<?> aClass);

    protected abstract String getApiName(Method method);

    protected abstract String getParameterName(Parameter parameter);

    protected boolean isNotTargetMethod(Method method) {
        return false;
    }

    private String getResourcePath(Class<?> aClass, String sourceFolder) {
        final String artifactId = mavenProject.getArtifact().getArtifactId();
        if (!artifactId.contains("-")) {
            log.error("模块配置错误，命名规则: ‘工程名-模块名’");
            throw new IllegalStateException("模块配置错误，命名规则: ‘工程名-模块名’");
        }
        final String prefix = artifactId.substring(0, artifactId.indexOf("-") + 1);
        if (aClass.getResource("") == null || aClass.getResource("").getPath() == null) {
            return aClass.getResource(File.separator).getPath().replaceAll("target/classes", "src/main/java");
        }

        final List<String> strings = Lists.newArrayList(aClass.getResource("").getPath().split(SLASH_REGEXP));
        final MavenProject parentModule = mavenProject.getParent();
        if (parentModule != null) {
            final List<String> modules = parentModule.getModules();
            for (String string : strings) {
                if (modules.contains(string)) {
                    return sourceFolder.replaceAll(artifactId, string);
                }
            }
        }
        return aClass.getResource("/").getPath().replaceAll("target/classes", "src/main/java");
    }

    protected ClassMeta doRetrieveClassMeta(Class<?> aClass, String sourceFolder, boolean needMethod, boolean needField, Map<String, ClassMeta> parameterizedMap) throws Exception {
        final File source = new File(sourceFolder);
        if (source.isFile() || !source.exists()) {
            throw new IllegalStateException("incorrect source folder");
        }

        final File file = new File(getSourcePath(aClass, sourceFolder));
        Map<String, Comment> commentMap = Maps.newHashMap();
        ClassMeta classMeta = new ClassMeta()
                .setClassName(aClass.getName())
                .setClassType(aClass);
        if (file.exists()) {
            classMeta.setDesc(sourceCodeReader.resolveClassComment(file));
            commentMap = sourceCodeReader.resolveComment(file, CommentType.API);
        }
        if (isCustomType(aClass)) {
            if (needField || aClass.isEnum()) {
                classMeta.setClassFieldMetas(listFieldMetas(aClass, source, parameterizedMap));
            }
            if (needMethod) {
                classMeta.setApis(listApiInfos(aClass, source, commentMap, parameterizedMap));
            }
        }
        foundClasses.put(aClass.getName(), classMeta);
        return classMeta;
    }

    protected List<Api> listApiInfos(Class<?> aClass, File source, Map<String, Comment> commentMap, Map<String, ClassMeta> parameterizedMap) throws Exception {
        List<Api> result = Lists.newArrayList();
        final Method[] methods = ReflectionUtils.getAllDeclaredMethods(aClass);
        for (Method method : methods) {
            if (method.isSynthetic() || isNotTargetMethod(method)) {
                continue;
            }
            method.setAccessible(true);
            final Api api = new Api();
            api.setName(getApiName(method));
            api.setReturnValue(getReturnValue(aClass, method, source, commentMap.get(method.getName())));
            api.setParameters(listParameters(aClass, method, source, commentMap.get(method.getName())));
            api.setDesc(Optional.ofNullable(commentMap.get(method.getName())).orElse(new Comment()).getValue());
            result.add(api);
        }
        return result;
    }

    private FieldMeta getReturnValue(Class<?> aClass, Method method, File source, Comment comment) throws Exception {
        final FieldMeta fieldMeta = new FieldMeta();
        final Class<?> returnType = method.getReturnType();
        final Type genericReturnType = method.getGenericReturnType();

        final MethodComment methodComment = (MethodComment) comment;
        fieldMeta.setName(returnType.getName());
        fieldMeta.setTypeName(genericReturnType.getTypeName());
        fieldMeta.setType("void".equals(genericReturnType.getTypeName()) ? null : getInnerClassMeta(aClass, returnType, source, genericReturnType));
        fieldMeta.setDesc(methodComment == null || methodComment.getReturnComment() == null ? "暂无" : methodComment.getReturnComment().getValue());
        return fieldMeta;
    }

    private ClassMeta getInnerClassMeta(Class<?> owner, Class<?> aClass, File source, Type genericReturnType) throws Exception {
        if (isCustomType(aClass)) {
            if (!cycleReferenceChecker.isCycled(owner, aClass)) {
                Map<String, ClassMeta> parameterizedMap = null;
                if (genericReturnType instanceof ParameterizedType) {
                    parameterizedMap = getParameterizedMap(owner, source, aClass, genericReturnType);
                }
                return doRetrieveClassMeta(aClass, source.getAbsolutePath(), false, true, parameterizedMap);
            } else {
                final ClassMeta classMeta = foundClasses.get(aClass.getName());
                if (classMeta == null) {
                    return new ClassMeta().setClassName(aClass.getName()).setClassType(aClass);
                }
                return classMeta;
            }
        } else if (genericReturnType != null && isGenericType(genericReturnType.getTypeName())) {
            if (!cycleReferenceChecker.isCycled(owner, aClass) || foundClasses.get(aClass.getName()) == null) {
                final ClassMeta classMeta = new ClassMeta();
                classMeta.setGenericTypes(listGenericType(aClass, genericReturnType.getTypeName(), source));
                classMeta.setClassName(((ParameterizedType) genericReturnType).getTypeName());
                classMeta.setClassType((Class<?>) (((ParameterizedType) genericReturnType).getRawType()));
                return classMeta;
            } else {
                return foundClasses.get(aClass.getName());
            }
        }
        return new ClassMeta().setClassName(aClass.getName()).setClassType(aClass);
    }

    private List<ClassMeta> listGenericType(Class<?> genericOwner, String typeName, File source) throws Exception {
        List<ClassMeta> classMetas = Lists.newArrayList();
        final String tempName = typeName.substring(typeName.indexOf("<") + 1, typeName.length() - 1);
        if (tempName.equalsIgnoreCase("?")) {
            return null;
        }
        List<String> parameterizedNameList = getParameterizedNameList(tempName);
        for (String className : parameterizedNameList) {
            final ClassMeta classMeta = new ClassMeta();
            if (isGenericType(className)) {
                final String classOriginalName = className.substring(0, className.indexOf("<")).trim();
                classMeta.setGenericTypes(listGenericType(genericOwner, className, source));
                classMeta.setClassType(urlClassLoader.loadClass(classOriginalName));
                classMeta.setClassName(classOriginalName);
                classMetas.add(classMeta);
            } else {
                Class<?> aClass = null;
                try {
                    aClass = urlClassLoader.loadClass(className.trim());
                } catch (ClassNotFoundException e) {
                    continue;
                }
                final ClassMeta innerClassMeta = getInnerClassMeta(genericOwner, aClass, source, null);
                classMetas.add(innerClassMeta);
            }
        }
        return classMetas;
    }

    private List<String> getParameterizedNameList(String tempName) {
        tempName = tempName.trim();
        if (StringUtils.isBlank(tempName)) {
            return Lists.newArrayList();
        }
        if (tempName.startsWith(",")) {
            tempName = tempName.substring(tempName.indexOf(",") + 1);
        }
        if (tempName.startsWith(",")) {
            tempName = tempName.substring(tempName.indexOf(">") + 1);
        }
        if (!tempName.contains(",")) {
            return Lists.newArrayList(tempName);
        }
        tempName = tempName.trim();
        if (!tempName.contains("<")) {
            return Lists.newArrayList(tempName.split(","));
        }
        if (!checkNested(tempName)) {
            return Lists.newArrayList(tempName);
        }

        List<String> result = Lists.newArrayList();
        if (tempName.indexOf(",") > tempName.indexOf("<")) {
            String firstGenericType = getFirstGenericType(tempName);
            result.add(firstGenericType);
            result.addAll(getNames(tempName.substring(tempName.indexOf(firstGenericType) + firstGenericType.length())));
        } else {
            result.add(tempName.substring(0, tempName.indexOf(",")));
            result.addAll(getNames(tempName.substring(tempName.indexOf(",") + 1)));
        }
        return result;
    }

    private Collection<String> getNames(String name) {
        name = name.trim();
        if (StringUtils.isBlank(name)) {
            return Lists.newArrayList();
        }
        if (name.startsWith(",")) {
            name = name.substring(name.indexOf(",") + 1);
        }
        if (name.startsWith(">")) {
            name = name.substring(name.indexOf(">") + 1);
        }
        if (!name.contains(",")) {
            return Lists.newArrayList(name);
        }
        name = name.trim();
        if (!name.contains("<")) {
            return Lists.newArrayList(name.split(","));
        }
        if (!checkNested(name)) {
            return Lists.newArrayList(name);
        }

        List<String> result = Lists.newArrayList();
        if (name.indexOf(",") > name.indexOf("<")) {
            String firstGenericType = getFirstGenericType(name);
            result.add(firstGenericType);
            result.addAll(getNames(name.substring(name.indexOf(firstGenericType) + firstGenericType.length())));
        } else {
            result.add(name.substring(0, name.indexOf(",")));
            result.addAll(getNames(name.substring(name.indexOf(",") + 1)));
        }
        return result;
    }

    private String getFirstGenericType(String name) {
        final char[] chars = name.toCharArray();
        int leftBlockNum = 0;
        int rightBlockNum = 0;
        int targetNum = 0;

        for (int i = 0; i < chars.length; i++) {
            char aChar = chars[i];
            if (aChar == '<') {
                leftBlockNum++;
            }
            if (aChar == '>') {
                rightBlockNum++;
            }
            if (leftBlockNum == rightBlockNum && leftBlockNum != 0) {
                targetNum = i;
                break;
            }
        }
        return name.substring(0, targetNum + 1);
    }

    private boolean checkNested(String tempName) {
        if (!tempName.contains("<")) {
            return false;
        }
        if (tempName.contains(",") && tempName.indexOf(",") < tempName.indexOf("<")) {
            return true;
        }
        final char[] chars = tempName.toCharArray();
        int count = 0;
        for (char aChar : chars) {
            if ('<' == aChar) {
                count++;
            }
        }
        return count > 1;
    }

    private boolean isGenericType(String typeName) {
        if (StringUtils.isBlank(typeName)) {
            return false;
        }
        return typeName.contains("<") || typeName.endsWith(">");
    }

    private Map<String, ClassMeta> getParameterizedMap(Class<?> rootClass, File source, Class<?> ownerType, Type genericType) throws Exception {
        Map<String, ClassMeta> result = Maps.newHashMap();
        final String typeName = genericType.getTypeName();
        final TypeVariable<? extends Class<?>>[] typeParameters = ownerType.getTypeParameters();
        final ParameterizedType genericType1 = (ParameterizedType) genericType;
        final Type[] actualTypeArguments = genericType1.getActualTypeArguments();
        for (int i = 0; i < typeParameters.length; i++) {
            if (actualTypeArguments[i] instanceof Class) {
                result.put(typeParameters[i].getName(), getInnerClassMeta(rootClass, (Class<?>) actualTypeArguments[i], source, null));
            } else if (actualTypeArguments[i] instanceof ParameterizedType) {
                result.put(typeParameters[i].getName(), getInnerClassMeta(rootClass, (Class<?>) ((ParameterizedType) actualTypeArguments[i]).getRawType(), source, actualTypeArguments[i]));
            }
        }
        return result;
    }

    private List<FieldMeta> listParameters(Class<?> aClass, Method method, File source, Comment comment) throws Exception {
        final MethodComment methodComment = (MethodComment) comment;
        List<FieldMeta> result = Lists.newArrayList();
        Map<String, Comment> parameterComments = methodComment == null ? Maps.newHashMap() : methodComment.getParameterComment();
        final Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            final FieldMeta fieldMeta = new FieldMeta();
            final Type parameterizedType = parameter.getParameterizedType();
            fieldMeta.setName(getParameterName(parameter));
            fieldMeta.setTypeName(parameterizedType.getTypeName());
            fieldMeta.setDesc(parameterComments.get(parameter.getName()) == null ? "暂无" : parameterComments.get(parameter.getName()).getValue());
            fieldMeta.setRestriction(findRestrictions(parameter));
            fieldMeta.setType(getInnerClassMeta(aClass, parameter.getType(), source, parameterizedType));
            result.add(fieldMeta);
        }
        return result;
    }

    private String findRestrictions(AnnotatedElement parameter) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final StringBuilder builder = new StringBuilder();

        if (ArrayUtils.isEmpty(parameter.getAnnotations())) {
            return StringUtils.EMPTY;
        }

        final Annotation notNull =  getAnnotation(parameter.getAnnotations(), NotNull.class);
        if (notNull != null) {
            builder.append("`非空`<br/>");
        }

        final Annotation notBlank = getAnnotation(parameter.getAnnotations(), NotBlank.class);
        final Annotation hibernateNotBlank = getAnnotation(parameter.getAnnotations(),org.hibernate.validator.constraints.NotBlank.class);
        if (notBlank != null || hibernateNotBlank != null) {
            builder.append("`非空字符串`<br/>");
        }

        final Annotation doNull = getAnnotation(parameter.getAnnotations(), Null.class);
        if (doNull != null) {
            builder.append("`必须为空值`<br/>");
        }

        final Annotation digits = getAnnotation(parameter.getAnnotations(), Digits.class);
        if (digits != null) {
            builder.append("`必须为数字-");
            final Object integer = digits.annotationType().getMethod("integer").invoke(digits);
            final Object fraction = digits.annotationType().getMethod("fraction").invoke(digits);
            builder.append("整数最大值为:" + integer + "位");
            builder.append("小数位最多为:" + fraction + "位`<br/>");
        }

        final Annotation assertFalse = getAnnotation(parameter.getAnnotations(), AssertFalse.class);
        if (assertFalse != null) {
            builder.append("`必须为false`<br/>");
        }

        final Annotation assertTrue = getAnnotation(parameter.getAnnotations(), AssertTrue.class);
        if (assertTrue != null) {
            builder.append("`必须为true`<br/>");
        }

        final Annotation max = getAnnotation(parameter.getAnnotations(), Max.class);
        if (max != null) {
            builder.append("`整形，最小值为");
            builder.append(max.annotationType().getMethod("value").invoke(max));
            builder.append("`<br/>");
        }

        final Annotation min = getAnnotation(parameter.getAnnotations(), Min.class);
        if (min != null) {
            builder.append("`整形，最大值为");
            builder.append(min.annotationType().getMethod("value").invoke(min));
            builder.append("`<br/>");
        }

        final Annotation past = getAnnotation(parameter.getAnnotations(), Past.class);
        if (past != null) {
            builder.append("`必须为过去的时间`<br/>");
        }

        final Annotation pattern = getAnnotation(parameter.getAnnotations(), Pattern.class);
        if (pattern != null) {
            builder.append("`请匹配正则表达式:");
            builder.append(pattern.annotationType().getMethod("regexp").invoke(pattern));
            builder.append("`<br/>");
        }

        final Annotation size = getAnnotation(parameter.getAnnotations(), Size.class);
        if (size != null) {
            builder.append("`最大容量为:");
            builder.append(size.annotationType().getMethod("max").invoke(size));
            builder.append(",最小容量为:");
            builder.append(size.annotationType().getMethod("min").invoke(size));
            builder.append("`<br/>");
        }

        final Annotation decimalMax = getAnnotation(parameter.getAnnotations(), DecimalMax.class);
        if (decimalMax != null) {
            builder.append("`浮点型最大值为:");
            builder.append(decimalMax.annotationType().getMethod("value").invoke(decimalMax));
            builder.append("是否包含边界:");
            builder.append(decimalMax.annotationType().getMethod("inclusive").invoke(decimalMax));
            builder.append("`<br/>");
        }

        final Annotation decimalMin = getAnnotation(parameter.getAnnotations(), DecimalMin.class);
        if (decimalMin != null) {
            builder.append("`浮点型最小值为:");
            builder.append(decimalMin.annotationType().getMethod("value").invoke(decimalMin));
            builder.append("是否包含边界:");
            builder.append(decimalMin.annotationType().getMethod("inclusive").invoke(decimalMin));
            builder.append("`<br/>");
        }

        final Annotation future = getAnnotation(parameter.getAnnotations(), Future.class);
        if (future != null) {
            builder.append("`必须为未来的时间`<br/>");
        }

        final Annotation pastOrPresent = getAnnotation(parameter.getAnnotations(), PastOrPresent.class);
        if (pastOrPresent != null) {
            builder.append("`必须为当前或者过去的时间`<br/>");
        }

        final Annotation negative = getAnnotation(parameter.getAnnotations(), Negative.class);
        if (negative != null) {
            builder.append("`必须为负数`<br/>");
        }

        final Annotation negativeOrZero = getAnnotation(parameter.getAnnotations(), NegativeOrZero.class);
        if (negativeOrZero != null) {
            builder.append("`必须为零或者负数`<br/>");
        }

        final Annotation positive = getAnnotation(parameter.getAnnotations(), Positive.class);
        if (positive != null) {
            builder.append("`必须为正数`<br/>");
        }

        final Annotation positiveOrZero = getAnnotation(parameter.getAnnotations(), PositiveOrZero.class);
        if (positiveOrZero != null) {
            builder.append("`必须为零或者整数`<br/>");
        }

        final Annotation notEmpty = getAnnotation(parameter.getAnnotations(), NotEmpty.class);
        if (notEmpty != null) {
            builder.append("`数组或字符串不能null, 并且容量必须大于0`<br/>");
        }

        final Annotation email = getAnnotation(parameter.getAnnotations(), Email.class);
        if (email != null) {
            builder.append("`必须为电子邮件地址格式`<br/>");
        }

        final Annotation requestBody = getAnnotation(parameter.getAnnotations(), RequestBody.class);
        if (requestBody != null) {
            builder.append("`请从requestBody传入参数`<br/>");
        }
        final String restrictions = builder.toString();
        if (StringUtils.isBlank(restrictions)) {
            return StringUtils.EMPTY;
        }
        return restrictions.substring(0, restrictions.lastIndexOf("<br/>"));
    }

    private Annotation getAnnotation(Annotation[] annotations, Class<? extends Annotation> annotationClass){
        for (Annotation annotation : annotations) {
            if(annotationClass.getName().equalsIgnoreCase(annotation.annotationType().getName())){
                return annotation;
            }
        }
        return null;
    }

    private List<FieldMeta> listFieldMetas(Class<?> aClass, File source, Map<String, ClassMeta> parameterizedMap) throws Exception {
        if (!isCustomType(aClass)) {
            return null;
        }
        Map<String, Comment> fieldCommentMap = getFieldCommentMap(aClass, source);
        List<FieldMeta> result = doGetFieldMetas(aClass, source, parameterizedMap, fieldCommentMap);
        addFieldInSuperClass(aClass, source, result);
        return CollectionUtils.isEmpty(result) ? null : result;
    }

    private void addFieldInSuperClass(Class<?> aClass, File source, List<FieldMeta> result) throws Exception {
        final Class<?> superclass = aClass.getSuperclass();
        if (superclass != null && Object.class != superclass) {
            final Type genericSuperclass = aClass.getGenericSuperclass();
            ClassMeta classMeta;
            if (genericSuperclass instanceof ParameterizedType) {
                classMeta = doRetrieveClassMeta(superclass, source.getAbsolutePath(), false, true, getParameterizedMap(aClass, source, superclass, genericSuperclass));
            } else {
                classMeta = doRetrieveClassMeta(superclass, source.getAbsolutePath(), false, true, null);
            }
            if (!CollectionUtils.isEmpty(classes) && !CollectionUtils.isEmpty(classMeta.getClassFieldMetas())) {
                result.addAll(classMeta.getClassFieldMetas());
            }
        }
    }

    private List<FieldMeta> doGetFieldMetas(Class<?> aClass, File source, Map<String, ClassMeta> parameterizedMap, Map<String, Comment> fieldCommentMap) throws Exception {
        List<FieldMeta> result = Lists.newArrayList();
        final Field[] declaredFields = aClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (needNotGetDetail(declaredField, aClass)) {
                continue;
            }
            declaredField.setAccessible(true);
            result.add(getFieldInfo(aClass, declaredField, parameterizedMap, source, fieldCommentMap));
        }
        return result;
    }

    private FieldMeta getFieldInfo(Class<?> rootClass, Field field, Map<String, ClassMeta> parameterizedMap, File source, Map<String, Comment> fieldCommentMap) throws Exception {
        final FieldMeta fieldMeta = new FieldMeta();
        fieldMeta.setName(field.getName());
        fieldMeta.setTypeName(parameterizedMap != null && parameterizedMap.get(field.getGenericType().getTypeName()) != null ?
                parameterizedMap.get(field.getGenericType().getTypeName()).getClassName() :
                processTypeName(field.getGenericType().getTypeName(), parameterizedMap));
        fieldMeta.setDesc(fieldCommentMap == null || fieldCommentMap.get(field.getName()) == null ? "暂无" : fieldCommentMap.get(field.getName()).getValue());
        fieldMeta.setRestriction(findRestrictions(field));

        if (!field.isEnumConstant()) {
            fieldMeta.setType(parameterizedMap != null && parameterizedMap.get(field.getGenericType().getTypeName()) != null ?
                    parameterizedMap.get(field.getGenericType().getTypeName()) :
                    getInnerClassMeta(rootClass,
                            parameterizedMap != null && parameterizedMap.get(field.getGenericType().getTypeName()) != null ? parameterizedMap.get(field.getGenericType().getTypeName()).getClassType() : field.getType(),
                            source, processGenericType(fieldMeta, field)));
        }
        return fieldMeta;
    }

    private Type processGenericType(FieldMeta fieldMeta, Field field) {
        final Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) genericType;
            final ParameterizedTypeImpl make = ParameterizedTypeImpl.make((Class<?>) parameterizedType.getRawType(), parameterizedType.getActualTypeArguments(), parameterizedType.getOwnerType());
            make.setName(fieldMeta.getTypeName());
            return make;
        } else {
            return genericType;
        }
    }

    private String processTypeName(String typeName, Map<String, ClassMeta> parameterizedMap) {
        if (parameterizedMap == null) {
            return typeName;
        }

        final String tempName1 = typeName.substring(typeName.indexOf("<") + 1, typeName.lastIndexOf(">"));
        final String tempName2 = typeName.substring(0, typeName.indexOf("<"));
        String tempName = StringUtils.EMPTY;

        for (Map.Entry<String, ClassMeta> metaEntry : parameterizedMap.entrySet()) {
            tempName = tempName1.replace(metaEntry.getKey(), metaEntry.getValue().getClassName());
        }
        return tempName2 + "<" + tempName + ">";
    }

    private boolean needNotGetDetail(Field declaredField, Class<?> aClass) {
        if (aClass.isEnum()) {
            return declaredField.isSynthetic() || !declaredField.isEnumConstant();
        }
        return Modifier.isFinal(declaredField.getModifiers()) ||
                Modifier.isStatic(declaredField.getModifiers()) ||
                declaredField.isSynthetic();
    }

    private Map<String, Comment> getFieldCommentMap(Class<?> aClass, File source) throws Exception {
        Map<String, Comment> fieldCommentMap = Maps.newHashMap();
        final File file = new File(getSourcePath(aClass, source.getAbsolutePath()));
        if (file.exists()) {
            fieldCommentMap = sourceCodeReader.resolveComment(file, aClass.isEnum() ? CommentType.ENUM : CommentType.FIELD);
        }
        return fieldCommentMap;
    }

    protected boolean isCustomType(Class<?> aClass) {
        if (aClass == null || StringUtils.isBlank(aClass.getName())) {
            return false;
        }
        final String className = aClass.getName();
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

    private String getSourcePath(Class<?> aClass, String sourceFolder) {
        String name = aClass.getName();
        String path = getResourcePath(aClass, sourceFolder);

        String replace = name.replace(".", File.separator);
        if (!sourceFolder.endsWith(File.separator)) {
            sourceFolder = sourceFolder + File.separator;
        }
        return path + ((path.endsWith(File.separator) ? "" : File.separator)) + replace + ".java";
    }


}

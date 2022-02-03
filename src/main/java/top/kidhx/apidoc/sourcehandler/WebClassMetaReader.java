package top.kidhx.apidoc.sourcehandler;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import top.kidhx.apidoc.bo.ClassMeta;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author HX
 * @date 2022/1/31
 */
public class WebClassMetaReader extends AbstractClassMetaReader {
    public static final String REQUESTBODY_CLASS_NAME = "org.springframework.web.bin.annotation.RequestBody";
    public static final String REQUESTPARAM_CLASS_NAME = "org.springframework.web.bin.annotation.RequestParam";
    public static final String METHOD_VALUE = "value";
    public static final String PATH_METHOD = "path";
    public static final String REQUEST_METHOD = "method";
    public static final Class<? extends Annotation>[] CONTROLLER_ANNOTATIONS = new Class[]{RestController.class, Controller.class};

    private final Class<?>[] mappingClasses = new Class[]{RequestMapping.class, GetMapping.class, PostMapping.class, PutMapping.class, DeleteMapping.class, PatchMapping.class};
    ThreadLocal<Class<?>> currentClass = new ThreadLocal<>();
    private Map<String, Class<? extends Annotation>> classLoaderMappings = Maps.newConcurrentMap();

    public WebClassMetaReader(Log log, MavenProject mavenProject, ClassLoader classLoader) {
        super(log, mavenProject, classLoader);
    }

    private void initMappingClass() {
        for (Class<?> mappingClass : mappingClasses) {
            try {
                final Class<?> aClass = classLoader.loadClass(mappingClass.getName());
                classLoaderMappings.put(aClass.getSimpleName(), (Class<? extends Annotation>) aClass);
            } catch (ClassNotFoundException e) {
                // do nothing
            }
        }
    }

    @Override
    protected ClassMeta retrieveClassMeta(Class<?> aClass, String sourceFolder) throws Exception {
        if (classLoaderMappings.entrySet().isEmpty()) {
            initMappingClass();
        }
        return doRetrieveClassMeta(aClass, sourceFolder, true, false, null);
    }

    @Override
    protected boolean needRetrieve(Class<?> aClass) {
        log.info("current class:" + aClass);
        for (Class<? extends Annotation> controllerAnnotationClass : CONTROLLER_ANNOTATIONS) {
            try {
                final Class<?> controllerAnnotation = classLoader.loadClass(controllerAnnotationClass.getName());
                if (controllerAnnotation != null) {
                    return true;
                }
            } catch (ClassNotFoundException e) {
                log.error(e);
            }
        }
        return false;
    }

    @Override
    protected String getApiName(Method method) {
        final Class<?> aClass = currentClass.get();
        Set<String> classRoutes = getClassRoute(aClass);
        return toMethod(getHttpMethod(method)) + "  " + toPath(classRoutes, getPath(method));
    }

    private Set<String> getHttpMethod(AnnotatedElement annotatedElement) {
        try {
            final GetMapping getMapping = AnnotationUtils.findAnnotation(annotatedElement, (Class<GetMapping>) classLoader.loadClass(GetMapping.class.getName()));
            if (getMapping != null) {
                return Sets.newHashSet(HttpMethod.GET.name());
            }

            final PostMapping postMapping = AnnotationUtils.findAnnotation(annotatedElement, (Class<PostMapping>) classLoader.loadClass(PostMapping.class.getName()));
            if (postMapping != null) {
                return Sets.newHashSet(HttpMethod.POST.name());
            }

            final DeleteMapping deleteMapping = AnnotationUtils.findAnnotation(annotatedElement, (Class<DeleteMapping>) classLoader.loadClass(DeleteMapping.class.getName()));
            if (deleteMapping != null) {
                return Sets.newHashSet(HttpMethod.DELETE.name());
            }

            final PatchMapping patchMapping = AnnotationUtils.findAnnotation(annotatedElement, (Class<PatchMapping>) classLoader.loadClass(PatchMapping.class.getName()));
            if (patchMapping != null) {
                return Sets.newHashSet(HttpMethod.PATCH.name());
            }

            final PutMapping putMapping = AnnotationUtils.findAnnotation(annotatedElement, (Class<PutMapping>) classLoader.loadClass(PutMapping.class.getName()));
            if (putMapping != null) {
                return Sets.newHashSet(HttpMethod.PUT.name());
            }

            final Class<RequestMapping> requestMappingClass = (Class<RequestMapping>) classLoader.loadClass(RequestMapping.class.getName());
            final RequestMapping requestMapping = AnnotationUtils.findAnnotation(annotatedElement, requestMappingClass);
            if (requestMapping != null) {
                return getRequestMappingMethod(requestMapping);
            }
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }

    private Set<String> getRequestMappingMethod(Annotation annotation) {
        Thread.currentThread().setContextClassLoader(classLoader);

        try {
            List<RequestMethod> value = JSON.parseArray(JSON.toJSONString(annotation.annotationType().getMethod(REQUEST_METHOD).invoke(annotation)), RequestMethod.class);
            if (!CollectionUtils.isEmpty(value)) {
                return Sets.newHashSet(value).stream().map(RequestMethod::name).collect(Collectors.toSet());
            }
            return Sets.newHashSet(HttpMethod.GET.name());
        } catch (Exception e) {
            log.error(e);
            return Sets.newHashSet(HttpMethod.GET.name());
        }
    }

    private Set<String> getClassRoute(Class<?> aClass) {
        return getPath(aClass);
    }

    private Set<String> getPath(AnnotatedElement annotatedElement) {
        try {
            final GetMapping getMapping = AnnotationUtils.findAnnotation(annotatedElement, (Class<GetMapping>) classLoader.loadClass(GetMapping.class.getName()));
            if (getMapping != null) {
                return doGetPath(getMapping);
            }

            final PostMapping postMapping = AnnotationUtils.findAnnotation(annotatedElement, (Class<PostMapping>) classLoader.loadClass(PostMapping.class.getName()));
            if (postMapping != null) {
                return doGetPath(postMapping);
            }

            final DeleteMapping deleteMapping = AnnotationUtils.findAnnotation(annotatedElement, (Class<DeleteMapping>) classLoader.loadClass(DeleteMapping.class.getName()));
            if (deleteMapping != null) {
                return doGetPath(deleteMapping);
            }

            final PatchMapping patchMapping = AnnotationUtils.findAnnotation(annotatedElement, (Class<PatchMapping>) classLoader.loadClass(PatchMapping.class.getName()));
            if (patchMapping != null) {
                return doGetPath(patchMapping);
            }

            final PutMapping putMapping = AnnotationUtils.findAnnotation(annotatedElement, (Class<PutMapping>) classLoader.loadClass(PutMapping.class.getName()));
            if (putMapping != null) {
                return doGetPath(putMapping);
            }

            final Class<RequestMapping> requestMappingClass = (Class<RequestMapping>) classLoader.loadClass(RequestMapping.class.getName());
            final RequestMapping requestMapping = AnnotationUtils.findAnnotation(annotatedElement, requestMappingClass);
            if (requestMapping != null) {
                return doGetPath(requestMapping);
            }

            if (annotatedElement instanceof Method) {
                return Sets.newHashSet("/" + ((Method) annotatedElement).getName());
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Set<String> doGetPath(Annotation annotation) {
        try {
            String[] value = (String[]) annotation.annotationType().getMethod(METHOD_VALUE).invoke(annotation);
            String[] path = (String[]) annotation.annotationType().getMethod(PATH_METHOD).invoke(annotation);

            return Sets.newHashSet(ArrayUtils.isEmpty(value) ? path : value);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String toPath(Set<String> classRotes, Set<String> value) {
        if (value == null) {
            return null;
        }
        final StringBuilder builder = new StringBuilder();
        if (!CollectionUtils.isEmpty(classRotes)) {
            for (String methodPath : value) {
                builder.append(methodPath);
                builder.append(" ");
            }
            return builder.toString().trim();
        }

        for (String methodPath : value) {
            for (String classRote : classRotes) {
                classRote = processClassRoute(classRote);
                if (methodPath.startsWith("/")) {
                    methodPath = methodPath.substring(1);
                }
                builder.append(classRote);
                builder.append(methodPath);
                builder.append(" ");
            }
        }
        return builder.toString().trim();
    }

    private String processClassRoute(String classRote) {
        if (!classRote.endsWith("/")) {
            classRote = classRote + "/";
        }
        return classRote;
    }

    private String toMethod(Set<String> httpMethod) {
        if (CollectionUtils.isEmpty(httpMethod)) {
            return HttpMethod.GET.name();
        }
        return String.join(" / ", httpMethod);
    }

    @Override
    protected String getParameterName(Parameter parameter) {
        String name = parameter.getName();
        final Annotation[] annotations = parameter.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotations.getClass().getName().equalsIgnoreCase(REQUESTPARAM_CLASS_NAME)) {
                name = ((RequestParam) annotation).name();
            }
            if (annotations.getClass().getName().equalsIgnoreCase(REQUESTBODY_CLASS_NAME)) {
                name += "说明: body传入";
            }
        }

        return name;
    }

    @Override
    protected boolean isNotTargetMethod(Method method) {
        return !isMappingClassPresent(method);
    }

    private boolean isMappingClassPresent(AnnotatedElement annotatedElement) {
        for (Map.Entry<String, Class<? extends Annotation>> stringClassEntry : classLoaderMappings.entrySet()) {
            if (annotatedElement.getAnnotation(stringClassEntry.getValue()) != null) {
                return true;
            }
        }
        return false;
    }
}

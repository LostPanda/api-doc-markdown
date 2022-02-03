package top.kidhx.apidoc.bo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author HX
 * @date 2022/1/30
 */
@Data
@Accessors(chain = true)
public class ClassMeta {
    /**
     * class name
     */
    private String className;
    /**
     * class type
     */
    private Class<?> classType;
    /**
     * class description
     */
    private String desc;
    /**
     * class field metadata
     */
    private List<FieldMeta> classFieldMetas;
    /**
     * api metadata
     */
    private List<Api> apis;
    /**
     * generic type info
     */
    private List<ClassMeta> genericTypes;
}

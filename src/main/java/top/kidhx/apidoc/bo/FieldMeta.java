package top.kidhx.apidoc.bo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author HX
 * @date 2022/1/30
 */
@Data
@Accessors(chain = true)
public class FieldMeta {
    /**
     * field name
     */
    private String name;
    /**
     * field type info
     */
    private ClassMeta type;
    /**
     * field type name
     */
    private String typeName;
    /**
     * field comment
     */
    private String desc;
    /**
     * field restrict(spring validation/hibernate)
     */
    private String restriction;
}

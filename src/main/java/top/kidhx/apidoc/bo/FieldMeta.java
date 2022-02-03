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

    private String name;

    private ClassMeta type;

    private String typeName;

    private String desc;

    private String restriction;
}

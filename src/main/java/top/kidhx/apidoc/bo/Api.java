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
public class Api {
    /**
     * API name
     */
    private String name;
    /**
     * description
     */
    private String desc;
    /**
     * return value metas
     */
    private FieldMeta returnValue;
    /**
     * parameter metas
     */
    private List<FieldMeta> parameters;
}

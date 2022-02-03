package top.kidhx.apidoc.bo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author HX
 * @date 2022/1/30
 */
@Data
@Accessors(chain = true)
public class Comment {
    /**
     * comment value
     */
    private String value;
}

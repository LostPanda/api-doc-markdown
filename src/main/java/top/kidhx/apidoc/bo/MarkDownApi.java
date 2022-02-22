package top.kidhx.apidoc.bo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author HX
 * @date 2022/1/30
 */
@Data
@Accessors(chain = true)
public class MarkDownApi {
    /**
     * file name
     */
    private String name;
    /**
     * md5 key
     */
    private String md5;
    /**
     * doc content
     */
    private String content;
}

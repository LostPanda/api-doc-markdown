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
    private String name;

    private String md5;

    private String content;
}

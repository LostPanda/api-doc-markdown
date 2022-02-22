package top.kidhx.apidoc.bo;

import com.google.common.collect.Maps;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @author HX
 * @date 2022/1/30
 */
@Data
@Accessors(chain = true)
public class MethodComment extends Comment {
    /**
     * parameter comments
     */
    private Map<String, Comment> parameterComment;
    /**
     * method return comment
     */
    private Comment returnComment;

    public MethodComment() {
        this.parameterComment = Maps.newHashMap();
        this.returnComment = new Comment();
    }
}

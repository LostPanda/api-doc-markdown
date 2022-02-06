package top.kidhx.apidoc.checkinterface;

import top.kidhx.apidoc.bo.ClassMeta;
import top.kidhx.apidoc.bo.FieldMeta;

import java.util.List;
import java.util.Map;

/**
 * @author HX
 * @date 2022/2/5
 */
public interface TestInterface {

    /**
     * 测试接口
     * @param metas 参数
     * @return 返回
     */
    ClassMeta  foundMetas(Map<Long, Long> metas);
}

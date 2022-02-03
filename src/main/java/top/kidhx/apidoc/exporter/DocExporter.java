package top.kidhx.apidoc.exporter;

import top.kidhx.apidoc.bo.MarkDownApi;

import java.util.List;

/**
 * @author HX
 * @date 2022/1/31
 */
public interface DocExporter {

    /**
     * export markdown api doc
     *
     * @param apiDocs generated api doc list
     */
    void exportApiDoc(List<MarkDownApi> apiDocs);
}

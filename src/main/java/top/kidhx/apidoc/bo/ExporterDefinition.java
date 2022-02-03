package top.kidhx.apidoc.bo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @author HX
 * @date 2022/1/31
 */
@Data
@Accessors(chain = true)
public class ExporterDefinition {

    /**
     * exporter name
     */
    String exporterName;
    /**
     * parameter definition from maven configuration
     */
    Map<String, String> parameters;
}

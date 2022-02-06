package top.kidhx.apidoc.exporter;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import top.kidhx.apidoc.bo.MarkDownApi;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

/**
 * @author HX
 * @date 2022/1/31
 */
public class FileDocExporter implements DocExporter {
    private String outputPath;

    private Log log;

    public FileDocExporter(Log log) {
        this.log = log;
    }

    @Override
    public void exportApiDoc(List<MarkDownApi> apiDocs) {
        if (StringUtils.isBlank(this.outputPath)) {
            this.outputPath = this.getClass().getResource(File.separator).getPath();
        }

        for (MarkDownApi apiDoc : apiDocs) {
            File file = new File(outputPath + apiDoc.getName() + ".md");
            try (PrintWriter printWriter = new PrintWriter(file)) {
                printWriter.write(apiDoc.getContent());
                log.info("file exporter:" + apiDoc.getName() + "export success, path: " + file.getAbsolutePath());
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    public String getOutputPath() {
        return this.outputPath;
    }

    public void setOutputPath(String outputPath) {
        validatePath(outputPath);
        this.outputPath = outputPath;
    }

    private void validatePath(String outputPath) {
        if (StringUtils.isBlank(this.outputPath)) {
            throw new IllegalArgumentException("outputPath reject empty value");
        }

        final File file = new File(this.outputPath);

        if (file.isFile()) {
            throw new IllegalArgumentException("outputPath must be a directory");
        }

        if (!file.exists()) {
            final boolean mkdirs = file.mkdirs();
        }
    }
}

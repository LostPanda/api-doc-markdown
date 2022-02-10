# api-doc-markdown
a maven plugin to generate markdown api files
## 介绍
一款基于java parser静态代码扫描的api markdown文档生成maven插件。直接读取类/方法级别的注释生成文档，免去接口另加注解的繁琐。插件会扫描所在模块拥有指定前缀的所有接口(interface)，并生成文档
并导出到本地文件.

## 使用
**示例**
```xml
<plugins>
            <!--必要插件，否则不能获取参数名称-->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.7.0</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                    <compilerArgument>-parameters</compilerArgument>
                </configuration>
            </plugin>
            <plugin>
                <groupId>com.github.kidhx</groupId>
                <artifactId>api-doc-markdown</artifactId>
                <version>1.0-SNAPSHOT</version>
                <executions>
                    <execution>
                        <id>generate-api-doc</id>
                        <goals>
                            <goal>generate-api-doc</goal>
                        </goals>
                        <!--执行实际默认绑定到deploy， 可根据自己需要修改-->
                        <phase>compile</phase>
                    </execution>
                </executions>
                <configuration>
                    <!--需要扫描的包前缀，必须已'.'隔开-->
                <configuration>
                    <packageName>top.kidhx.apidoc.checkinterface</packageName>
                    <exporters>
                        <exporter>
                            <!--文件类型导出-->
                            <exporterName>file</exporterName>
                            <parameters>
                              <!--不配置默认导出到class path根目录-->
                              <outputPath>/Users/xxx/myApiDoc<outputPath>
                            </parameters>
                        </exporter>
                    </exporters>
                </configuration>
            </plugin>
        </plugins>
```

## 注意事项
1. 注释依赖.java静态扫描，jar包中由于不打包注释，无法读取注释
2. 注释结构化依赖于通用的注释规则。对于方法级别`@param` 会读取到对应方法的说明中，而`@return`会结构化到返回说明，其他则读取到api层面的说明中。

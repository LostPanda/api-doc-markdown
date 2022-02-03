package top.kidhx.apidoc.sourcehandler;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import top.kidhx.apidoc.bo.Comment;
import top.kidhx.apidoc.bo.MethodComment;
import top.kidhx.apidoc.bo.enums.CommentType;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author HX
 * @date 2022/1/31
 */
public class SourceCodeReader {

    private static final Pattern REGEX = Pattern.compile("\\s*|\t|\r|\n");

    private static String handleFieldComment(String content) {
        if (content.contains("*")) {
            content = content.substring(content.indexOf("*") + 1).trim();
        }
        final Matcher m = REGEX.matcher(content);
        content = m.replaceAll("");
        return content;
    }

    public Map<String, Comment> resolveComment(File file, CommentType commentType) throws Exception {
        final CompilationUnit compilationUnit = JavaParser.parse(file);
        switch (commentType) {
            case API:
                return parseMethodComment(compilationUnit);
            case FIELD:
                return parseFieldComment(compilationUnit);
            case ENUM:
                return parseEnumComment(compilationUnit);
            default:
        }
        return Maps.newHashMap();
    }

    private Map<String, Comment> parseEnumComment(CompilationUnit compilationUnit) {
        final EnumCommentVisitor enumCommentVisitor = new EnumCommentVisitor();
        final HashMap<String, Comment> result = Maps.newHashMap();
        enumCommentVisitor.visit(compilationUnit, result);
        return result;
    }

    private Map<String, Comment> parseFieldComment(CompilationUnit compilationUnit) {
        final FieldCommentVisitor fieldCommentVisitor = new FieldCommentVisitor();
        Map<String, Comment> result = Maps.newHashMap();
        fieldCommentVisitor.visit(compilationUnit, result);
        return result;
    }

    private Map<String, Comment> parseMethodComment(CompilationUnit compilationUnit) {
        final MethodCommentVisitor methodCommentVisitor = new MethodCommentVisitor();
        Map<String, String> result = Maps.newHashMap();
        methodCommentVisitor.visit(compilationUnit, result);
        return toMethodComments(result);
    }

    private Map<String, Comment> toMethodComments(Map<String, String> commentMap) {
        Map<String, Comment> result = Maps.newHashMap();
        commentMap.forEach((k, v) -> {
            if (StringUtils.isNotBlank(v)) {
                result.put(k, toMethodComment(v));
            }
        });
        return result;
    }

    private Comment toMethodComment(String originalComment) {
        final MethodComment methodComment = new MethodComment();
        final List<String> split = Arrays.stream(originalComment.split("\n"))
                .filter(a -> a.trim().length() > 0)
                .collect(Collectors.toList());
        final StringBuilder builder = new StringBuilder();
        for (String s : split) {
            if (s.contains("@param")) {
                final Comment comment = new Comment();
                final String[] line = s.split("@param");
                final String[] filteredLine = line[1].trim().replaceAll("\\s{1,}", " ").split(" ");
                if (filteredLine.length > 1) {
                    methodComment.getParameterComment().put(filteredLine[0], comment.setValue(filteredLine[1]));
                }
            } else if (s.contains("@return")) {
                final Comment comment = new Comment();
                final String[] line = s.split("@return");
                if (line.length > 1) {
                    methodComment.setReturnComment(comment.setValue(line[1].trim()));
                }
            } else {
                if (s.contains("*")) {
                    builder.append(s.substring(s.indexOf("*") + 1));
                } else {
                    builder.append(s.replace("\n", ","));
                }
                builder.append("\n");
            }
        }
        methodComment.setValue(builder.toString());
        return methodComment;
    }

    public String resolveClassComment(File file) throws FileNotFoundException {
        final CompilationUnit unit = JavaParser.parse(file);
        return parseClassComment(unit);
    }

    private String parseClassComment(CompilationUnit unit) {
        final ClassCommentVisitor classCommentVisitor = new ClassCommentVisitor();
        final List<String> result = Lists.newArrayList();
        classCommentVisitor.visit(unit, result);
        return result.size() > 0 ? result.get(0) : null;
    }

    private static class MethodCommentVisitor extends VoidVisitorAdapter<Map<String, String>> {
        @Override
        public void visit(MethodDeclaration n, Map<String, String> commentMap) {
            super.visit(n, commentMap);
            final Optional<com.github.javaparser.ast.comments.Comment> comment = n.getComment();
            comment.ifPresent(c -> commentMap.put(n.getNameAsString(), c.getContent()));
        }
    }

    private static class FieldCommentVisitor extends VoidVisitorAdapter<Map<String, Comment>> {
        @Override
        public void visit(FieldDeclaration n, Map<String, Comment> commentMap) {
            super.visit(n, commentMap);
            final Optional<com.github.javaparser.ast.comments.Comment> comment = n.getComment();
            comment.ifPresent(c -> commentMap.put(n.getVariables().get(0).getNameAsString(), new Comment().setValue(handleFieldComment(c.getContent()))));
        }
    }

    private static class EnumCommentVisitor extends VoidVisitorAdapter<Map<String, Comment>> {
        @Override
        public void visit(EnumConstantDeclaration n, Map<String, Comment> commentMap) {
            super.visit(n, commentMap);
            final Optional<com.github.javaparser.ast.comments.Comment> comment = n.getComment();
            comment.ifPresent(c -> commentMap.put(n.getNameAsString(), new Comment().setValue(handleFieldComment(c.getContent()))));
        }
    }

    private static class ClassCommentVisitor extends VoidVisitorAdapter<List<String>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration n, List<String> result) {
            super.visit(n, result);
            final Optional<com.github.javaparser.ast.comments.Comment> comment = n.getComment();
            if (comment.isPresent()) {
                String c = toClassComment(comment.get());
                if (StringUtils.isNotBlank(c)) {
                    result.add(c);
                }
            }
        }

        private String toClassComment(com.github.javaparser.ast.comments.Comment comment) {
            String desc = "";
            String author = "";
            String date = "";
            String content = comment.getContent();

            final StringBuilder builder = new StringBuilder();
            for (String s : Lists.newArrayList(content.split("\n"))) {
                if (s.contains("@author")) {
                    author = s.split("@author")[1].trim();
                } else if (s.contains("@date")) {
                    date = s.split("@date")[1].trim();
                } else {
                    s = s.replace("*", "").replaceAll(" ", "");
                    if (StringUtils.isNotBlank(s)) {
                        if (StringUtils.isNotBlank(builder.toString().trim())) {
                            builder.append(",");
                        }
                        builder.append(s);
                    }
                }
            }

            if (StringUtils.isBlank(builder.toString().trim())) {
                return null;
            }

            final StringBuilder resultBuilder = new StringBuilder();
            resultBuilder.append(builder.toString());
            if (StringUtils.isNotBlank(author)) {
                resultBuilder.append("-");
                resultBuilder.append(author);
            }
            if (StringUtils.isNotBlank(date)) {
                resultBuilder.append("-");
                resultBuilder.append(date);
            }
            return resultBuilder.toString()
                    .replaceAll(File.separator, "")
                    .replaceAll(" ", "");
        }
    }
}

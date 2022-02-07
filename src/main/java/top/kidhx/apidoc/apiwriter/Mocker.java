package top.kidhx.apidoc.apiwriter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.util.CollectionUtils;
import top.kidhx.apidoc.bo.ClassMeta;
import top.kidhx.apidoc.bo.FieldMeta;

import java.time.temporal.Temporal;
import java.util.*;

/**
 * @author HX
 * @date 2022/1/31
 */
public class Mocker {

    private final List<String> stringMockDictionary = ImmutableList.of("naruto", "one piece", "world of warcraft", "lol");

    private final ClassLoader loader;

    public Mocker(ClassLoader classLoader) {
        this.loader = classLoader;
    }

    public String mock(FieldMeta fieldMeta) throws ClassNotFoundException {
        return JSON.toJSONString(fieldMeta.getType() == null ? "%" : mockValue(fieldMeta.getType()), SerializerFeature.PrettyFormat);
    }

    private Map<Object, Object> mockPojo(ClassMeta classMeta) throws ClassNotFoundException {
        Map<Object, Object> object = Maps.newHashMap();

        if (classMeta != null) {
            final List<FieldMeta> classFieldMetas = classMeta.getClassFieldMetas();
            if (!CollectionUtils.isEmpty(classFieldMetas)) {
                for (FieldMeta classFieldMeta : classFieldMetas) {
                    final Object value = mockValue(classFieldMeta.getType() == null || classFieldMeta.getType().getClassType() == null ?
                            new ClassMeta().setClassName(classFieldMeta.getTypeName())
                            : classFieldMeta.getType()
                    );
                    object.put(classFieldMeta.getName(), Optional.ofNullable(value).orElse(classFieldMeta.getType()));
                }
            } else {
                object.put("cycle reference checked, object name(should be self)", classMeta.getClassName());
            }
        }
        return object;
    }

    private Object mockValue(ClassMeta classMeta) throws ClassNotFoundException {
        Class<?> parameterType = classMeta.getClassType();
        if (parameterType == null) {
            parameterType = loader.loadClass(classMeta.getClassName());
        }

        if (parameterType == boolean.class || parameterType == Boolean.class) {
            return mockBoolean();
        } else if (parameterType == byte.class || parameterType == Byte.class) {
            return mockByte();
        } else if (parameterType == short.class || parameterType == Short.class) {
            return mockShort();
        } else if (parameterType == int.class || parameterType == Integer.class) {
            return mockInteger();
        } else if (parameterType == long.class || parameterType == Long.class) {
            return mockLong();
        } else if (parameterType == float.class || parameterType == Float.class) {
            return mockFloat();
        } else if (parameterType == double.class || parameterType == Double.class) {
            return mockDouble();
        } else if (parameterType == char.class || parameterType == Character.class) {
            return mockCharacter();
        } else if(parameterType == String.class){
            return mockString();
        }else if (parameterType == Date.class || Temporal.class.isAssignableFrom(parameterType) || java.sql.Date.class == parameterType) {
            return "2022-01-01";
        } else if (Collection.class.isAssignableFrom(parameterType)) {
            return mockCollection(classMeta);
        } else if (Map.class.isAssignableFrom(parameterType)) {
            return mockMap(classMeta);
        }
        return mockPojo(classMeta);

    }

    private Object mockMap(ClassMeta classMeta) throws ClassNotFoundException {
        final List<ClassMeta> genericTypes = classMeta.getGenericTypes();
        Map<Object, Object> result = Maps.newHashMap();
        if (genericTypes == null || genericTypes.size() < 2) {
            result.put("unknown", "unknown");
            return result;
        }

        result.put(mockValue(genericTypes.get(0)), mockValue(genericTypes.get(1)));
        return result;
    }

    private Object mockCollection(ClassMeta classMeta) throws ClassNotFoundException {
        final List<Object> objects = Lists.newArrayList();
        final List<ClassMeta> genericTypes = classMeta.getGenericTypes();
        if (CollectionUtils.isEmpty(genericTypes)) {
            objects.add("unknown");
            return objects;
        }
        objects.add(mockValue(genericTypes.get(0)));
        return objects;
    }

    private Byte mockByte() {
        final byte[] bytes = new byte[1];
        new Random().nextBytes(bytes);
        return bytes[0];
    }

    private Short mockShort() {
        return new Short(new Random().nextInt(100) + "");
    }

    private Float mockFloat() {
        return new Random().nextFloat();
    }

    private Boolean mockBoolean() {
        return new Random().nextBoolean();
    }

    private Character mockCharacter() {
        return Math.random() < .5 ? 'a' : 'd';
    }

    public Double mockDouble() {
        return new Random().doubles(1.0, 100000.0).findFirst().getAsDouble();
    }

    public Integer mockInteger() {
        return new Random().nextInt(1000);
    }

    public Long mockLong() {
        return new Random().longs(1, 100000).findFirst().getAsLong();
    }

    public String mockString() {
        return stringMockDictionary.get(((Double) Math.floor(Math.random() * stringMockDictionary.size())).intValue());
    }

}

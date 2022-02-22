package top.kidhx.apidoc.bo;

import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Objects;

/**
 * custom parameterizedTypeImpl - to set type name
 * @author HX
 * @date 2022/1/30
 */
public class ParameterizedTypeImpl implements ParameterizedType {
    private final Type[] actualTypeArguments;
    private final Class<?> rawType;
    private final Type ownerType;
    private String name;

    private ParameterizedTypeImpl(Class<?> rawType, Type[] actualTypeArguments, Type ownerType) {
        this.actualTypeArguments = actualTypeArguments;
        this.rawType = rawType;
        this.ownerType = ownerType;
    }

    public static ParameterizedTypeImpl make(Class<?> var0, Type[] var1, Type var2) {
        return new ParameterizedTypeImpl(var0, var1, var2);
    }

    private void validateConstructorArguments() {
        final TypeVariable<? extends Class<?>>[] var1 = this.rawType.getTypeParameters();
        if (var1.length != this.actualTypeArguments.length) {
            throw new MalformedParameterizedTypeException();
        } else {
            for (int var2 = 0; var2 < this.actualTypeArguments.length; ++var2) {

            }
        }
    }

    @Override
    public Type[] getActualTypeArguments() {
        return this.actualTypeArguments;
    }

    @Override
    public Type getRawType() {
        return this.rawType;
    }

    @Override
    public Type getOwnerType() {
        return this.ownerType;
    }

    @Override
    public boolean equals(Object var1) {
        if (var1 instanceof ParameterizedType) {
            final ParameterizedType var2 = (ParameterizedType) var1;
            if ((this == var2)) {
                return true;
            } else {
                final Type var3 = var2.getOwnerType();
                final Type var4 = var2.getRawType();
                return Objects.equals(this.ownerType, var3) && Objects.equals(this.rawType, var4) && Arrays.equals(this.actualTypeArguments, var2.getActualTypeArguments());
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.actualTypeArguments) ^ Objects.hashCode(this.ownerType) ^ Objects.hashCode(this.rawType);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (ownerType != null) {
            if (ownerType instanceof Class)
                sb.append(((Class) ownerType).getName());
            else
                sb.append(ownerType.toString());

            sb.append("$");

            if (ownerType instanceof sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl) {
                // Find simple name of nested type by removing the
                // shared prefix with owner.
                sb.append(rawType.getName().replace(((ParameterizedTypeImpl) ownerType).rawType.getName() + "$",
                        ""));
            } else
                sb.append(rawType.getSimpleName());
        } else
            sb.append(rawType.getName());

        if (actualTypeArguments != null &&
                actualTypeArguments.length > 0) {
            sb.append("<");
            boolean first = true;
            for (Type t : actualTypeArguments) {
                if (!first)
                    sb.append(", ");
                sb.append(t.getTypeName());
                first = false;
            }
            sb.append(">");
        }

        return sb.toString();
    }

    @Override
    public String getTypeName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

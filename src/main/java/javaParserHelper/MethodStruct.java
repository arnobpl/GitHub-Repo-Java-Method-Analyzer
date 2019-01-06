package javaParserHelper;

import java.util.List;

/**
 * This class is a structure for storing Java method signature. For example: void DoTask(arg a, arg b).
 */
public class MethodStruct {
    //region variables

    public final String parentNodes;
    public final String returnType;
    public final String methodName;
    public final List<String> parameters;

    //endregion


    //region constructors

    public MethodStruct(String parentNodes, String returnType, String methodName, List<String> parameters) {
        this.parentNodes = parentNodes;
        this.returnType = returnType;
        this.methodName = methodName;
        this.parameters = parameters;
    }

    //endregion


    //region methods

    private String getParametersAsString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String param : parameters) {
            stringBuilder.append(param);
            stringBuilder.append(",");
        }

        if (stringBuilder.length() > 1)
            stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length());

        return stringBuilder.toString();
    }

    //endregion


    //region override methods

    @Override
    public String toString() {
        return (parentNodes + ":" + returnType + ":" + methodName + ":" + getParametersAsString());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MethodStruct))
            return false;

        return this.toString().equals(obj.toString());
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    //endregion
}

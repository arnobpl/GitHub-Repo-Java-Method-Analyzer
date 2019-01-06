package javaParserHelper;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This class creates method signature list from a Java file.
 */
public class JavaMethodVisitor {
    //region static methods

    /**
     * This method returns a list of method signatures of a given Java source stream.
     *
     * @param reader Java source stream
     * @return a list of method signatures of a given Java source stream
     * @throws ParseProblemException if parsing problem occurs
     */
    public static List<MethodStruct> getMethodSignatures(Reader reader) throws ParseProblemException {
        CompilationUnit compilationUnit = JavaParser.parse(reader);

        List<MethodStruct> methodSignatures = new LinkedList<>();
        compilationUnit.accept(new MethodVisitor(), methodSignatures);

        return methodSignatures;
    }

    //endregion

    private static class MethodVisitor extends VoidVisitorAdapter<List<MethodStruct>> {
        //region override methods

        @Override
        public void visit(MethodDeclaration methodDeclaration, List<MethodStruct> methodSignatures) {
            super.visit(methodDeclaration, methodSignatures);
            methodSignatures.add(getMethodSignature(methodDeclaration));
        }

        //endregion


        //region helper methods

        private static MethodStruct getMethodSignature(MethodDeclaration methodDeclaration) {
            return new MethodStruct(
                    getParents(methodDeclaration),
                    methodDeclaration.getTypeAsString(),
                    methodDeclaration.getNameAsString(),
                    getParameterAsStringList(methodDeclaration.getParameters()));
        }

        private static String getParents(MethodDeclaration methodDeclaration) {
            int parentCount = 0;
            //StringBuilder parents = new StringBuilder();

            Node currentNode = methodDeclaration;
            while (currentNode.getParentNode().isPresent()) {
                currentNode = currentNode.getParentNode().get();

                //TODO: how to identify every parent node; (mitigated by parent node count)
                parentCount++;
            }

            return Integer.toString(parentCount);
            //return parents.toString();
        }

        private static List<String> getParameterAsStringList(NodeList<Parameter> parameters) {
            List<String> parameterStrings = new ArrayList<>(parameters.size());

            for (Parameter param : parameters) {
                parameterStrings.add(param.getTypeAsString());
            }

            return parameterStrings;
        }

        //endregion
    }
}

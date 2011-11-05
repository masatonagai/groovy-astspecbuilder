/*
 * Copyright 2011 Nagai Masato
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package astspecbuilder

import static org.objectweb.asm.Opcodes.*

import java.util.Map

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.EnumConstantClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.GenericsType
import org.codehaus.groovy.ast.ImportNode
import org.codehaus.groovy.ast.InnerClassNode
import org.codehaus.groovy.ast.InterfaceHelperClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.MixinNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.PackageNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.AnnotationConstantExpression
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ArrayExpression
import org.codehaus.groovy.ast.expr.AttributeExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.BitwiseNegationExpression
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.ast.expr.CastExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ClosureListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.FieldExpression
import org.codehaus.groovy.ast.expr.GStringExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.MethodPointerExpression
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression
import org.codehaus.groovy.ast.expr.NotExpression
import org.codehaus.groovy.ast.expr.PostfixExpression
import org.codehaus.groovy.ast.expr.PrefixExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.RangeExpression
import org.codehaus.groovy.ast.expr.SpreadExpression
import org.codehaus.groovy.ast.expr.SpreadMapExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.expr.TernaryExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.ast.expr.UnaryMinusExpression
import org.codehaus.groovy.ast.expr.UnaryPlusExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.AssertStatement
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.BreakStatement
import org.codehaus.groovy.ast.stmt.CaseStatement
import org.codehaus.groovy.ast.stmt.CatchStatement
import org.codehaus.groovy.ast.stmt.ContinueStatement
import org.codehaus.groovy.ast.stmt.DoWhileStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ForStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.SwitchStatement
import org.codehaus.groovy.ast.stmt.SynchronizedStatement
import org.codehaus.groovy.ast.stmt.ThrowStatement
import org.codehaus.groovy.ast.stmt.TryCatchStatement
import org.codehaus.groovy.ast.stmt.WhileStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.syntax.Token

/**
 * <p>This class provides methods to generate the AST spec DSL for
 * {@link AstBuilder#buildFromSpec}.
 * </p>
 * 
 * @author Nagai Masato
 */
class AstSpecBuilder {
    String indent = ' ' * 4
    
    private int depth = 0
    private StringBuilder spec
    
    private def addLine(String line) {
        spec << (indent * depth) + line + '\n'
    }
    private def addBlock(String name, Object body) {
        addLine(name + ' {')
        depth++
        addNode(body)
        depth--
        addLine('}')
    }
    private def addExpression(String exp) {
        addLine("expression.add(${exp})")    
    }
    private static class Str {
        Object value        
        /**
         * Use create() instead.
         */
        Str(Object value) {
            this.value = value
        }
        @Override
        String toString() {
            "'${value}'"
        }
        static Str create(Object value) {
            null != value ? new Str(value) : null
        }
    }
    private static class ClassName {
        ClassNode value
        boolean asStr
        /**
         * Use create() instead.
         */
        ClassName(Object value) {
            this.value = value
        }        
        ClassName(Map args) {
            this.value = args.value
            this.asStr = args.asStr
        }
        @Override
        String toString() {
            // [LclassName; -> className[] 
            def name = value.name.replaceFirst('\\[L(.+);', '$1[]')
            if (asStr) { 
                Str.create(name)
            } else {
                "${name}.class"
            }
        }
        static ClassName create(Object value) {
            null != value ? new ClassName(value) : null
        }
        static ClassName create(Map args) {
            args ? new ClassName(args) : null
        }
        
    }
    private static class Modifiers {
        static Map<Integer, String> nameDict = [
            (ACC_PUBLIC): "ACC_PUBLIC",
            (ACC_PRIVATE): "ACC_PRIVATE",
            (ACC_PROTECTED): "ACC_PROTECTED",
            (ACC_STATIC): "ACC_STATIC",
            (ACC_FINAL): "ACC_FINAL",
            (ACC_SUPER): "ACC_SUPER",
            (ACC_SYNCHRONIZED): "ACC_SYNCHRONIZED",
            (ACC_VOLATILE): "ACC_VOLATILE",
            (ACC_BRIDGE): "ACC_BRIDGE",
            (ACC_VARARGS): "ACC_VARARGS",
            (ACC_TRANSIENT): "ACC_TRANSIENT",
            (ACC_NATIVE): "ACC_NATIVE",
            (ACC_INTERFACE): "ACC_INTERFACE",
            (ACC_ABSTRACT): "ACC_ABSTRACT",
            (ACC_STRICT): "ACC_STRICT",
            (ACC_SYNTHETIC): "ACC_SYNTHETIC",
            (ACC_ANNOTATION): "ACC_ANNOTATION",
            (ACC_ENUM): "ACC_ENUM",
        ] 
        int value    
        /**
         * Use create() instead. 
         */
        private Modifiers(int value) {
            this.value = value
        }
        @Override
        String toString() {
            def s
            nameDict.each { k, v ->
                if ((value & k) != 0) {
                    if (s) {
                        s += (' | ' + v)
                    } else {
                        s = v 
                    }
                }
            }
            s
        }
        static Modifiers create(int value) {
            0 != value ? new Modifiers(value) : null
        }
    }
    private static class Label {
        String value
        Label(String value) {
            this.value = value
        }
        @Override
        String toString() {
            Str.create(value)
        }
        static Label create(String value) {
            null != value ? new Label(value) : null    
        }
    }
    private def addNode(Map args) {
        assert ['name', 'attr', 'child'].containsAll(args.keySet())
        if (!args.attr && !args.child) {
            if (null == args.child) {
                addLine("${args.name}()")
            } else {
                addBlock(args.name, []) 
            }
        } else {
            def s
            if (args.attr) {
                s = args.attr instanceof List ? 
                    "${args.name} ${args.attr.findAll{it && it.toString()}.join(', ')}" : 
                    "${args.name} ${args.attr}"
     	   } else {
                s = args.name
            }
            if (args.child) {
                addBlock(args.attr ? "${s}," : s, 
                    args.child instanceof Object[] ? 
                        args.child as List : args.child)
            } else {
                addLine(s) 
            }    
        }
    }
    private def addNode(ClosureListExpression node) {
       addNode(name: 'closureList', child: node.expressions) 
    }
    private def addNode(Collection nodes) {
        if (nodes) {
            for (node in nodes) {
                addNode(node)
            }
        }
    }
    private def addNode(String node) {
        addLine(node)
    }
    
    private def addDoesNotSupport(String s) {
        addLine("// The DSL does not support ${s}.")    
    }
    
    private static class Strings {
        List value    
        /**
         * Use create() instead.
         */
        private Strings(List value) {
            this.value = value
        }
        static Strings create(List value) {
            null != value ? new Strings(value) : null
        }
    }
    private def addNode(Strings node) {
        addNode(name: 'strings', child: node.value)    
    }
    
    private static class Values {
        List value
        /**
         * Use create() instead. 
         */
        private Values(List value) {
            this.value = value    
        }
        static Values create(List value) {
            null != value ? new Values(value) : value
        }
    }
    private def addNode(Values node) {
        addNode(name: 'values', child: node.value)
    }
    
    // statements
    private def addNode(AssertStatement node) {
        addNode(name: 'assertStatement', 
            child: [node.booleanExpression, node.messageExpression])
    }
    private def addNode(BlockStatement node) {
        addNode(name: 'block', child: node.statements)
    }
    private def addNode(BreakStatement node) {
        addNode(name: 'breakStatement', attr: Label.create(node.label))
    }
    private def addNode(CaseStatement node) {
        addNode(name: 'caseStatement', child: [node.expression, node.code])
    }
    private static class DefaultCase {
        BlockStatement value
        /**
         * Use create() instead.
         */
        private DefaultCase(BlockStatement value) {
            this.value = value
        }
        static DefaultCase create(BlockStatement value) {
            null != value ? new DefaultCase(value) : value   
        }
    }
    private def addNode(DefaultCase node) {
        addNode(name: 'defaultCase', child: node.value.statements)
    }
    private def addNode(CatchStatement node) {
        addNode(name: 'catchStatement', child: [node.variable, node.code])
    }
    private def addNode(ContinueStatement node) {
        addNode(name: 'continueStatement', child: Label.create(node.label))
    }
    private def addNode(DoWhileStatement node) {
        addDoesNotSupport("DoWhileStatement")
    }
    private def addNode(EmptyStatement node) {
        addNode(name: 'empty')
    }
    private def addNode(ExpressionStatement node) {
        addNode(name: 'expression', child: node.expression)
    }
    private def addNode(ForStatement node) {
        addNode(name: 'forStatement', 
            child: [node.variable, node.collectionExpression, node.loopBlock])
    }
    private def addNode(IfStatement node) {
        addNode(name: 'ifStatement', 
            child: [node.booleanExpression, node.ifBlock, node.elseBlock])
    }
    private def addNode(ReturnStatement node) {
        addNode(name: 'returnStatement', child: node.expression)
    }
    private def addNode(SynchronizedStatement node) {
        addNode(name: 'synchronizedStatement', 
            child: [node.expression, node.code])
    }
    private def addNode(SwitchStatement node) {
        addNode(name: 'switchStatement', 
            child: [node.expression, DefaultCase.create(node.defaultStatement), node.caseStatements])
    }
    private def addNode(ThrowStatement node) {
        addNode(name: 'throwStatement', child: node.expression)
    }
    private def addNode(TryCatchStatement node) {
        addNode(name: 'tryCatch', 
            child: [node.tryStatement, node.finallyStatement, node.catchStatements])
    }
    private def addNode(WhileStatement node) {
        addNode(name: 'whileStatement', 
            child: [node.booleanExpression, node.loopBlock])
    }
      // expressions
    private def addNode(ArgumentListExpression node) {
        addNode(name: 'argumentList', child: node.expressions)
    }
    private def addNode(ArrayExpression node) {
        addNode(name: 'array', 
            attr: ClassName.create(node.elementType),
            child: node.expressions)
    }
    private def addNode(BinaryExpression node) {
        addNode(name: 'binary', 
            child: [node.leftExpression, node.operation, node.rightExpression])
    }
    private def addNode(BitwiseNegationExpression node) {
        addNode(name: 'bitwiseNegation', child: node.expression)
    }
    private def addNode(BooleanExpression node) {
        addNode(name: 'booleanExpression', child: node.expression)
    }
    private def addNode(CastExpression node) {
        addNode(name: 'cast', attr: ClassName.create(node.type), child: node.expression)
    }
    private def addNode(ClassExpression node) {
        addNode(name: 'classExpression', attr: ClassName.create(node.type))
    }
    private def addNode(ClosureExpression node) {
        addNode(name: 'closure', child: [Parameters.create(node.parameters), node.code])
    }
    private def addNode(ConstantExpression node) {
        def attr 
        switch (node.type.name) {
            case "java.lang.String": attr = Str.create(node.value); break
            case "java.lang.Long": attr = "${node.value}L"; break
            case "java.math.BigDecimal":
            case "java.math.BigInteger": attr = "${node.value}G"; break
            case "java.lang.Float": attr = "${node.value}F"; break
            case "java.lang.Double": attr = "${node.value}D"; break
            default: attr = "${node.value}"
        }
        addNode(name: 'constant', attr: attr)
    }
    private def addNode(ConstructorCallExpression node) {
        addNode(name: 'constructorCall', 
            attr: ClassName.create(node.type), child: node.arguments)
    }
    private def addNode(DeclarationExpression node) {
        addNode(name: 'declaration', 
            child: [node.leftExpression, node.operation, node.rightExpression])    
    }
    private def addNode(ElvisOperatorExpression node) {
        addNode(name: 'elvisOperator',
            child: [node.booleanExpression, node.falseExpression])
    }
    private def addNode(EmptyExpression node) {
        addExpression("new ${EmptyExpression.class.name}()")
    }
    private def addNode(FieldExpression node) {
        addNode(name: 'field', child: node.field)
    }
    private def addNode(GStringExpression node) {
        addNode(name: 'gString', 
            attr: Str.create(node.text), 
            child: [Strings.create(node.strings), Values.create(node.values)])
    }
    private def addNode(ListExpression node) {
        addNode(name: 'list', child: node.expressions)
    }
    private def addNode(MapExpression node) {
        addNode(name: 'map', child: node.mapEntryExpressions)
    }
    private def addNode(MapEntryExpression node) {
        addNode(name: 'mapEntry', child: [node.keyExpression, node.valueExpression])
    }
    private def addNode(MethodCallExpression node) {
        addNode(name: 'methodCall', 
            child: [node.objectExpression, node.method, node.arguments])
    }
    private def addNode(MethodPointerExpression node) {
        addNode(name: 'methodPointer', child: [node.expression, node.methodName])
    }
    private def addNode(NamedArgumentListExpression node) {
        addNode(name: 'namedArgumentList', child: node.mapEntryExpressions)    
    }
    private def addNode(NotExpression node) {
        addNode(name: 'not', child: node.expression)
    }
    private def addNode(PostfixExpression node) {
        addNode(name: 'postfix', child: [node.expression, node.operation])
    }
    private def addNode(PrefixExpression node) {
        addNode(name: 'prefix', child: [node.operation, node.expression])
    }
    private def addNode(AnnotationConstantExpression node) {
        addNode(name: 'annotationConstant', child: node.value)
    }
    private def addNode(AttributeExpression node) {
        addNode(name: 'attribute', child: [node.objectExpression, node.property])
    }
    private def addNode(PropertyExpression node) {
        addNode(name: 'property', child: [node.objectExpression, node.property])
    }
    private def addNode(RangeExpression node) {
        addNode(name: 'range', child: [node.from, node.to, "inclusive ${node.inclusive}"])
    }
    private def addNode(SpreadExpression node) {
        addNode(name: 'spread', child: node.expression)
    }
    private def addNode(SpreadMapExpression node) {
        addNode(name: 'spreadMap', child: node.expression)
    }
    private def addNode(StaticMethodCallExpression node) {
        addNode(name: 'staticMethodCall', 
            attr: [ClassName.create(node.ownerType), Str.create(node.method)], 
            child: node.arguments)
    }
    private def addNode(TernaryExpression node) {
        addNode(name: 'ternary', 
            child: [node.booleanExpression, node.trueExpression, node.falseExpression])
    }
    private def addNode(TupleExpression node) {
        addNode(name: 'tuple', child: node.expressions)
    }
    private def addNode(UnaryMinusExpression node) {
        addNode(name: 'unaryMinus', child: node.expression)
    }
    private def addNode(UnaryPlusExpression node) {
        addNode(name: 'unaryPlus', child: node.expression)
    }
    private def addNode(VariableExpression node) {
        addNode(name: 'variable', attr: Str.create(node.text))
    }
      // annotated nodes
    private def addNode(EnumConstantClassNode node) {
        addDoesNotSupport('EnumConstantClassNode')
    }
    private def addNode(InterfaceHelperClassNode node) {
        addDoesNotSupport('InterfaceHelperClassNode')
    }
    private static class Interfaces {
        ClassNode[] value
        /**
         * Use create() instead.
         */
        private Interfaces(ClassNode[] value) {
            this.value = value
        }
        static Interfaces create(ClassNode[] value) {
            null != value ? new Interfaces(value) : value
        }
    }
    private def addNode(Interfaces node) {
        addNode(name: 'interfaces', child: node.value)
    }
    private def addNode(InnerClassNode node) {
        addNode(name: 'innerClass',
            attr: [Str.create(node.name), Modifiers.create(node.modifiers)],
            child: [node.outerClass, node.superClass, Interfaces.create(node.interfaces),
                Mixins.create(node.mixins)])
    }
    private static class Mixins {
        ClassNode[] value
        /**
         * Use create() instaed.
         */
        private Mixins(ClassNode[] value) {
            this.value = value
        }
        static Mixins create(ClassNode[] value) {
            null != value ? new Mixins(value) : value
        }
    }
    private def addNode(Mixins node) {
        addNode(name: 'mixins', child: node.value)        
    }
    private def addNode(MixinNode node) {
        addNode(name: 'mixin', 
            attr: [Str.create(node.name), Modifiers.create(node.modifiers)], 
            child: [node.superClass, Interfaces.create(node.interfaces)])
    }
    private def addNode(ClassNode node) {
        if (node.resolved) {
            addNode(name: 'classNode', attr: ClassName.create(node))
        } else {
            addNode(name: 'classNode',
                attr: [ClassName.create(value: node, asStr: true),
                    Modifiers.create(node.modifiers)],
                child: [node.superClass, Interfaces.create(node.interfaces),
                    Mixins.create(node.mixins),
                    GenericsTypes.create(node.genericsTypes)])
        }
    }
    private def addNode(ConstructorNode node) {
        addNode(name: 'constructor', 
            attr: Modifiers.create(node.modifiers),
            child: [Parameters.create(node.parameters), 
                Exceptions.create(node.exceptions), node.code])
    }
    private def addNode(FieldNode node) {
        addNode(name: 'fieldNode', 
            attr: [Str.create(node.name), Modifiers.create(node.modifiers),
                ClassName.create(node.type), ClassName.create(node.owner)], 
            child: node.initialExpression)
    }
    private def addNode(ImportNode node) {
        if (node.packageName) {
            addDoesNotSupport("new ImportNode(String packageName)")
            return
        }
        addNode(name: 'importNode',
            attr: [ClassName.create(node.type), Str.create(node.alias)])
    }
    private def addNode(MethodNode node) {
        addNode(name: 'method', 
            attr: [Str.create(node.name), Modifiers.create(node.modifiers),
                ClassName.create(node.returnType)],
            child: [Parameters.create(node.parameters), 
                Exceptions.create(node.exceptions), 
                node.code, 
                Annotations.create(node.annotations)])
    }
    private static class Exceptions {
        ClassNode[] value
        /**
         * Use create() instead.
         */
        private Exceptions(ClassNode[] value) {
            this.value = value
        } 
        static Exceptions create(ClassNode[] value) {
            null != value ? new Exceptions(value) : value
        }
    }
    private def addNode(Exceptions node) {
        addNode(name: 'exceptions', child: node.value)        
    }
    private def addNode(PackageNode node) {
        addDoesNotSupport("PackageNode")
    }
    private static class Parameters {
        Parameter[] value
        /**
         * Use create() instead.
         */
        private Parameters(Parameter[] value) {
            this.value = value
        }    
        static Parameters create(Parameter[] value) {
            null != value ? new Parameters(value) : value
        }
    }
    private def addNode(Parameters node) {
        addNode(name: 'parameters', child: node.value)
    }
    private def addNode(Parameter node) {
        addNode(name: 'parameter',
            attr: "${Str.create(node.name)}: ${ClassName.create(node.type)}",
            child: node.initialExpression)
    }
    private def addNode(PropertyNode node) {
        addNode(name: 'propertyNode', 
            attr: [ Str.create(node.name), Modifiers.create(node.modifiers),
                ClassName.create(node.field.type), ClassName.create(node.field.owner) ],
            child: node.initialExpression)
    }
    
    // others
    private static class Annotations {
        List value 
        /**
         * Use create() instead. 
         */
        private Annotations(List value) {
            this.value = value
        }
        static Annotations create(List value) {
            null != value ? new Annotations(value) : value
        }
    }
    private def addNode(Annotations node) {
        addNode(name: 'annotations', child: node.value)    
    }
    private static class AnnotationMember {
        String name
        Expression expression
        /**
         * Use create() instead. 
         */
        private AnnotationMember(String name, Expression expression) {
            this.name = name
            this.expression = expression
        }
        static AnnotationMember create(String name, Expression expression) {
            null != name ? new AnnotationMember(name, expression) : null
        }
    }
    private def addNode(AnnotationNode node) {
        def members = []
        node.members.each { k, v -> members.add(AnnotationMember.create(k, v)) }
        addNode(name: 'annotation',
            attr: ClassName.create(node.classNode), 
            child: members)
    }
    private def addNode(AnnotationMember node) {
        addNode(name: 'member', 
            attr: Str.create(node.name),
            child: node.expression)
    }
    private static class GenericsTypes {
        GenericsType[] value
        /**
         * Use create() instead.
         */
        private GenericsTypes(GenericsType[] value) {
            this.value = value
        }
        static GenericsTypes create(GenericsType[] value) {
            null != value ? new GenericsTypes(value) : value
        }
    }
    private def addNode(GenericsTypes node) {
        if (!node.value) {
            return
        }
        addNode(name: 'genericsTypes', child: node.value)
    }
    private static class UpperBound {
        ClassNode[] value
        /**
         * Use create() instead. 
         */
        private UpperBound(ClassNode[] value) {
            this.value = value
        }
        static UpperBound create(ClassNode[] value) {
            null != value ? new UpperBound(value) : value
        }
    }
    private def addNode(UpperBound node) {
        addNode(name: 'upperBound', child: node.value)    
    }
    private static class LowerBound {
        ClassNode value
        /**
         * Use create() instead.
         */
        private LowerBound(ClassNode value) {
            this.value = value
        }
        static LowerBound create(ClassNode value) {
            null != value ? new LowerBound(value) : value
        }
    } 
    private def addNode(LowerBound node) {
        addNode(name: 'lowerBound', attr: ClassName.create(node.value))
    }
    private def addNode(GenericsType node) {
        def children = []  
        if (!node.resolved) {
            if (node.upperBounds) children.add(UpperBound.create(node.upperBounds))
            if (node.lowerBound) children.add(LowerBound.create(node.lowerBound))
        }
        addNode(name: 'genericsType',
            attr: ClassName.create(node.type), child: children)
    }
    private def addNode(ModuleNode node) {
        addDoesNotSupport('ModuleNode')
    }
    private def addNode(Token node) {
        addNode(name: 'token', attr: Str.create(node.text))    
    }
    private def addNode(Label node) {
        addNode(name: 'label', attr: node)
    }
    
    /**
     * <p>Generates the AST spec DSL from the source code.</p>
     * <p>This method is a shourtcut for
     * <code>build(new AstBuilder().buildFromString(...))</code></p>
     * <p>Therefore, see {@link AstBuilder#buildFromString} for the details of 
     * the parameters because those are for the method.</p>
     * 
     * @param phase the compile phase.
     * @param statementsOnly when true, includes statements only.
     * @param source the source code.
     * @return the AST spec DSL.
     */
    String build(CompilePhase phase = CompilePhase.CLASS_GENERATION, 
        boolean statementsOnly = true, String source) {
        build(new AstBuilder().buildFromString(phase, statementsOnly, source))
    }
    /**
     * <p>Generates the AST spec DSL from the AST node.</p>
     * 
     * @param ast an AST node.
     * @return the AST spec DSL.
     */
    String build(ASTNode ast) {
        build([ast])    
    }
    /**
     * <p>Generates the AST spec DSL from the list of AST nodes.</p>
     * 
     * @param ast a list of AST nodes.
     * @return the AST spec DSL.
     */
    String build(List<ASTNode> ast) {
        spec = new StringBuilder()
        addNode(ast)
        spec.toString()
    }
}
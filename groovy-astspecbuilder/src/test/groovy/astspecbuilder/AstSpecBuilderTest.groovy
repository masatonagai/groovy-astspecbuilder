package astspecbuilder

import static AstSpecAssert.*
import static org.objectweb.asm.Opcodes.*
import groovy.lang.GroovyObject

import java.io.IOException

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.GenericsType
import org.codehaus.groovy.ast.ImportNode
import org.codehaus.groovy.ast.InnerClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.MixinNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.VariableScope
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
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import org.junit.Test

/**
 * A unit test class for {@link AstSpecBuilder}.
 * <p>Most of the test cases are from 
 * <code>AstBuilderFromSpecificationTest</code>.</p>
 * 
 * @author Nagai Masato
 */
class AstSpecBuilderTest {
    
    @Test void testIndent() {
        def result = 
            new AstSpecBuilder(indent: '\t'*2)
                .build(new ListExpression([new ConstantExpression('foo')]))
        def expected = '''\
list {
\t\tconstant 'foo'
}
'''
        assertSpec(expected, result)   
    }
    
    @Test void testBuildFromString() {
        def result = new AstSpecBuilder().build("'foo'")
        def expected = '''\
block {
    returnStatement {
        constant 'foo'
    }
}
'''
    }
    
    private String build(ASTNode ast) {
        new AstSpecBuilder().build(ast);
    } 
    private String build(List<ASTNode> ast) {
        new AstSpecBuilder().build(ast);
    } 
    
    // statements 
    @Test void testAssertStatement() {
        /*
          assert true : "should always be true"
          assert 1 == 2
        */
        def result = build(new BlockStatement(
            [
                new AssertStatement(
                    new BooleanExpression(
                        new ConstantExpression(true)
                    ),
                    new ConstantExpression("should always be true")
                ),
                new AssertStatement(
                    new BooleanExpression(
                        new BinaryExpression(
                            new ConstantExpression(1),
                            new Token(Types.COMPARE_EQUAL, "==", -1, -1),
                            new ConstantExpression(2)
                        )
                    )
                ),
            ],
            new VariableScope()
        ))
        def expected = '''\
block {
    assertStatement {
        booleanExpression {
            constant true
        }
        constant 'should always be true'
    }
    assertStatement {
        booleanExpression {
            binary {
                constant 1
                token '=='
                constant 2
            }
        }
        constant null
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testForStatementAndClosureListExpression() {
        /*
          for (int x = 0; x < 10; x++) {
              println x
          }
        */

        def expected = '''\
forStatement {
    parameter 'forLoopDummyParameter': java.lang.Object.class
    closureList {
        declaration {
            variable 'x'
            token '='
            constant 0
        }
        binary {
            variable 'x'
            token '<'
            constant 10
        }
        postfix {
            variable 'x'
            token '++'
        }
    }
    block {
        expression {
            methodCall {
                variable 'this'
                constant 'println'
                argumentList {
                    variable 'x'
                }
            }
        }
    }
}
'''
        def result = build(new ForStatement(
            new Parameter(ClassHelper.make(Object, false), "forLoopDummyParameter"),
            new ClosureListExpression(
                [
                    new DeclarationExpression(
                        new VariableExpression("x"),
                        new Token(Types.EQUALS, "=", -1, -1),
                        new ConstantExpression(0)
                    ),
                    new BinaryExpression(
                        new VariableExpression("x"),
                        new Token(Types.COMPARE_LESS_THAN, "<", -1, -1),
                        new ConstantExpression(10)
                    ),
                    new PostfixExpression(
                        new VariableExpression("x"),
                        new Token(Types.PLUS_PLUS, "++", -1, -1)
                    )
                ]
            ),
            new BlockStatement(
                [
                    new ExpressionStatement(
                        new MethodCallExpression(
                            new VariableExpression("this"),
                            new ConstantExpression("println"),
                            new ArgumentListExpression(
                                    new VariableExpression("x"),
                            )
                        )
                    )
                ],
                new VariableScope()
            )
        ))
        assertSpec(expected, result)
    }
    @Test void testIfStatement() {
        // if (foo == bar) println "Hello" else println "World"
        def result = build(new IfStatement(
            new BooleanExpression(
                new BinaryExpression(
                    new VariableExpression("foo"),
                    new Token(Types.COMPARE_EQUAL, "==", -1, -1),
                    new VariableExpression("bar")
                )
            ),
            new ExpressionStatement(
                new MethodCallExpression(
                    new VariableExpression("this"),
                    new ConstantExpression("println"),
                    new ArgumentListExpression(
                            [new ConstantExpression("Hello")]
                    )
                )
            ),
            new ExpressionStatement(
                new MethodCallExpression(
                    new VariableExpression("this"),
                    new ConstantExpression("println"),
                    new ArgumentListExpression(
                            [new ConstantExpression("World")]
                    )
                )
            )
        ))
        def expected = '''\
ifStatement {
    booleanExpression {
        binary {
            variable 'foo'
            token '=='
            variable 'bar'
        }
    }
    expression {
        methodCall {
            variable 'this'
            constant 'println'
            argumentList {
                constant 'Hello'
            }
        }
    }
    expression {
        methodCall {
            variable 'this'
            constant 'println'
            argumentList {
                constant 'World'
            }
        }
    }
}
'''
        assertSpec(expected, result)
    }

    @Test void testReturnAndSynchronizedStatement() {
        /*
          synchronized (this) {
              return 1
          }
        */
        def result = build(new SynchronizedStatement(
            new VariableExpression("this"),
            new BlockStatement(
                [new ReturnStatement(
                        new ConstantExpression(1)
                )],
                new VariableScope()
            )
        ))
        def expected = '''\
synchronizedStatement {
    variable 'this'
    block {
        returnStatement {
            constant 1
        }
    }
}
'''
        assertSpec(expected, result)
    }

    @Test void testSwitchAndCaseAndBreakStatements() {
        /*
          switch (foo) {
              case 0: break "some label"
              case 1:
              case 2:
                  println "<3"
                  break;
              default:
                  println ">2"
          }
        */
        def result = build(new SwitchStatement(
            new VariableExpression("foo"),
            [
                new CaseStatement(
                    new ConstantExpression(0),
                    new BreakStatement("some label")
                ),
                new CaseStatement(
                    new ConstantExpression(1),
                    new EmptyStatement()
                ),
                new CaseStatement(
                    new ConstantExpression(2),
                    new BlockStatement(
                        [
                            new ExpressionStatement(
                                new MethodCallExpression(
                                    new VariableExpression("this"),
                                    new ConstantExpression("println"),
                                    new ArgumentListExpression(
                                        [new ConstantExpression("<3")]
                                    )
                                )
                            ),
                            new BreakStatement()
                        ], new VariableScope()
                    )
                )
            ],
            new BlockStatement(
                [new ExpressionStatement(
                    new MethodCallExpression(
                        new VariableExpression("this"),
                        new ConstantExpression("println"),
                        new ArgumentListExpression(
                            [new ConstantExpression(">2")]
                        )
                    )
                )],
                new VariableScope()
            )
        ))
        def expected = '''\
switchStatement {
    variable 'foo'
    defaultCase {
        expression {
            methodCall {
                variable 'this'
                constant 'println'
                argumentList {
                    constant '>2'
                }
            }
        }
    }
    caseStatement {
        constant 0
        breakStatement 'some label'
    }
    caseStatement {
        constant 1
        empty()
    }
    caseStatement {
        constant 2
        block {
            expression {
                methodCall {
                    variable 'this'
                    constant 'println'
                    argumentList {
                        constant '<3'
                    }
                }
            }
            breakStatement()
        }
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testTryCatchStatement() {
        /*
          try {
              return 1
          } catch (Exception e) {
               throw e
          }
        */
        TryCatchStatement tryCatchStatement = new TryCatchStatement(
            new BlockStatement(
                [new ReturnStatement(
                    new ConstantExpression(1)
                )],
                new VariableScope()
            ),
            new EmptyStatement()
        )
        tryCatchStatement.addCatch(
            new CatchStatement(
                new Parameter(
                    ClassHelper.make(Exception, false), "e"
                ),
                new BlockStatement(
                    [new ThrowStatement(
                        new VariableExpression("e")
                    )],
                    new VariableScope()
                )
            )
        )
        def result = build(tryCatchStatement)
        def expected = '''\
tryCatch {
    block {
        returnStatement {
            constant 1
        }
    }
    empty()
    catchStatement {
        parameter 'e': java.lang.Exception.class
        block {
            throwStatement {
                variable 'e'
            }
        }
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testTryFinallyStatement() {
        /*
          try {
              return 1
          } finally {
               x.close()
          }
        */
        TryCatchStatement tryCatchStatement = new TryCatchStatement(
            new BlockStatement(
                [new ReturnStatement(
                    new ConstantExpression(1)
                )],
                new VariableScope()
            ),
            new BlockStatement(
                [
                    new BlockStatement(
                        [
                            new ExpressionStatement(
                                new MethodCallExpression(
                                    new VariableExpression('x'),
                                    'close',
                                    new ArgumentListExpression()
                                )
                            )
                        ],
                        new VariableScope())
                ],
                new VariableScope()
            )
        )
        def result = build(tryCatchStatement)
        def expected = '''\
tryCatch {
    block {
        returnStatement {
            constant 1
        }
    }
    block {
        block {
            expression {
                methodCall {
                    variable 'x'
                    constant 'close'
                    argumentList {
                    }
                }
            }
        }
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testWhileStatementAndContinueStatement() {
        /*
          while (true) {
              x++
              continue
          }
        */
        def result = build(new WhileStatement(
            new BooleanExpression(
                new ConstantExpression(true)
            ),
            new BlockStatement(
                [
                    new ExpressionStatement(
                        new PostfixExpression(
                            new VariableExpression("x"),
                            new Token(Types.PLUS_PLUS, "++", -1, -1),
                        )
                    ),
                    new ContinueStatement()
                ],
                new VariableScope()
            )
        ))
        def expected = '''\
whileStatement {
    booleanExpression {
        constant true
    }
    block {
        expression {
            postfix {
                variable 'x'
                token '++'
            }
        }
        continueStatement()
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testWhileStatementAndContinueToLabelStatement() {
        /*
          while (true) {
              x++
              continue "some label"
          }
        */
        def result = build(new WhileStatement(
            new BooleanExpression(
                new ConstantExpression(true)
            ),
            new BlockStatement(
                [
                    new ExpressionStatement(
                        new PostfixExpression(
                            new VariableExpression("x"),
                            new Token(Types.PLUS_PLUS, "++", -1, -1),
                        )
                    ),
                    new ContinueStatement("some label")
                ],
                new VariableScope()
            )
        ))
        def expected = '''\
whileStatement {
    booleanExpression {
        constant true
    }
    block {
        expression {
            postfix {
                variable 'x'
                token '++'
            }
        }
        continueStatement {
            label 'some label'
        }
    }
}
'''
        assertSpec(expected, result)
    }

    // expressions 
    @Test void testAnnotationConstantExpression() {
        def result = build(new AnnotationConstantExpression(
            new AnnotationNode(
                ClassHelper.make(Override.class, false)
            )
        ))
        def expected = '''\
annotationConstant {
    annotation java.lang.Override.class
}
'''
        assertSpec(expected, result)
    }
    @Test void testAnnotationWithParameter() {
        def methodNode = new MethodNode(
            "myMethod",
            ACC_PUBLIC,
            ClassHelper.make(Object, false),
            [] as Parameter[],
            [] as ClassNode[],
            new BlockStatement([], new VariableScope())
        )
        def annotation = new AnnotationNode(ClassHelper.make(Override, false))
        annotation.setMember('timeout', new ConstantExpression(50L))
        methodNode.addAnnotation(annotation)
        def result = build(methodNode)
        def expected = '''\
method 'myMethod', ACC_PUBLIC, java.lang.Object.class, {
    parameters {
    }
    exceptions {
    }
    block {
    }
    annotations {
        annotation java.lang.Override.class, {
            member 'timeout', {
                constant 50L
            }
        }
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testArgumentListExpressionNoArgs() {
        def result = build(new ArgumentListExpression())
        def expected = '''\
argumentList {
}
'''
        assertSpec(expected, result)
    }
    public void testArgumentListExpression_OneListArg() {
        def result = build(new ArgumentListExpression(
            [new ConstantExpression("constant1"),
                new ConstantExpression("constant2"),
                new ConstantExpression("constant3"),
                new ConstantExpression("constant4"),
            ]
        ))
        def expected = '''\
argumentList {
    constant 'constant1'
    constant 'constant2'
    constant 'constant3'
    constant 'constant4'
}
'''
        assertSpec(expected, result)
    }
    @Test void testArrayExpression() {
        // new Integer[]{1, 2, 3}
        def result = build(new ArrayExpression(
            ClassHelper.make(Integer, false),
            [new ConstantExpression(1),
                new ConstantExpression(2),
                new ConstantExpression(3),]
        ))
        def expected = '''\
array java.lang.Integer.class, {
    constant 1
    constant 2
    constant 3
}
'''
        assertSpec(expected, result)
    }
    @Test void testAttributeExpression() {
        // represents foo.bar attribute invocation
        def result = build(new AttributeExpression(
            new VariableExpression("foo"),
            new ConstantExpression("bar")
        ))
        def expected = '''\
attribute {
    variable 'foo'
    constant 'bar'
}
'''
        assertSpec(expected, result)
    }
    @Test void testBitwiseNegationExpression() {
        def result = build(new BitwiseNegationExpression(
            new ConstantExpression(1)
        ))
        def expected = '''\
bitwiseNegation {
    constant 1
}
'''
        assertSpec(expected, result)
    }
    @Test void testCastExpression() {
        def result = build(new CastExpression(
            ClassHelper.make(Integer, false),
            new ConstantExpression("")
        ))
        def expected = '''\
cast java.lang.Integer.class, {
    constant ''
}
'''
        assertSpec(expected, result)
    }
    @Test void testClassExpression() {
        // def foo = String
        def result = build(new DeclarationExpression(
            new VariableExpression("foo"),
            new Token(Types.EQUALS, "=", -1, -1),
            new ClassExpression(ClassHelper.make(String, false))
        ))
        def expected = '''\
declaration {
    variable 'foo'
    token '='
    classExpression java.lang.String.class
}
'''
        assertSpec(expected, result)
    }
    @Test void testClosureExpression() {
        // { parm -> println parm }
        def result = build(new ClosureExpression(
            [new Parameter(
                ClassHelper.make(Object, false), "parm"
            )] as Parameter[],
            new BlockStatement(
                [new ExpressionStatement(
                    new MethodCallExpression(
                        new VariableExpression("this"),
                        new ConstantExpression("println"),
                        new ArgumentListExpression(
                            new VariableExpression("parm")
                        )
                    )
                )],
                new VariableScope()
            )
        ))
        def expected = '''\
closure {
    parameters {
        parameter 'parm': java.lang.Object.class
    }
    block {
        expression {
            methodCall {
                variable 'this'
                constant 'println'
                argumentList {
                    variable 'parm'
                }
            }
        }
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testClosureExpressionMultipleParameters() {
        // { x,y,z -> println z }
        def result = build(new ClosureExpression(
            [new Parameter(ClassHelper.make(Object, false), "x"),
                new Parameter(ClassHelper.make(Object, false), "y"),
                new Parameter(ClassHelper.make(Object, false), "z")] as Parameter[],
            new BlockStatement(
                [new ExpressionStatement(
                    new MethodCallExpression(
                        new VariableExpression("this"),
                        new ConstantExpression("println"),
                        new ArgumentListExpression(
                            new VariableExpression("z")
                        )
                    )
                )],
                new VariableScope()
            )
        ))
        def expected = '''\
closure {
    parameters {
        parameter 'x': java.lang.Object.class
        parameter 'y': java.lang.Object.class
        parameter 'z': java.lang.Object.class
    }
    block {
        expression {
            methodCall {
                variable 'this'
                constant 'println'
                argumentList {
                    variable 'z'
                }
            }
        }
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testConstructorCallExpression() {
        // new Integer(4)
        def result = build(new ConstructorCallExpression(
            ClassHelper.make(Integer, false),
            new ArgumentListExpression(
                new ConstantExpression(4)
            )
        ))
        def expected = '''\
constructorCall java.lang.Integer.class, {
    argumentList {
        constant 4
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testDeclarationAndListExpression() {
        // represents def foo = [1, 2, 3]
        def result = build(new DeclarationExpression(
            new VariableExpression("foo"),
            new Token(Types.EQUALS, "=", -1, -1),
            new ListExpression(
                    [new ConstantExpression(1),
                            new ConstantExpression(2),
                            new ConstantExpression(3),]
            )
        ))
        def expected = '''\
declaration {
    variable 'foo'
    token '='
    list {
        constant 1
        constant 2
        constant 3
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testElvisOperatorExpression() {
        // name ?: 'Anonymous'
        def expected = '''\
elvisOperator {
    booleanExpression {
        variable 'name'
    }
    constant 'Anonymous'
}
'''
        def result = build(new ElvisOperatorExpression(
            new VariableExpression('name'),
            new ConstantExpression('Anonymous')
        ))
        assertSpec(expected, result)
    }
    @Test void testFieldExpression() {
        // public static String foo = "a value"
        def result = build([
            new FieldExpression(
                new FieldNode(
                    "foo",
                    ACC_PUBLIC | ACC_STATIC,
                    ClassHelper.make(String, false),
                    ClassHelper.make(this.class, false),
                    new ConstantExpression("a value")
                )
            )
        ])
        def expected = """\
field {
    fieldNode 'foo', ACC_PUBLIC | ACC_STATIC, java.lang.String.class, ${this.class.name}.class, {
        constant 'a value'
    }
}
"""
        assertSpec(expected, result)
    }
    @Test void testGenericsType() {
        // class MyClass<T, U extends Number> {}
        def classNode = new ClassNode(
            "MyClass", 
            ACC_PUBLIC,
            ClassHelper.make(Object, false),
            [ClassHelper.make(GroovyObject, false)] as ClassNode[],
            [] as MixinNode[]
        )
        classNode.setGenericsTypes([
            new GenericsType(ClassHelper.make(Object, false)),
            new GenericsType(ClassHelper.make(Number, false), [ClassHelper.make(Number, false)] as ClassNode[], null),
        ] as GenericsType[])
        def result = build(classNode)
        def expected = '''\
classNode 'MyClass', ACC_PUBLIC, {
    classNode java.lang.Object.class
    interfaces {
        classNode groovy.lang.GroovyObject.class
    }
    mixins {
    }
    genericsTypes {
        genericsType java.lang.Object.class
        genericsType java.lang.Number.class, {
            upperBound {
                classNode java.lang.Number.class
            }
        }
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testGenericsTypeWithLowerBounds() {
        def classNode = new ClassNode(
            "MyClass", ACC_PUBLIC, ClassHelper.make(Object, false)
        )
        classNode.setGenericsTypes(
            [
                new GenericsType(ClassHelper.make(Object, false)),
                new GenericsType(
                    ClassHelper.make(Number, false),
                    [ClassHelper.make(Number, false), ClassHelper.make(Comparable, false)] as ClassNode[],
                    ClassHelper.make(Integer, false)),
            ] as GenericsType[]
        )
        def result = build(classNode)
        def expected = '''\
classNode 'MyClass', ACC_PUBLIC, {
    classNode java.lang.Object.class
    interfaces {
        classNode groovy.lang.GroovyObject
    }
    mixins {
    }
    genericsTypes {
        genericsType java.lang.Object.class
        genericsType java.lang.Number.class, {
            upperBound {
                classNode java.lang.Number.class
                classNode java.lang.Comparable
            }
            lowerBound Integer
        }
    }
}
'''
        }
    @Test void testGStringExpression() {
        def result = build(new GStringExpression('$foo astring $bar',
            [new ConstantExpression(''), new ConstantExpression(' astring '), new ConstantExpression('')],
            [new VariableExpression('foo'), new VariableExpression('bar')]))
        def expected = '''\
gString '$foo astring $bar', {
    strings {
        constant ''
        constant ' astring '
        constant ''
    }
    values {
        variable 'foo'
        variable 'bar'
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testMapAndMapEntryExpression() {
        // [foo: 'bar', baz: 'buz']
        def result = build(new MapExpression(
            [
                new MapEntryExpression(
                    new ConstantExpression('foo'),
                    new ConstantExpression('bar')
                ),
                new MapEntryExpression(
                    new ConstantExpression('baz'),
                    new ConstantExpression('buz')
                ),
            ]
        ))
        def expected = '''\
map {
    mapEntry {
        constant 'foo'
        constant 'bar'
    }
    mapEntry {
        constant 'baz'
        constant 'buz'
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testMethodPointerExpression() {
        // Integer.&toString
        def result = build(new MethodPointerExpression(
            new ClassExpression(ClassHelper.make(Integer, false)),
            new ConstantExpression("toString")
        ))
        def expected = '''\
methodPointer {
    classExpression java.lang.Integer.class
    constant 'toString'
}
'''
        assertSpec(expected, result)
    }
    @Test void testNamedArgumentListExpression() {
        // new String(foo: 'bar')
        def result = build(new ConstructorCallExpression(
            ClassHelper.make(String),
            new TupleExpression(
                new NamedArgumentListExpression(
                    [
                        new MapEntryExpression(
                            new ConstantExpression('foo'),
                            new ConstantExpression('bar'),
                        )
                    ]
                )
            )
        ))
        def expected = '''\
constructorCall java.lang.String.class, {
    tuple {
        namedArgumentList {
            mapEntry {
                constant 'foo'
                constant 'bar'
            }
        }
    }
}
'''
        assertSpec(expected, result)
    }

    @Test void testNotExpression() {
        // !true
        def result = build(new NotExpression(
            new ConstantExpression(true)
        ))
        def expected = '''\
not {
    constant true
}
'''
        assertSpec(expected, result)
    }
    @Test void testParametersDefaultValues() {
        /*
          public String myMethod(String parameter = null) {
            'some result'
          }
         */
        def result = build(new MethodNode(
            "myMethod",
            ACC_PUBLIC,
            ClassHelper.make(String.class, false),
            [new Parameter(ClassHelper.make(String, false), "parameter", new ConstantExpression(null))] as Parameter[],
            [] as ClassNode[],
            new BlockStatement(
                [new ReturnStatement(
                        new ConstantExpression('some result')
                )],
                new VariableScope()
            )
        ))
        def expected = '''\
method 'myMethod', ACC_PUBLIC, java.lang.String.class, {
    parameters {
        parameter 'parameter': java.lang.String.class, {
            constant null
        }
    }
    exceptions {
    }
    block {
        returnStatement {
            constant 'some result'
        }
    }
    annotations {
    }
}
'''
        assertSpec(expected, result)
    }

    @Test void testParametersVarArgs() {
        /*
          public String myMethod(String... parameters) {
            'some result'
          }
        */
        // vararg methods are just array methods. 
        def result = build(new MethodNode(
            "myMethod",
            ACC_PUBLIC,
            ClassHelper.make(String.class, false),
            [new Parameter(ClassHelper.make(String[], false), "parameters")] as Parameter[],
            [] as ClassNode[],
            new BlockStatement(
                    [new ReturnStatement(
                            new ConstantExpression('some result')
                    )],
                    new VariableScope()
            )
        ))
        def expected = '''\
method 'myMethod', ACC_PUBLIC, java.lang.String.class, {
    parameters {
        parameter 'parameters': java.lang.String[].class
    }
    exceptions {
    }
    block {
        returnStatement {
            constant 'some result'
        }
    }
    annotations {
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testPostfixExpression() {
        // 1++
        def result = build(new PostfixExpression(
            new ConstantExpression(1),
            new Token(Types.PLUS_PLUS, "++", -1, -1)
        ))
        def expected = '''\
postfix {
    constant 1
    token '++'
}
'''
         assertSpec(expected, result)
    }
    @Test void testPrefixExpression() {
        // ++1
        def result = build(new PrefixExpression(
            new Token(Types.PLUS_PLUS, "++", -1, -1),
            new ConstantExpression(1)
        ))
        def expected = '''\
prefix {
    token '++'
    constant 1
}
'''
        assertSpec(expected, result)
    }
    @Test void testPropertyExpression() {
        // foo.bar
        def result = build(new PropertyExpression(
            new VariableExpression("foo"),
            new ConstantExpression("bar")
        ))
        def expected = '''\
property {
    variable 'foo'
    constant 'bar'
}
'''
        assertSpec(expected, result)
    }
    @Test void testRangeExpression() {
        // (0..10)
        def result = build(new RangeExpression(
            new ConstantExpression(0),
            new ConstantExpression(10),
            true
        ))
        def expected = '''\
range {
    constant 0
    constant 10
    inclusive true
}
'''
        assertSpec(expected, result)
    }
    @Test void testRangeExpression_Exclusive() {
        // (0..<10)
        def result = build(new RangeExpression(
            new ConstantExpression(0),
            new ConstantExpression(10),
            false
        ))
        def expected = '''\
range {
    constant 0
    constant 10
    inclusive false
}
'''
        assertSpec(expected, result)
    }
    @Test void testSimpleMethodCall() {
        def result = build(new MethodCallExpression(
            new VariableExpression("this"),
            new ConstantExpression("println"),
            new ArgumentListExpression(
                    [new ConstantExpression("Hello")]
            )
        ))
        def expected = '''\
methodCall {
    variable 'this'
    constant 'println'
    argumentList {
        constant 'Hello'
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testSpreadExpression() {
        // ['foo', *['bar', 'baz']]
        def result = build(new ListExpression([
            new ConstantExpression('foo'),
            new SpreadExpression(
                new ListExpression([
                        new ConstantExpression('bar'),
                        new ConstantExpression('baz'),
                ])
            )]
        ))
        def expected = '''\
list {
    constant 'foo'
    spread {
        list {
            constant 'bar'
            constant 'baz'
        }
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testSpreadMapExpression() {
        // func(*:m)
        def result = build(new MethodCallExpression(
            new VariableExpression('this', ClassHelper.make(Object, false)),
            'func',
            new MapEntryExpression(
                new SpreadMapExpression(
                    new VariableExpression('m', ClassHelper.make(Object, false))
                ),
                new VariableExpression('m', ClassHelper.make(Object, false))
            )
        ))
        def expected = '''\
methodCall {
    variable 'this'
    constant 'func'
    tuple {
        mapEntry {
            spreadMap {
                variable 'm'
            }
            variable 'm'
        }
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testDoWhileStatement() {
        // The DSL does not support DoWhileStatement.
    }

    @Test void testStaticMethodCallExpression() {
        def result = build([new StaticMethodCallExpression(
            ClassHelper.make(Math.class, false),
            "min",
            new ArgumentListExpression(
                    new ConstantExpression(1),
                    new ConstantExpression(2)
            )
        )])
        def expected = '''\
staticMethodCall java.lang.Math.class, 'min', {
    argumentList {
        constant 1
        constant 2
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testTernaryExpression() {
        def result = build(new TernaryExpression(
            new BooleanExpression(
                    new ConstantExpression(true)
            ),
            new ConstantExpression('male'),
            new ConstantExpression('female')
        ))
        def expected = '''\
ternary {
    booleanExpression {
        constant true
    }
    constant 'male'
    constant 'female'
}
'''
        assertSpec(expected, result)
    }

    @Test void testUnaryMinusExpression() {
        def result = build(
            new UnaryMinusExpression(
                new VariableExpression("foo"))
        )
        def expected = '''\
unaryMinus {
    variable 'foo'
}
'''
        assertSpec(expected, result) 
    }
    @Test void testUnaryPlusExpression() {
        def result = build(
            new UnaryPlusExpression(
                new VariableExpression("foo"))
        )
        def expected = '''\
unaryPlus {
    variable 'foo'
}
'''
        assertSpec(expected, result) 
    }
    
    // annotated nodes
    @Test void testConstructorNode() {
        // public <init>(String foo, Integer bar) throws IOException, Exception {}
        def result = build(new ConstructorNode(
            ACC_PUBLIC,
            [
                new Parameter(ClassHelper.make(String, false), "foo"),
                new Parameter(ClassHelper.make(Integer, false), "bar")
            ] as Parameter[],
            [
                ClassHelper.make(Exception, false),
                ClassHelper.make(IOException, false)
            ] as ClassNode[],
            new BlockStatement()
        ))
        def expected = '''\
constructor ACC_PUBLIC, {
    parameters {
        parameter 'foo': java.lang.String.class
        parameter 'bar': java.lang.Integer.class
    }
    exceptions {
        classNode java.lang.Exception.class
        classNode java.io.IOException.class
    }
    block {
    }
}
'''
        assertSpec(expected, result)
    }

    @Test public void testImportNode() {
        def result = build([
            new ImportNode(ClassHelper.make(String, false), "string"),
            new ImportNode(ClassHelper.make(Integer, false), null),
            new ImportNode("java.io")
        ])
        def expected = '''\
importNode java.lang.String.class, 'string'
importNode java.lang.Integer.class
// The DSL does not support new ImportNode(String packageName).
'''
        assertSpec(expected, result)
    }
    @Test void testInnerClassNode() {
        /*
        class Foo {
          static class Bar {
          }
        }
        */
        def result = build(new InnerClassNode(
            new ClassNode(
                    "Foo",
                    ACC_PUBLIC,
                    ClassHelper.make(Object, false),
                    [ClassHelper.make(GroovyObject, false)] as ClassNode[],
                    [] as MixinNode[]
            ),
            'Foo$Bar',
            ACC_PUBLIC,
            ClassHelper.make(Object, false),
            [ClassHelper.make(GroovyObject, false)] as ClassNode[],
            [] as MixinNode[]
        ))
        def expected = '''\
innerClass 'Foo$Bar', ACC_PUBLIC, {
    classNode 'Foo', ACC_PUBLIC, {
        classNode java.lang.Object.class
        interfaces {
            classNode groovy.lang.GroovyObject.class
        }
        mixins {
        }
    }
    classNode java.lang.Object.class
    interfaces {
        classNode groovy.lang.GroovyObject.class
    }
    mixins {
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testMethodNode() {
        /*
          @Override
          public String myMethod(String parameter) throws Exception, IOException {
            'some result'
          }
        }
        */
        def methodNode = new MethodNode(
            "myMethod",
            ACC_PUBLIC,
            ClassHelper.make(String, false),
            [new Parameter(ClassHelper.make(String, false), "parameter")] as Parameter[],
            [ClassHelper.make(Exception, false), ClassHelper.make(IOException, false)] as ClassNode[],
            new BlockStatement(
                [new ReturnStatement(
                        new ConstantExpression('some result')
                )],
                new VariableScope()
            )
        )
        methodNode.addAnnotation(new AnnotationNode(ClassHelper.make(Override, false)))
        def result = build(methodNode)
        def expected = '''\
method 'myMethod', ACC_PUBLIC, java.lang.String.class, {
    parameters {
        parameter 'parameter': java.lang.String.class
    }
    exceptions {
        classNode java.lang.Exception.class
        classNode java.io.IOException.class
    }
    block {
        returnStatement {
            constant 'some result'
        }
    }
    annotations {
        annotation java.lang.Override.class
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testMixinNode() {
        def result = build(new ClassNode(
                "MyClass", ACC_PUBLIC,
                ClassHelper.make(Object, false),
                [ClassHelper.make(GroovyObject, false)] as ClassNode[],
                [
                        new MixinNode("ClassA", ACC_PUBLIC, ClassHelper.make(String, false)),
                        new MixinNode(
                                "ClassB",
                                ACC_PUBLIC,
                                ClassHelper.make(String, false),
                                [ClassHelper.make(GroovyObject, false)] as ClassNode[]), // interfaces
                ] as MixinNode[]
        ))
        def expected = '''\
classNode 'MyClass', ACC_PUBLIC, {
    classNode java.lang.Object.class
    interfaces {
        classNode groovy.lang.GroovyObject.class
    }
    mixins {
        mixin 'ClassA', ACC_PUBLIC, {
            classNode java.lang.String.class
            interfaces {
            }
        }
        mixin 'ClassB', ACC_PUBLIC, {
            classNode java.lang.String.class
            interfaces {
                classNode groovy.lang.GroovyObject.class
            }
        }
    }
}
'''
        assertSpec(expected, result)
    }
    @Test void testPropertyNode() {
        //  def myField = "foo"
        def result = build(new PropertyNode(
            "MY_VALUE",
            ACC_PUBLIC,
            ClassHelper.make(String, false),
            ClassHelper.make(this.class, false),
            new ConstantExpression("foo"),
            null,
            null
        ))
        def expected = """\
propertyNode 'MY_VALUE', ACC_PUBLIC, java.lang.String.class, ${this.class.name}.class, {
    constant 'foo'
}
"""
        assertSpec(expected, result)
    }
    
}
package astspecbuilder

import static junit.framework.Assert.*

import org.codehaus.groovy.control.CompilationFailedException

class AstSpecAssert {
    
    static void assertSpec(String expected, String result) {
        assertEquals(expected, result)
        def script = """\
import static org.objectweb.asm.Opcodes.*
new org.codehaus.groovy.ast.builder.AstBuilder().buildFromSpec {
${result}
}
"""
        try {
            new GroovyShell().evaluate(script)       
        } catch (CompilationFailedException e) {
            assertFalse(e.message, true);
        }
    }

}

import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11.GL_INVALID_ENUM;
import static org.lwjgl.opengl.GL11C.GL_TRUE;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Shader implements AutoCloseable
{
    private int shaderId;

    Shader(String shaderFile, ShaderType shaderType) {
        createShaderObject(shaderType);
        compileShader(shaderFile);
    }

    public int GetShaderId() {
        return shaderId;
    }

    public void DeleteShader() {
        glDeleteShader(shaderId);
    }

    private void createShaderObject(ShaderType shaderType) {
        shaderId = glCreateShader(shaderType.getValue());

        switch (shaderId) {
            case 0:
                System.err.println("An error occurred attempting to create shader");
                System.exit(-1);
                break;
            case GL_INVALID_ENUM:
                System.err.println("An invalid shader type was provided when attempting to create a shader.");
                System.exit(-1);
                break;
        }
    }

    private void compileShader(String shaderFileName) {
        try {

            // In Java, all classes has "Object" has a superclass.
            // This "Object" class has the "getClass" method.
            // This returns a runtime class of this Object.
            // From this, we can get the class loader which loaded the class.
            // Class Loaders: https://docs.oracle.com/en/java/javase/12/docs/api/java.base/java/lang/ClassLoader.html
            // getResource will find a resource with the given name, relative to the class location
            var relativeDirectory =
                    Paths.get(getClass().getClassLoader().getResource(shaderFileName + ".glsl").toURI());

            var vertexShaderContent = Files.readString(relativeDirectory);

            // glShaderSource will set the source code of a given shader object to what the content of the
            // given string is.
            glShaderSource(shaderId, vertexShaderContent);

            glCompileShader(shaderId);

            try (MemoryStack stack = stackPush()) {
                var compilationSuccessBuffer = stack.mallocInt(1);
                glGetShaderiv(shaderId, GL_COMPILE_STATUS, compilationSuccessBuffer);

                var compilationSuccess = compilationSuccessBuffer.get(0);

                if (compilationSuccess != GL_TRUE) {
                    var shaderCompilationLog = glGetShaderInfoLog(shaderId);
                    System.out.println(shaderCompilationLog);
                    System.exit(-1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();

            // Terminate. If we can't find the shader there's nothing that can be done.
            System.exit(-1);
        } catch (URISyntaxException syntaxException) {
            syntaxException.printStackTrace();

            // Terminate. If we can't find the shader there's nothing that can be done.
            System.exit(-1);
        }
    }

    @Override
    public void close() throws Exception {
        System.out.println("Closing shader!");
        glDeleteShader(shaderId);
    }
}

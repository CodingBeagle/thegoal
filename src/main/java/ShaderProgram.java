import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class ShaderProgram {
    private int shaderProgramId;

    ShaderProgram(Shader vertexShader, Shader fragmentShader) {
        CreateShaderObject();
        LinkShaderProgram(vertexShader, fragmentShader);
    }

    public void activate() {
        glUseProgram(shaderProgramId);
    }

    public void set4dUniform(String uniformName, float v1, float v2, float v3, float v4) {
        // Set uniforms
        // We need to have an active shader program before we can retrieve uniform locations of it
        int renderColorUniformLocation = glGetUniformLocation(shaderProgramId, "renderColor");
        glUniform4f(renderColorUniformLocation, 0.5f, 0.5f, 0.5f, 1.0f);
    }

    public void setMatrix4fv(String uniformName, Matrix4f matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = matrix.get(stack.mallocFloat(16));

            int transformationMatrixLocation = glGetUniformLocation(shaderProgramId, "transform");
            glUniformMatrix4fv(transformationMatrixLocation, false, fb);
        }
    }

    private void CreateShaderObject() {
        // In OpenGL, compiled shaders have to be linked together to a shader program.
        // An active shader program's shaders will be used when issuing render calls
        shaderProgramId = glCreateProgram();

        if (shaderProgramId == 0) {
            System.err.println("An error occurred attempting to create a shader program.");
            System.exit(-1);
        }
    }

    private void LinkShaderProgram(Shader vertexShader, Shader fragmentShader) {
        glAttachShader(shaderProgramId, vertexShader.GetShaderId());
        glAttachShader(shaderProgramId, fragmentShader.GetShaderId());

        glLinkProgram(shaderProgramId);

        try (MemoryStack stack = stackPush()) {
            IntBuffer linkStatusBuffer = stack.callocInt(1);

            glGetProgramiv(shaderProgramId, GL_LINK_STATUS, linkStatusBuffer);

            int linkStatus = linkStatusBuffer.get(0);

            if (linkStatus != GL_TRUE) {
                String shaderProgramLinkLog = glGetShaderInfoLog(shaderProgramId);
                System.out.println(shaderProgramLinkLog);
                System.exit(-1);
            }
        }
    }
}

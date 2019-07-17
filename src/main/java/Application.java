import org.lwjgl.*;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import static org.lwjgl.opengl.GL33C.*;

public class Application {

    // The window handle
    private long window;

    // Simple Triangle Vertices
    float vertices[] = {
            -0.5f, -0.5f, 0.0f,
            0.5f, -0.5f, 0.0f,
            0.0f, 0.5f, 0.0f
    };

    public static void main(String[] args) {
        new Application().run();
    }

    public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        init();
        loop();

        // Clean up all of GLFW's resources that were allocated.
        glfwTerminate();
    }

    private void init() {
        // Setup an error callback. The default implementation
        // Will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        // Create the window
        window = glfwCreateWindow(800, 600, "The Goal", NULL, NULL);

        if (window == NULL)
            throw new RuntimeException("Failed to create the application window.");

        // Get the thread stack and push a new frame
        // The MemoryStack API is an API by LWJGL used in the cases where you need to efficiently allocate
        // Stack variables used to retrieve values using a function call.
        // In Java, there is no way to state that you want a variable to be defined on the stack.
        // In a language like C, this is simple, you just define it within a function scope.
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // The stack frame is popped automatically

        // Make the OpenGl context current
        glfwMakeContextCurrent(window);

        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's OpenGl Context, or any other context
        // That is managed externally.
        // LWJGL detects the context that is current in the current thread, creates the GLCapabilities instance
        // And makes the OpenGL bindings available for use.
        GL.createCapabilities();

        // TODO: Read up on Java data types
        CopyVerticesToGpu();

        // We create and compile vertex shader object
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        compileShader(vertexShader, "vertex");

        // We create and compile fragment shader object
        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        compileShader(fragmentShader, "fragment");

        // Set the clear color
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

        // Run the rendering loop until the user has attempted to close
        // the window
        while (!glfwWindowShouldClose(window)) {
            // Clear the framebuffer
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Swap the color buffers
            glfwSwapBuffers(window);

            // Poll for window events.
            glfwPollEvents();
        }
    }

    private void compileShader(int vertexShader, String shaderFileName) {
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
            glShaderSource(vertexShader, vertexShaderContent);

            glCompileShader(vertexShader);

            try (MemoryStack stack = stackPush()) {
                var compilationSuccessBuffer = stack.mallocInt(1);
                glGetShaderiv(vertexShader, GL_COMPILE_STATUS, compilationSuccessBuffer);

                var compilationSuccess = compilationSuccessBuffer.get(0);

                if (compilationSuccess != GL_TRUE) {
                    var shaderCompilationLog = glGetShaderInfoLog(vertexShader);
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

    private void CopyVerticesToGpu() {
        // We generate an OpenGL buffer object
        // OpenGL buffers can be used for many things. They are simply allocated memory which can be used to store whatever you want
        int vbo = glGenBuffers();

        // Now we bind our generated buffer to the GL_ARRAY_BUFFER target. This essentially means that we will be using it as
        // as a vertex buffer object.
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        // Now that we have bound our buffer object to a target, we can start to make OpenGL calls to functions
        // That affect the state relevant for that object
        // Here we copy our vertice data to the GPU, to our newly created buffer object.
        // We also hint to OpenGL that the date most likely won't change. This means that OpenGL can make some assumptions
        // about the data which can be used to optimize it.
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
    }
}

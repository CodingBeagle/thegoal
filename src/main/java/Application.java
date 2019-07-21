import org.lwjgl.*;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.ovr.OVRVector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.IntBuffer;
import java.nio.file.Files;
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
            // Positions        // Colors
             0.5f,  0.5f, 0.0f, 1.0f, 0.0f, 0.0f, // Top right
             0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f, // Bottom right
            -0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, // Bottom left
            -0.5f,  0.5f, 0.0f, 0.5f, 0.5f, 1.0f // top left
    };

    int indices[] = {
        0, 1, 3, // first triangle
        1, 2, 3 // second triangle
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

        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if ( key == GLFW_KEY_SPACE && action == GLFW_PRESS ) {

                if (!wireframeToggle) {
                    glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                    wireframeToggle = true;
                } else {
                    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
                    wireframeToggle = false;
                }
            }
        });

        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }

    boolean wireframeToggle = false;

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's OpenGl Context, or any other context
        // That is managed externally.
        // LWJGL detects the context that is current in the current thread, creates the GLCapabilities instance
        // And makes the OpenGL bindings available for use.
        GL.createCapabilities();

        // TODO: Read up on Java data types

        // OpenGl Core REQUIRES us to use Vertex Array Objects (VAOs)
        // VAOs are OpenGL objects which will save state related to these calls:
        // -- Calls to glEnableVertexAttribArray or glDisableVertexAttribArray
        // -- Vertex attribute configurations via glVertexAttribPointer
        // -- Vertex buffer objects associated with vertex attributes by calls to glVertexAttribPointer
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

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

        int ebo = glGenBuffers();

        // With Element Buffer Objects, we can give OpenGL a list of indices, describing the order
        // In which triangles should be rendered from the vertices array.
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        // In the vertex shader we specified that location 0 accepted a 3D vector as input
        // OpenGL is very flexible when it comes to how to feed input into that location
        // But that also means we have to describe how the buffer is structured
        // So that OpenGL knows how to take the x, y and z number of each vertex described
        // in our array
        // Parameter 1: The index of the location we want to input to
        // Parameter 2: The number of components per generic vertex attribute
        // -- We have 3 components, since our input is a Vec3 in the vertex shader
        // Parameter 3: The data type of each component.
        // -- They are 32-bit floats
        // Parameter 4: Should data be normalized. Should be FALSE for floats.
        // Parameter 5: The byte offset between each consecutive generic vertex attribute.
        // -- Our array is tightly packed, so 0 byte offset between them
        // Parameter 6: The byte offset of the first component of the first generic vertex
        // Attribute.
        // -- This is 0 for us. It begins at the start of the array.
        // NOTICE: glVertexAttribPointer reads the currently bound buffer in GL_ARRAY_BUFFER
        // and stores it in the VAO, so unbinding the buffer in GL_ARRAY_BUFFER will not affect
        // The currently bound VAO

        // Position attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * 4, 0);

        // Color attribute
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * 4, 3 * 4);

        // Enables the generic vertex attribute array specified by index
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        // Cleanup
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glDisableVertexAttribArray(0);

        var shaderProgram = new ShaderProgram(
                new Shader("vertex", ShaderType.VERTEX),
                new Shader("fragment", ShaderType.FRAGMENT));

        shaderProgram.activate();

        shaderProgram.set4dUniform("senderColor", 0.5f, 0.5f, 0.5f, 1.0f);

        // Set the clear color
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        glBindVertexArray(vao);

        // Run the rendering loop until the user has attempted to close
        // the window
        while (!glfwWindowShouldClose(window)) {
            // Clear the framebuffer
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);

            // Swap the color buffers
            glfwSwapBuffers(window);

            // Poll for window events.
            glfwPollEvents();
        }
    }
}

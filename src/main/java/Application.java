import org.lwjgl.*;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import static org.lwjgl.opengl.GL11.*;

public class Application {

    // The window handle
    private long window;

    public static void main(String[] args) {
        new Application().run();
    }

    public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        init();
        loop();
    }

    private void init() {
        // Setup an error callback. The default implementation
        // Will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        // Create the window
        window = glfwCreateWindow(800, 600, "The Goal", NULL, NULL);

        if (window== NULL)
            throw new RuntimeException("Failed to create the application window.");

        // Get the thread stack and push a new frame
        // The MemoryStack API is an API by LWJGL used in the cases where you need to efficiently allocate
        // Stack variables used to retrieve values using a function call.
        // In Java, there is no way to state that you want a variable to be defined on the stack.
        // In a language like C, this is simple, you just define it within a function scope.
        try ( MemoryStack stack = stackPush() ) {
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

        // Set the clear color
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

        // Run the rendering loop until the user has attempted to close
        // the window
        while ( !glfwWindowShouldClose(window) ) {
            // Clear the framebuffer
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Swap the color buffers
            glfwSwapBuffers(window);

            // Poll for window events.
            glfwPollEvents();
        }
    }
}

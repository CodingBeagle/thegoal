// this is an example of what is probably the most simple vertex shader possible.
// We simply take in a 3D vector and pass it along as output without doing any processing / transformation.

// Each shader begins with a declaration of the shader version
#version 330 core

// Here we declare the input vertex attributes
// In this case, we just need a single 3D vector because we just want position information for each vector
// We also explicitly state that this input should be at location 0
layout (location = 0) in vec3 aPos;

void main()
{
    // gl_Position is a predefined variables of a VERTEX shader, and is the output of the shader
    // It is mandatory to set this (as it is the output!).
    gl_Position = vec4(aPos.x, aPos.y, aPos.z, 1.0);
}
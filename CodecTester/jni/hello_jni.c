#include <string.h>
#include <jni.h>
 
jstring Java_com_bridge_tool_HelloJni_hello(JNIEnv* env, jobject javaThis) {
 return (*env)->NewStringUTF(env, "Hello from C Code!");
}
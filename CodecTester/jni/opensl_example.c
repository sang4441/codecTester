/*
opensl_example.c:
OpenSL example module
Copyright (c) 2012, Victor Lazzarini
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

#include <android/log.h>
#include "opensl_io.h"

#define BUFFERFRAMES 1024
//#define VECSAMPS_MONO 64
#define VECSAMPS_MONO 160
//#define VECSAMPS_STEREO 128
#define VECSAMPS_STEREO 320

#define SR 8000

static int on;
OPENSL_STREAM  *p;
int samps;


void start_process() {
  p = android_OpenAudioDevice(SR,1,2,BUFFERFRAMES);
  if(p == NULL) return;
}


short * getBuffer(int samplingRate, int bufferSize) {
//	__android_log_write(ANDROID_LOG_INFO, "JNI", "here1");
//	VECSAMPS_MONO = bufferSize;
	short  inbuffer[bufferSize];
	samps = android_AudioIn(p,inbuffer,bufferSize);
//	__android_log_write(ANDROID_LOG_INFO, "JNI", "here2");

//	int i = 0;

//	short min=0, max=0, average=0;
//	for (i; i < VECSAMPS_MONO; i++) {
//		average = average + inbuffer[i];
//		if (inbuffer[i] < min) {
//			min = inbuffer[i];
//		}
//		if (inbuffer[i] > max) {
//			max = inbuffer[i];
//		}
//	}
//	char str[15];
//	__android_log_write(ANDROID_LOG_ERROR, "line", "-------------------------------------------------------");


	return inbuffer;
}

void setBuffer(short * data, int bufferSize) {
	__android_log_write(ANDROID_LOG_ERROR, "Average", "hello");
	int i, j;

	short outbuffer[VECSAMPS_STEREO];
	for(i = 0, j=0; i < VECSAMPS_MONO; i++, j+=2) {
		 outbuffer[j] = outbuffer[j+1] = data[i];
	}
    android_AudioOut(p,outbuffer,samps*2);
}

void stop_process(){
  android_CloseAudioDevice(p);
}

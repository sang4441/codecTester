/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.sipdroid.codecs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


class RAW extends CodecBase implements Codec {
	RAW() {
		CODEC_NAME = "RAW";
		CODEC_USER_NAME = "RAW";
		CODEC_DESCRIPTION = "512kbit";
		CODEC_NUMBER = 88;
		CODEC_DEFAULT_SETTING = "always";
		CODEC_SAMPLE_RATE = 32000;		// default for most narrow band codecs
		CODEC_FRAME_SIZE = 640;		// default for most narrow band codecs
		super.update();
	}

	public int open() { return 0; }
	public int encode(short lin[], int offset, byte encoded[], int size){
		short[] shrink = new short[size]; // shrink short array. if don't, BufferOverflow
		System.arraycopy(lin, 0, shrink, 0, size);
		
		// gzip 추가
//		byte[] buf = new byte[size*2];
//		ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shrink);
//		buf = gzip(buf);
//		System.arraycopy(buf, 0, encoded, 0, buf.length);
//		return buf.length;
		
		ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shrink);
		return size*2;
	}
	public int decode(byte encoded[], short lin[], int size){
		byte[] payload = new byte[size];
		System.arraycopy(encoded, 0, payload, 0, size);
		
		// gzip 추가
//		byte[] buf = gunzip(payload);
//		ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(lin);
//		return buf.length/2;
		
		// to turn bytes to shorts as either big endian or little endian. 
		ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(lin);
		return size/2;
	}
	public void close() {}
	
	public void init() {
	}
}

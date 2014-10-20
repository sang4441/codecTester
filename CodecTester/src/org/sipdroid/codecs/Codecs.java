/*
 * Copyright (C) 2010 The Sipdroid Open Source Project
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

import java.util.HashMap;
import java.util.Vector;

import android.util.Log;

public class Codecs {
	private static final Vector<Codec> codecs = new Vector<Codec>() {
		{ 
			add(new G722()); // 9
			// save space (until a common library for all bitrates gets available?)
			add(new SILK24()); // 120
			add(new SILK16()); // 119
			add(new SILK8()); // 117
			
			add(new alaw()); // 8
			add(new ulaw()); // 0
			add(new Speex()); // 98
			add(new GSM()); // 3
			add(new BV16()); // 106
//			add(new AAC()); // 96, jni compile error
			add(new Opus()); // 107
			add(new iLBC()); // 97
			add(new RAW()); // 88
		}
	};
	private static final HashMap<Integer, Codec> codecsNumbers;
	private static final HashMap<String, Codec> codecsNames;

	static {
		final int size = codecs.size();
		codecsNumbers = new HashMap<Integer, Codec>(size);
		codecsNames = new HashMap<String, Codec>(size);

		for (Codec c : codecs) {
			codecsNames.put(c.name(), c);
			codecsNumbers.put(c.number(), c);
		}
	}

	public static Codec get(int key) {
		return codecsNumbers.get(key);
	}

	public static Codec getName(String name) {
		return codecsNames.get(name);
	}

	public static Vector<Codec> get() {
		return codecs;
	}
	
	public static int[] getCodecs() {
		Vector<Integer> v = new Vector<Integer>(codecs.size());
		Log.d("ssh", "getCodecs before : " + codecs);

		for (Codec c : codecs) {
			c.update();
			if (!c.isValid())
				continue;
			v.add(c.number());
		}
		Log.d("ssh", "available codec list : " + v);
		Log.d("ssh", "getCodecs after : " + codecs);
		int i[] = new int[v.size()];
		for (int j = 0; j < i.length; j++)
			i[j] = v.elementAt(j);
		return i;
	}

	public static class Map {
		public int number;
		public Codec codec;
		Vector<Integer> numbers;
		Vector<Codec> codecs;

		Map(int n, Codec c, Vector<Integer> ns, Vector<Codec> cs) {
			number = n;
			codec = c;
			numbers = ns;
			codecs = cs;
		}

		public boolean change(int n) {
			int i = numbers.indexOf(n);

			if (i >= 0 && codecs.elementAt(i) != null) {
				codec.close();
				number = n;
				codec = codecs.elementAt(i);
				return true;
			}
			return false;
		}

		public String toString() {
			return "Codecs.Map { " + number + ": " + codec + "}";
		}
	}

}

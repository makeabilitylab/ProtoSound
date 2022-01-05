package com.makeability.protosound.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class HelperUtils {
	/**
	 *
	 * @param bytes
	 * @return
	 */
	public static short[] convertByteArrayToShortArray(byte[] bytes) {
		short[] result = new short[bytes.length / 2];
		ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(result);
		return result;
	}
	/**
	 *
	 * @param data raw audio data
	 * @return loudness, in decibel
	 */
	public static double db(short[] data) {
		double rms = 0.0;
		int dataLength = 0;
		for (short datum : data) {
			if (datum != 0) {
				dataLength++;
			}
			rms += datum * datum;
		}
		rms = rms / dataLength;
		return 10 * Math.log10(rms);
	}

	/**
	 *
	 * @param soundBuffer raw audio data
	 * @return loudness, in decibel
	 */
	public static double db(List<Short> soundBuffer) {
		double rms = 0.0;
		int dataLength = 0;
		for (int i = 0; i < soundBuffer.size(); i++) {
			if (soundBuffer.get(i) != 0) {
				dataLength++;
			}
			rms += soundBuffer.get(i) * soundBuffer.get(i);
		}
		rms = rms / dataLength;
		return 10 * Math.log10(rms);
	}
}

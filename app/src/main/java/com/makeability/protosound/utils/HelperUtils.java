package com.makeability.protosound.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
}

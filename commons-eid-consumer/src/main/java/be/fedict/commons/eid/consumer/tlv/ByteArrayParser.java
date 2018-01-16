/*
 * Commons eID Project.
 * Copyright (C) 2014 - 2018 BOSA.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License version 3.0 as published by
 * the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, see https://www.gnu.org/licenses/.
 */

package be.fedict.commons.eid.consumer.tlv;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/**
 * Byte Array Fields Offset/Length Parser supports extraction of byte array
 * slices, unsigned 8 and 16-bit values from byte array integers
 * 
 * @author Frank Marien
 */
public class ByteArrayParser {

	private ByteArrayParser() {
	}

	/**
	 * Parses the given file using the meta-data annotations within the baClass
	 * parameter.
	 */
	public static <T> T parse(byte[] file, Class<T> baClass) {
		T t;
		try {
			t = parseThrowing(file, baClass);
		} catch (Exception ex) {
			throw new RuntimeException("error parsing file: " + baClass.getName(), ex);
		}
		return t;
	}

	private static <T> T parseThrowing(byte[] data, Class<T> baClass) throws InstantiationException, IllegalAccessException {
		Field[] fields = baClass.getDeclaredFields();

		T baObject = baClass.newInstance();
		for (Field field : fields) {
			ByteArrayField baFieldAnnotation = field.getAnnotation(ByteArrayField.class);
			if (baFieldAnnotation != null) {
				int offset = baFieldAnnotation.offset();
				int length = baFieldAnnotation.length();
				if (field.getType().isArray() && field.getType().getComponentType().equals(byte.class)) {
					byte[] byteArray = new byte[length];
					System.arraycopy(data, offset, byteArray, 0, length);
					field.set(baObject, byteArray);
				} else if (field.getType().equals(int.class)) {
					ByteBuffer buff = ByteBuffer.wrap(data);
					switch (length) {
						case 1 :
							field.set(baObject, (int) buff.get(offset) & 0xff);
							break;

						case 2 :
							field.set(baObject, (int) buff.getShort(offset) & 0xffff);
							break;
					}
				}
			}
		}

		return baObject;
	}
}

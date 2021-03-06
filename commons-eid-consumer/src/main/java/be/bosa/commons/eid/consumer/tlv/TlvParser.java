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

package be.bosa.commons.eid.consumer.tlv;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Tag-Length-Value parser. The TLV-format is used in the eID card for encoding
 * of the identity and address files.
 * 
 * @author Frank Cornelis
 * 
 */
public class TlvParser {

	private static final Log LOG = LogFactory.getLog(TlvParser.class);

	private TlvParser() {
	}

	/**
	 * Parses the given file using the meta-data annotations within the tlvClass
	 * parameter.
	 */
	public static <T> T parse(byte[] file, Class<T> tlvClass) {
		try {
			return parseThrowing(file, tlvClass);
		} catch (Exception ex) {
			throw new RuntimeException("error parsing file: " + tlvClass.getName(), ex);
		}
	}

	private static byte[] copy(byte[] source, int idx, int count) {
		byte[] result = new byte[count];
		System.arraycopy(source, idx, result, 0, count);
		return result;
	}

	private static <T> T parseThrowing(byte[] file, Class<T> tlvClass)
			throws InstantiationException, IllegalAccessException, DataConvertorException, UnsupportedEncodingException {
		Field[] fields = tlvClass.getDeclaredFields();
		Map<Integer, Field> tlvFields = new HashMap<>();
		T tlvObject = tlvClass.newInstance();
		
		for (Field field : fields) {
			TlvField tlvFieldAnnotation = field.getAnnotation(TlvField.class);
			if (tlvFieldAnnotation != null) {
				int tagId = tlvFieldAnnotation.value();
				if (tlvFields.containsKey(tagId)) {
					throw new IllegalArgumentException("TLV field duplicate: " + tagId);
				}
				tlvFields.put(tagId, field);
			}
			OriginalData originalDataAnnotation = field.getAnnotation(OriginalData.class);
			if (originalDataAnnotation != null) {
				field.setAccessible(true);
				field.set(tlvObject, file);
			}
		}

		int idx = 0;
		while (idx < file.length - 1) {
			int tag = file[idx];
			idx++;
			byte lengthByte = file[idx];
			int length = lengthByte & 0x7f;
			while ((lengthByte & 0x80) == 0x80) {
				idx++;
				lengthByte = file[idx];
				length = (length << 7) + (lengthByte & 0x7f);
			}
			idx++;
			if (0 == tag) {
				idx += length;
				continue;
			}
			if (tlvFields.containsKey(tag)) {
				Field tlvField = tlvFields.get(tag);
				Class<?> tlvType = tlvField.getType();
				ConvertData convertDataAnnotation = tlvField.getAnnotation(ConvertData.class);
				byte[] tlvValue = copy(file, idx, length);
				Object fieldValue;
				if (null != convertDataAnnotation) {
					Class<? extends DataConvertor<?>> dataConvertorClass = convertDataAnnotation.value();
					DataConvertor<?> dataConvertor = dataConvertorClass.newInstance();
					fieldValue = dataConvertor.convert(tlvValue);
				} else if (String.class == tlvType) {
					fieldValue = new String(tlvValue, "UTF-8");
				} else if (Boolean.TYPE == tlvType) {
					fieldValue = true;
				} else if (tlvType.isArray() && Byte.TYPE == tlvType.getComponentType()) {
					fieldValue = tlvValue;
				} else {
					throw new IllegalArgumentException("unsupported field type: " + tlvType.getName());
				}
				if (tlvField.get(tlvObject) != null && !tlvField.getType().isPrimitive()) {
					throw new RuntimeException("field was already set: " + tlvField.getName());
				}
				tlvField.setAccessible(true);
				tlvField.set(tlvObject, fieldValue);
			} else {
				LOG.debug("unknown tag: " + (tag & 0xff) + ", length: " + length);
			}
			idx += length;
		}
		return tlvObject;
	}
}

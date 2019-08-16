package io.tony.photo.utils;

import java.nio.ByteBuffer;

public class Bytes {

  public static byte[] longToBytes(long value) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.putLong(value);
    return buffer.array();
  }

  public static long bytesToLong(byte[] value) {
    return ByteBuffer.wrap(value).getLong();
  }
}

package io.humangraphics.backend.lambda.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ByteStreams {
  private ByteStreams() {}

  /**
   * Reads all bytes from {@code source} and returns the results as a byte array. Leaves the stream
   * open. Propagates all exceptions. In an exception occurs, then the stream is left in an
   * unspecified state.
   */
  public static byte[] toByteArray(InputStream source) throws IOException {
    ByteArrayOutputStream target = new ByteArrayOutputStream();
    try {
      copyTo(source, target);
    } finally {
      target.close();
    }
    return target.toByteArray();
  }

  /**
   * Copies all bytes from {@code source} to {@code target}. Leaves all streams open. Propagates all
   * exceptions. If an exception occurs, then both streams are left in an unspecified state.
   */
  public static void copyTo(InputStream source, OutputStream target) throws IOException {
    byte[] buf = new byte[16384];
    for (int nread = source.read(buf); nread != -1; nread = source.read(buf)) {
      target.write(buf, 0, nread);
    }
  }
}

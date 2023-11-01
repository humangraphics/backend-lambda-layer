package io.humangraphics.backend.lambda.util;

public final class LambdaLayer {
  private LambdaLayer() {}

  public static boolean DEBUG =
      System.getProperty("io.humangraphics.backend.lambda.debug", "true").equals("true");
}

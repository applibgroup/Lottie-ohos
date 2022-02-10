package com.airbnb.lottie.network;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * The result of the operation of obtaining a Lottie animation
 */
public interface LottieFetchResult extends Closeable {
  /**
   * operation successful
   * @return Is the operation successful
   */
  boolean isSuccessful();

  /**
   * Received content stream
   * @throws IOException
   * @return Received content stream
   */
  @NotNull
  InputStream bodyByteStream() throws IOException;

  /**
   * Type of content received
   * @return Type of content received
   */
  @Nullable
  String contentType();

  /**
   * Operation error
   * @return Operation error
   */
  @Nullable
  String error();
}

package com.airbnb.lottie.network;



import com.airbnb.lottie.L;
import com.airbnb.lottie.utils.HMOSLogUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

public class DefaultLottieFetchResult implements LottieFetchResult {

  @NotNull
  private final HttpURLConnection connection;

  public DefaultLottieFetchResult(@NotNull HttpURLConnection connection) {
    this.connection = connection;
  }

  @Override public boolean isSuccessful() {
    try {
      return connection.getResponseCode() / 100 == 2;
    } catch (IOException e) {
      return false;
    }
  }

  @NotNull @Override public InputStream bodyByteStream() throws IOException {
    return connection.getInputStream();
  }

  @Nullable
  @Override public String contentType() {
    return connection.getContentType();
  }

  @Nullable @Override public String error() {
    try {
      return isSuccessful() ? null :
          "Unable to fetch " + connection.getURL() + ". Failed with " + connection.getResponseCode() + "\n" + getErrorFromConnection(connection);
    } catch (IOException e) {
      HMOSLogUtil.warn(L.TAG,"get error failed "+ e.getLocalizedMessage());
      return e.getMessage();
    }
  }

  @Override public void close() {
    connection.disconnect();
  }

  private String getErrorFromConnection(HttpURLConnection connection) throws IOException {
    BufferedReader r = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
    StringBuilder error = new StringBuilder();
    String line;

    try {
      while ((line = r.readLine()) != null) {
        error.append(line).append('\n');
      }
    } catch (Exception e) {
      throw e;
    } finally {
      try {
        r.close();
      } catch (Exception e) {
        // Do nothing.
      }
    }
    return error.toString();
  }
}

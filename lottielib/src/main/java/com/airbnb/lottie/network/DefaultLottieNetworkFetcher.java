package com.airbnb.lottie.network;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class DefaultLottieNetworkFetcher implements LottieNetworkFetcher {

  @Override
  @NotNull
  public LottieFetchResult fetchSync(@NotNull String url) throws IOException {
    final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setRequestMethod("GET");
    connection.connect();
    return new DefaultLottieFetchResult(connection);
  }
}

package io.tony.photo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

import io.tony.photo.service.AlbumStore;
import io.tony.photo.service.PhotoStore;
import io.tony.photo.service.impl.AlbumFileStore;
import io.tony.photo.service.impl.PhotoStoreImpl;
import io.tony.photo.web.AlbumsRequestHandlerRegistry;
import io.tony.photo.web.PhotoRequestHandlerRegistry;
import io.tony.photo.web.WebServer;

public class App {

  private static final Logger log = LoggerFactory.getLogger(App.class);

  private static final int pageSize = 10;
  private PhotoStore photoStore;
  private WebServer webServer;
  private AlbumStore albumStore;

  private int port;
  private String path;

  public App(String path, int port) {
    this.path = path;
    this.port = port;
    photoStore = new PhotoStoreImpl(path);
    albumStore = new AlbumFileStore(Paths.get(path, "albums"), photoStore);
    webServer = new WebServer(port);
  }

  public void run() {
    webServer.registryRequestHandler(new PhotoRequestHandlerRegistry(photoStore));
    webServer.registryRequestHandler(new AlbumsRequestHandlerRegistry(albumStore));
    webServer.start();
  }

  public static void main(String[] args) throws Exception {
    if (args == null || args.length < 1) {
      return;
    }
    String storage = args[0];
    int port = args.length >= 2 ? Integer.parseInt(args[1]) : 8200;
    new App(storage, port).run();
  }
}

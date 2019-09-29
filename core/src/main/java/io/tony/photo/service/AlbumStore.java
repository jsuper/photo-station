package io.tony.photo.service.impl;

import java.util.List;

import io.tony.photo.pojo.Album;

public interface AlbumStore {

  void addAlbum(Album album);

  void deleteAlbum(Album album);

  Album getAlbum(String id);

  List<Album> getAlbums(int size);
}

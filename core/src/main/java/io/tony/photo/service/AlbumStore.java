package io.tony.photo.service;

import java.util.List;

import io.tony.photo.pojo.Album;

public interface AlbumStore {

  Album addAlbum(Album album);

  Album updateAlbum(Album album);

  void deleteAlbum(Album album);

  Album getAlbum(String id);

  List<Album> getAlbums(int size);

  void addPhotos(String id, List<String> photos);

  void deletePhotoFromAlbum(String id, List<String> photos);
}

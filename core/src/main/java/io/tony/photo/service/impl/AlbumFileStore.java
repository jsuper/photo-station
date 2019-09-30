package io.tony.photo.service.impl;

import com.drew.lang.StringUtil;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import io.tony.photo.pojo.Album;
import io.tony.photo.pojo.Photo;
import io.tony.photo.service.AlbumStore;
import io.tony.photo.service.PhotoIndexStore;
import io.tony.photo.service.PhotoStore;
import io.tony.photo.utils.FileOp;
import io.tony.photo.utils.Json;
import io.tony.photo.utils.Strings;

public class AlbumFileStore implements AlbumStore {
  private static final Logger log = LoggerFactory.getLogger(AlbumFileStore.class);

  private Path albumMetaStore;
  private PhotoStore photoStore;
  private PhotoIndexStore indexStore;

  public AlbumFileStore(Path albumMetaStore, PhotoStore photoStore) {
    if (Files.notExists(albumMetaStore)) {
      FileOp.createDirectoryQuietly(albumMetaStore);
    }
    this.albumMetaStore = albumMetaStore;
    this.photoStore = photoStore;
    this.indexStore = photoStore.getIndexStore();
  }

  @Override
  public Album addAlbum(Album album) {
    if (album.getId() == null || album.getId().isBlank()) {
      album.setId(UUID.randomUUID().toString().replaceAll("-", "").toLowerCase());
    }
    Path albumPath = albumMetaStore.resolve(album.getId());
    try (OutputStream fis = Files.newOutputStream(albumPath)) {
      fis.write(Json.toJson(album).getBytes("UTF-8"));
    } catch (Exception e) {
      throw new RuntimeException("Add album failed.", e);
    }
    return album;
  }

  @Override
  public Album updateAlbum(Album album) {
    return addAlbum(album);
  }

  @Override
  public void deleteAlbum(Album album) {
    try {
      Files.deleteIfExists(albumMetaStore.resolve(album.getId()));
      this.indexStore.deleteAlbums(album.getId());
    } catch (Exception e) {
      throw new RuntimeException("Delete album failed.", e);
    }
  }

  @Override
  public Album getAlbum(String id) {
    Album from = Json.from(albumMetaStore.resolve(id), Album.class);
    getAlbumInfo(from);
    return from;
  }

  @Override
  public List<Album> getAlbums(int size) {
    try {
      return Files.list(this.albumMetaStore)
        .map(p -> getAlbum(p.getFileName().toString()))
        .sorted(Comparator.comparingLong(Album::getCreateTime))
        .limit(size).collect(Collectors.toList());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return Collections.emptyList();
  }

  @Override
  public void addPhotos(String id, List<String> photos) {
    Album album = Json.from(albumMetaStore.resolve(id), Album.class);
    if (album != null) {
      List<Photo> all = photos.stream().map(photo -> photoStore.getPhotoById(photo)).filter(Objects::nonNull)
        .collect(Collectors.toList());
      boolean hasCover = Strings.notBlank(album.getCover());
      double bestRatio = 3d / 2d;
      double currentBestRatio = album.getCoverRatio() == null ? 0 : album.getCoverRatio();
      for (Photo photo : all) {
        if (photo.getAlbums() == null) {
          photo.setAlbums(new HashSet<>());
        }
        photo.getAlbums().add(album.getId());
        if (!hasCover) {
          double ratio = (photo.getWidth() * 1d) / (photo.getHeight() * 1d);
          if (ratio == bestRatio || currentBestRatio == 0) {
            album.setCover(photo.getId());
            album.setCoverRatio(ratio);
            hasCover = true;
          } else {
            if ((ratio - bestRatio) < (currentBestRatio - bestRatio)) {
              album.setCover(photo.getId());
              currentBestRatio = ratio;
            }
          }
        }
        photoStore.update(photo);
      }
      updateAlbum(album);
    }
  }

  @Override
  public void deletePhotoFromAlbum(String id, List<String> photos) {
    Album album = Json.from(albumMetaStore.resolve(id), Album.class);
    if (album != null) {
      int i = photos.indexOf(album.getCover());
      if (i > 0) {
        album.setCover(null);
        updateAlbum(album);
      }
    }
  }


  private void getAlbumInfo(Album album) {
    long start = System.currentTimeMillis();
    Query query = new TermQuery(new Term("albums", album.getName()));
    SortField sortByTimeStampAsc = new SortField("timestamp", SortField.Type.LONG);
    SortField sortByTimeStampDesc = new SortField("timestamp", SortField.Type.LONG, true);

    List<Document> startTime = this.indexStore.search(query, sortByTimeStampAsc, 1);
    try {
      if (startTime.size() == 1) {
        String date = startTime.get(0).getField("date").stringValue();
        album.setStart(DateTools.stringToDate(date));
      }

      List<Document> endQuery = this.indexStore.search(query, sortByTimeStampDesc, 1);
      if (endQuery.size() == 1) {
        String date = endQuery.get(0).getField("date").stringValue();
        album.setEnd(DateTools.stringToDate(date));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    album.setPhotos(indexStore.getTotalDocs("albums", album.getId()));
    start = System.currentTimeMillis() - start;
    log.debug("Query album info spends: {}ms", start);
  }
}

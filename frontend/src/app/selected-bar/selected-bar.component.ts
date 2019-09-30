import { Component, OnInit, ElementRef } from '@angular/core';
import { SectionService } from 'app/sections/section.service';
import { MatDialogRef, MatDialog } from '@angular/material/dialog';
import { AlbumService } from 'app/services/album.service';
import { Observable } from 'rxjs';
import { Album } from 'app/model/album.model';


@Component({
  selector: 'add-to-album',
  templateUrl: 'add-photo-to-album-dialog.html',
  styleUrls: ['add-photo-to-album-dialog.css']
})
export class AddPhotoToAlbumDialog {

  scrolled: boolean = false;
  showAddDiv: boolean = false;
  albumName: string;

  constructor(public dialogRef: MatDialogRef<AddPhotoToAlbumDialog>,
    private albumService: AlbumService,
    private sectionService: SectionService) {
  }

  onNoClick(): void {
    this.dialogRef.close();
  }

  onScroll(): void {
    let ele = document.getElementsByClassName('albums-container')[0];
    this.scrolled = ele.scrollTop > 0;
  }

  onPreAddClick(): void {
    if (this.showAddDiv) {
      return;
    }
    this.showAddDiv = true;
  }

  albums():Album[]{
    return this.albumService.getAlbums();
  }

  onAddAlbum(): void {
    console.log(this.albumName);
    if (!this.albumName || !this.albumName.length) {
      alert('请输入相册名称');
      return;
    }
    this.albumService.addAlbum(this.albumName).subscribe(resp => {
      this.albumName = null;
      this.showAddDiv = false;
    });
  }

  addToAlbum(album: Album): void {
    let photoId: string[] = this.sectionService.getSelectedPhotoId();
    if (!photoId || !photoId.length) {
      alert('未选择任何照片');
      return;
    }
    this.albumService.addPhotoToAlbum(album.id, photoId).subscribe(resp => {
      if (resp.code === 200) {
        this.sectionService.addAlbumToSelectedPhoto(album.id);
        this.onNoClick();
      }
    });
  }
}


@Component({
  selector: 'app-selected-bar',
  templateUrl: './selected-bar.component.html',
  styleUrls: ['./selected-bar.component.css']
})
export class SelectedBarComponent implements OnInit {

  constructor(private sectionService: SectionService,
    public dialog: MatDialog) { }

  ngOnInit() {
  }

  addToAlbum(): void {
    const dialogRef = this.dialog.open(AddPhotoToAlbumDialog, {
      width: '380px',
      height: '485px',
      panelClass: 'add-to-album',
    });
    dialogRef.afterClosed().subscribe(result => {
      console.log('The dialog was closed');
    });
  }

  hasSelectedBlocks(): boolean {
    return this.sectionService.numberOfSelectedBlocks() > 0;
  }

  selectedBlocks(): number {
    return this.sectionService.numberOfSelectedBlocks();
  }

  clearSelections(): void {
    this.sectionService.clearSelections();
  }

}

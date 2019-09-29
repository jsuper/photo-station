import { Component, OnInit } from '@angular/core';
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

  albums: Album[] = [];
  scrolled: boolean = false;

  constructor(public dialogRef: MatDialogRef<AddPhotoToAlbumDialog>,
    private albumService: AlbumService) {
    albumService.listAlbums().subscribe(resp => this.albums = resp);
  }

  onNoClick(): void {
    this.dialogRef.close();
  }

  onScroll(): void {
    let ele = document.getElementsByClassName('albums-container')[0];
    this.scrolled = ele.scrollTop > 0;
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

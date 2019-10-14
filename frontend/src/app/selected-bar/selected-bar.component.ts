import { Component, OnInit, ElementRef, Inject } from '@angular/core';
import { SectionService } from 'app/sections/section.service';
import { MatDialogRef, MatDialog, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AlbumService } from 'app/services/album.service';
import { Observable } from 'rxjs';
import { Album } from 'app/model/album.model';
import { MatButton } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PhotoService } from 'app/photo.service';


@Component({
  selector: 'add-to-album',
  templateUrl: 'add-photo-to-album-dialog.html',
  styleUrls: ['add-photo-to-album-dialog.css']
})
export class AddPhotoToAlbumDialog {

  scrolled: boolean = false;
  showAddDiv: boolean = false;
  albumName: string;
  canClickAdd: boolean = true;
  constructor(public dialogRef: MatDialogRef<AddPhotoToAlbumDialog>,
    private albumService: AlbumService,
    private sectionService: SectionService,
    private _snackBar: MatSnackBar,) {
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

  albums(): Album[] {
    return this.albumService.getAlbums();
  }

  onAddAlbum(): void {
    console.log(this.albumName);
    if (!this.albumName || !this.albumName.length) {
      alert('请输入相册名称');
      return;
    }
    console.log(this.canClickAdd);

    if (this.canClickAdd) {
      this.canClickAdd = false;
      this.albumService.addAlbum(this.albumName,()=>{
        this.albumName = null;
        this.showAddDiv = false;
        this.canClickAdd = true;
      });
    }
  }

  addToAlbum(album: Album): void {
    let photoId: string[] = this.sectionService.getSelectedPhotoId();
    if (!photoId || !photoId.length) {
      alert('未选择任何照片');
      return;
    }
    let sb = this._snackBar.open(`正在将照片添加至相册${album.name}...`) ;
    this.onNoClick();
    this.albumService.addPhotoToAlbum(album.id, photoId).subscribe(resp => {
      if (resp.code === 200) {
        this.sectionService.addAlbumToSelectedPhoto(album.id);
      }
      sb.dismiss() ;
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
    public dialog: MatDialog,
    private _snackBar: MatSnackBar,
    private _photoService: PhotoService) { }

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

  addToFavorites(): void {
    let sb = this._snackBar.open('正在将图片添加至收藏...');
    let photos: string[] = this.sectionService.getSelectedPhotoId();
    if (photos && photos.length > 0) {
      this._photoService.addToFavorite(photos)
        .subscribe(resp => {
          console.log(resp);

          this.sectionService.clearSelections();
          sb.dismiss();
          setTimeout(() => {
            window.location.reload();
          }, 500);
        });
    } else {
      sb.dismiss();
    }
  }


  showDeleteConfirm(removeTrash: MatButton): void {
    console.log(removeTrash);

    let target = removeTrash._elementRef.nativeElement;

    let offsetLeft = target.offsetLeft;
    let top = target.offsetTop;
    let height = target.offsetHeight;
    const confirmTrash = this.dialog.open(RemoveTrashConfirmDialog, {
      hasBackdrop: true,
      panelClass: 'remove-trash-dialog',
      position: {
        top: (top + height) + 'px',
        right: (window.innerWidth - offsetLeft - height - Math.ceil(height / 2)) + 'px',
      }
    });

  }

}

@Component({
  template: `
  <div mat-dialog-content>
    <div style="display:flex;letter-spacing: .00625em; font-family: 'Google Sans',Roboto,Arial,sans-serif;
    font-size: 1rem; font-weight: 500; line-height: 1.5rem;">
      <span style="flex-grow: 1;flex-shrink: 1;
      overflow: hidden;
      word-wrap: break-word;color: rgba(0,0,0,0.54);
      font: 400 1rem/1.5rem Roboto,Arial,sans-serif;
      line-height: 24px;">要将当前选中的照片移动到回收站吗？</span>
    </div>
  </div>
  <div mat-dialog-actions style="justify-content: flex-end;">
    <button mat-button (click)="onCancelClicked();">取消</button>
    <button mat-button cdkFocusInitial (click)="movePhotoToTrash()">移到回收站</button>
  </div>`,
  selector: 'remove-trash-confirm',
})
export class RemoveTrashConfirmDialog {

  constructor(
    public dialogRef: MatDialogRef<RemoveTrashConfirmDialog>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private _snackBar: MatSnackBar,
    private sectionService: SectionService,
    private photoService: PhotoService) { }

  onCancelClicked(): void {
    this.dialogRef.close();
  }

  movePhotoToTrash(): void {
    console.log("Move photos to trash");
    let sb = this._snackBar.open('正在移至回收站...');
    let allPhotos: string[] = this.sectionService.getSelectedPhotoId();
    if (allPhotos && allPhotos.length) {
      this.photoService.delete(allPhotos)
        .subscribe(resp => {
          console.log(resp);
          sb.dismiss();
          window.location.reload();
        });
    }
    this.sectionService.clearSelections();
    this.onCancelClicked();
  }
}

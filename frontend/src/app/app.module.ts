import { BrowserModule } from '@angular/platform-browser';
import { NgModule, LOCALE_ID } from '@angular/core';
import { FlexLayoutModule } from "@angular/flex-layout";
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';
import { registerLocaleData } from '@angular/common';
import localeZhHans from "@angular/common/locales/zh-Hans";

import { InfiniteScrollModule } from 'ngx-infinite-scroll';
import { AppRoutingModule } from 'app/app-routing.module';
import { AppComponent } from 'app/app.component';
import { MaterialModule } from "app/material.module";
import { PhotoViewerComponent } from "app/photo-viewer/photo-viewer.component";
import { PhotoUploaderComponent } from './photo-uploader/photo-uploader.component';
import { FormsModule } from '@angular/forms';
import { PhotoJustifyDisplayComponent } from './photo-justify-display/photo-justify-display.component';
import { PhotoGroupListComponent } from './photo-group-list/photo-group-list.component';
import { SelectedBarComponent, AddPhotoToAlbumDialog } from './selected-bar/selected-bar.component';
import { GroupsComponent } from './groups/groups.component';

registerLocaleData(localeZhHans, 'zh-Hans');

@NgModule({
  declarations: [
    AppComponent,
    PhotoViewerComponent,
    PhotoUploaderComponent,
    PhotoJustifyDisplayComponent,
    PhotoGroupListComponent,
    SelectedBarComponent,
    AddPhotoToAlbumDialog,
    GroupsComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    MaterialModule,
    FlexLayoutModule,
    InfiniteScrollModule,
    HttpClientModule,
    FormsModule
  ],
  entryComponents: [
    PhotoViewerComponent, AddPhotoToAlbumDialog],
  providers: [{ provide: LOCALE_ID, useValue: 'zh-Hans' }],
  bootstrap: [AppComponent]
})
export class AppModule { }

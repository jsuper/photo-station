import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FlexLayoutModule } from "@angular/flex-layout";
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';

import { InfiniteScrollModule } from 'ngx-infinite-scroll';
import { AppRoutingModule } from 'app/app-routing.module';
import { AppComponent } from 'app/app.component';
import { MaterialModule } from "app/material.module";
import { PhotoDisplayComponent } from "app/photo-display/photo-display.component";
import { NavMenuComponent } from "app/layout/nav-menu/nav-menu.component";
import { NavItemComponent } from "app/layout/nav-item/nav-item.component";
import { PhotoViewerComponent } from "app/photo-viewer/photo-viewer.component";
import { PhotoUploaderComponent } from './photo-uploader/photo-uploader.component';
import { FormsModule } from '@angular/forms';


@NgModule({
  declarations: [
    AppComponent,
    PhotoDisplayComponent,
    NavMenuComponent,
    NavItemComponent,
    PhotoViewerComponent,
    PhotoUploaderComponent,
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
    PhotoViewerComponent,],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }

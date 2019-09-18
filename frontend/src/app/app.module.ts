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
import { PhotoDisplayComponent } from "app/photo-display/photo-display.component";
import { NavMenuComponent } from "app/layout/nav-menu/nav-menu.component";
import { NavItemComponent } from "app/layout/nav-item/nav-item.component";
import { PhotoViewerComponent } from "app/photo-viewer/photo-viewer.component";
import { PhotoUploaderComponent } from './photo-uploader/photo-uploader.component';
import { FormsModule } from '@angular/forms';

registerLocaleData(localeZhHans, 'zh-Hans');

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
  providers: [{ provide: LOCALE_ID, useValue: 'zh-Hans' }],
  bootstrap: [AppComponent]
})
export class AppModule { }

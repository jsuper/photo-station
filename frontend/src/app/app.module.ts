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


@NgModule({
  declarations: [
    AppComponent,
    PhotoDisplayComponent,
    NavMenuComponent,
    NavItemComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    MaterialModule,
    FlexLayoutModule,
    InfiniteScrollModule,
    HttpClientModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }

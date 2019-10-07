import { Component, OnInit, ElementRef } from '@angular/core';
import { Box } from 'app/flex-layout/flex-layout.model';
import { FlexLayoutService } from 'app/flex-layout/flex-layout.service';
import { AlbumService } from 'app/services/album.service';
import { Album } from 'app/model/album.model';

@Component({
  selector: 'app-groups',
  templateUrl: './groups.component.html',
  styleUrls: ['./groups.component.css']
})
export class GroupsComponent implements OnInit {

  constructor(private el: ElementRef,
    private albumService:AlbumService) { }

  ngOnInit() {


  }

  getAlbums():Album[] {
    return this.albumService.getAlbums();
  }

}

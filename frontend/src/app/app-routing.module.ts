import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { PhotoGroupListComponent } from './photo-group-list/photo-group-list.component';
import { GroupsComponent } from './groups/groups.component';
import { PhotoJustifyDisplayComponent } from './photo-justify-display/photo-justify-display.component';
import { FlexPhotoComponent } from './common/flex-photo/flex-photo.component';


const routes: Routes = [
  { path: 'photos', component: PhotoGroupListComponent },
  { path: 'photos/:field', component: PhotoGroupListComponent },
  { path: 'groups', component: PhotoGroupListComponent },
  { path: 'albums', component: GroupsComponent, data: { 'field': 'album' } },
  { path: 'album/:id', component: PhotoJustifyDisplayComponent, outlet: 'popup' },
  { path: 'deleted', component: FlexPhotoComponent, data: { 'field': 'deleted', 'query': '1' } },
  { path: 'favorites', component: FlexPhotoComponent, data: { 'field': 'favorite', 'query': '1' } },
  { path: '', redirectTo: '/photos', pathMatch: 'full' },
  { path: '**', redirectTo: '/photos', pathMatch: 'full' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }

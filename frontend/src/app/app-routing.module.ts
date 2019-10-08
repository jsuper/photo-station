import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { PhotoGroupListComponent } from './photo-group-list/photo-group-list.component';
import { GroupsComponent } from './groups/groups.component';
import { PhotoJustifyDisplayComponent } from './photo-justify-display/photo-justify-display.component';


const routes: Routes = [
  { path: '', redirectTo: '/photos', pathMatch: 'full', data: { 'q': '' } },
  { path: 'photos', component: PhotoGroupListComponent },
  { path: 'photos/:field', component: PhotoGroupListComponent },
  { path: 'groups', component: PhotoGroupListComponent },
  { path: 'albums', component: GroupsComponent, data: { 'field': 'album' } },
  { path: 'album/:id', component: PhotoJustifyDisplayComponent, outlet: 'popup' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }

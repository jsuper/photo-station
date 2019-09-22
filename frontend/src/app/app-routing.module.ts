import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { PhotoDisplayComponent } from './photo-display/photo-display.component';
import { PhotoJustifyDisplayComponent } from './photo-justify-display/photo-justify-display.component';
import { PhotoGroupListComponent } from './photo-group-list/photo-group-list.component';


const routes: Routes = [
  { path: '', redirectTo: '/photos', pathMatch: 'full', data: { 'q': '' } },
  { path: 'photos', component: PhotoJustifyDisplayComponent },
  { path: 'photos/:field', component: PhotoJustifyDisplayComponent },
  { path: 'groups', component: PhotoGroupListComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }

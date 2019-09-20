import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { PhotoDisplayComponent } from './photo-display/photo-display.component';
import { PhotoJustifyDisplayComponent } from './photo-justify-display/photo-justify-display.component';


const routes: Routes = [
  { path: '', redirectTo: '/photos', pathMatch: 'full', data: { 'q': '' } },
  { path: 'photos', component: PhotoDisplayComponent },
  { path: 'photos/:field', component: PhotoDisplayComponent },
  { path: 'justify', component: PhotoJustifyDisplayComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }

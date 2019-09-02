import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { PhotoDisplayComponent } from './photo-display/photo-display.component';


const routes: Routes = [
  // { path: '', redirectTo: '/photos', pathMatch: 'full' },
  { path: 'photos', component: PhotoDisplayComponent },
  { path: 'photos/:field', component: PhotoDisplayComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }

import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { EMPTY, catchError, finalize, forkJoin, map, of, switchMap, tap } from 'rxjs';
import { ProductTableComponent } from './components/product-table/product-table.component';
import { ProcessingRunsTableComponent } from './components/processing-runs-table/processing-runs-table.component';
import { UploadPanelComponent } from './components/upload-panel/upload-panel.component';
import {
  Product,
  ProcessSelection,
  ProcessingRunResponse
} from './models/product.models';
import { ProductApiService } from './services/product-api.service';
import { LanguageService } from './services/language.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    AccordionModule,
    ButtonModule,
    CardModule,
    DialogModule,
    MessageModule,
    TagModule,
    ToolbarModule,
    UploadPanelComponent,
    ProductTableComponent,
    ProcessingRunsTableComponent
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  readonly i18n = inject(LanguageService);
  readonly products = signal<Product[]>([]);
  readonly processingRun = signal<ProcessingRunResponse | null>(null);
  readonly processingRuns = signal<ProcessingRunResponse[]>([]);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);
  uploadDialogVisible = false;

  constructor(private readonly api: ProductApiService) {
  }

  ngOnInit(): void {
    this.loadLatestRun();
    this.loadProcessingRuns();
  }

  process(selection: ProcessSelection): void {
    this.busy.set(true);
    this.errorMessage.set(null);

    forkJoin({
      csv: this.api.uploadCsv(selection.csvFile),
      json: this.api.uploadJson(selection.jsonFile)
    }).pipe(
      switchMap(({ csv, json }) => this.api.process({
        csvBatchId: csv.batchId,
        jsonBatchId: json.batchId,
        primarySource: selection.primarySource,
        fallbackMissingValues: selection.fallbackMissingValues
      })),
      switchMap(run => this.api.getProducts({
        runId: run.runId
      }).pipe(map(products => ({ run, products })))),
      tap(({ run, products }) => {
        this.processingRun.set(run);
        this.processingRuns.update(runs => [run, ...runs.filter(existingRun => existingRun.runId !== run.runId)]);
        this.products.set(products);
        this.uploadDialogVisible = false;
      }),
      catchError(error => {
        this.uploadDialogVisible = false;
        this.errorMessage.set(this.errorMessageFrom(error));
        return EMPTY;
      }),
      finalize(() => this.busy.set(false))
    ).subscribe();
  }

  selectProcessingRun(run: ProcessingRunResponse): void {
    if (this.busy() || this.processingRun()?.runId === run.runId) {
      return;
    }

    this.busy.set(true);
    this.errorMessage.set(null);
    this.api.getProducts({ runId: run.runId }).pipe(
      tap(products => {
        this.processingRun.set(run);
        this.products.set(products);
      }),
      catchError(error => {
        this.errorMessage.set(this.errorMessageFrom(error));
        return EMPTY;
      }),
      finalize(() => this.busy.set(false))
    ).subscribe();
  }

  private loadLatestRun(): void {
    this.busy.set(true);
    this.errorMessage.set(null);
    this.api.getLatestRun().pipe(
      switchMap(run => this.api.getProducts({
        runId: run.runId
      }).pipe(map(products => ({ run, products })))),
      tap(({ run, products }) => {
        this.processingRun.set(run);
        this.products.set(products);
      }),
      catchError(error => {
        if (error.status !== 404) {
          this.errorMessage.set(this.errorMessageFrom(error));
        }
        return EMPTY;
      }),
      finalize(() => this.busy.set(false))
    ).subscribe();
  }

  private loadProcessingRuns(): void {
    this.api.getProcessingRuns().pipe(
      catchError(error => {
        if (error.status !== 404) {
          this.errorMessage.set(this.errorMessageFrom(error));
        }
        return of([] as ProcessingRunResponse[]);
      })
    ).subscribe(runs => this.processingRuns.set(runs));
  }

  validCount(): number {
    return this.products().filter(product => product.valid).length;
  }

  invalidCount(): number {
    return this.products().filter(product => !product.valid).length;
  }

  formatProcessingDate(value: string): string {
    return new Intl.DateTimeFormat(this.i18n.locale(), {
      dateStyle: 'short',
      timeStyle: 'short'
    }).format(new Date(value));
  }

  private errorMessageFrom(error: { status?: number }): string {
    if (error.status === 400) {
      return this.i18n.t('errors.badRequest');
    }
    if (error.status === 404) {
      return this.i18n.t('errors.notFound');
    }
    if ((error.status ?? 0) >= 500) {
      return this.i18n.t('errors.server');
    }
    return this.i18n.t('errors.generic');
  }
}

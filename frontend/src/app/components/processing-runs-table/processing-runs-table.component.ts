import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ProcessingRunResponse } from '../../models/product.models';
import { LanguageService } from '../../services/language.service';

@Component({
  selector: 'app-processing-runs-table',
  standalone: true,
  imports: [CommonModule, FormsModule, InputTextModule, SelectModule, TableModule, TagModule],
  templateUrl: './processing-runs-table.component.html'
})
export class ProcessingRunsTableComponent {
  readonly i18n = inject(LanguageService);
  @Input({ required: true }) runs: ProcessingRunResponse[] = [];
  @Input() selectedRunId: number | null = null;
  @Output() readonly runSelected = new EventEmitter<ProcessingRunResponse>();
  readonly sourceOptions = computed(() => [
    { label: this.i18n.t('common.csv'), value: 'CSV' },
    { label: this.i18n.t('common.json'), value: 'JSON' }
  ]);
  readonly fallbackOptions = computed(() => [
    { label: this.i18n.t('common.yes'), value: true },
    { label: this.i18n.t('common.no'), value: false }
  ]);

  selectRun(run: ProcessingRunResponse): void {
    this.runSelected.emit(run);
  }

  formatDate(value: string): string {
    return new Intl.DateTimeFormat(this.i18n.locale(), {
      dateStyle: 'short',
      timeStyle: 'short'
    }).format(new Date(value));
  }
}

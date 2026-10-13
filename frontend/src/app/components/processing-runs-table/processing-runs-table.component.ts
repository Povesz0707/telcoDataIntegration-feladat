import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnDestroy, Output, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePickerModule } from 'primeng/datepicker';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ProcessingRunResponse } from '../../models/product.models';
import { LanguageService } from '../../services/language.service';

type ProcessingRunTableRow = ProcessingRunResponse & {
  startedAtDate: string;
  startedAtDisplay: string;
};

@Component({
  selector: 'app-processing-runs-table',
  standalone: true,
  imports: [CommonModule, DatePickerModule, FormsModule, InputNumberModule, InputTextModule, SelectModule, TableModule, TagModule],
  templateUrl: './processing-runs-table.component.html'
})
export class ProcessingRunsTableComponent implements OnDestroy {
  readonly i18n = inject(LanguageService);
  private readonly filterDelayMs = 350;
  private readonly filterTimers = new Map<string, ReturnType<typeof setTimeout>>();
  private readonly parsedDateFilters = new Map<string, Date>();
  readonly tableLoading = signal(false);
  private tableRuns: ProcessingRunTableRow[] = [];
  @Input({ required: true })
  set runs(value: ProcessingRunResponse[]) {
    this.tableRuns = value.map(run => ({
      ...run,
      startedAtDate: this.formatFilterDate(new Date(run.startedAt)),
      startedAtDisplay: this.formatDate(run.startedAt)
    }));
  }
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

  scheduleFilter(field: string, filterCallback: (value: unknown) => void, value: unknown): void {
    this.tableLoading.set(true);

    const previousTimer = this.filterTimers.get(field);
    if (previousTimer) {
      clearTimeout(previousTimer);
    }

    const timer = setTimeout(() => {
      filterCallback(value === '' ? null : value);
      this.filterTimers.delete(field);

      if (this.filterTimers.size === 0) {
        this.tableLoading.set(false);
      }
    }, this.filterDelayMs);

    this.filterTimers.set(field, timer);
  }

  filterTextValue(value: unknown): string {
    return value === null || value === undefined ? '' : String(value);
  }

  filterNumberValue(value: unknown): number | null {
    return typeof value === 'number' ? value : null;
  }

  dateFilterInputValue(value: unknown): Date | null {
    if (typeof value !== 'string' || value.length === 0) {
      return null;
    }

    const cachedValue = this.parsedDateFilters.get(value);
    if (cachedValue) {
      return cachedValue;
    }

    const [year, month, day] = value.split('-').map(Number);
    if (![year, month, day].every(Number.isInteger)) {
      return null;
    }

    const parsedDate = new Date(year, month - 1, day);
    this.parsedDateFilters.set(value, parsedDate);
    return parsedDate;
  }

  onDateFilterChange(filterCallback: (value: unknown) => void, value: Date | null): void {
    this.scheduleFilter('startedAtDate', filterCallback, value ? this.formatFilterDate(value) : null);
  }

  onDateFilterClear(filterCallback: (value: unknown) => void): void {
    this.scheduleFilter('startedAtDate', filterCallback, null);
  }

  private formatFilterDate(value: Date): string {
    const year = value.getFullYear();
    const month = String(value.getMonth() + 1).padStart(2, '0');
    const day = String(value.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  get displayedRuns(): ProcessingRunTableRow[] {
    return this.tableRuns;
  }

  formatDate(value: string): string {
    return new Intl.DateTimeFormat(this.i18n.locale(), {
      dateStyle: 'short',
      timeStyle: 'short'
    }).format(new Date(value));
  }

  ngOnDestroy(): void {
    this.filterTimers.forEach(timer => clearTimeout(timer));
    this.filterTimers.clear();
    this.parsedDateFilters.clear();
  }
}

import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TagModule } from 'primeng/tag';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { LanguageService } from '../../services/language.service';
import { ProcessSelection, SourceType } from '../../models/product.models';

@Component({
  selector: 'app-upload-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, ButtonModule, DividerModule, SelectButtonModule, TagModule, ToggleSwitchModule],
  templateUrl: './upload-panel.component.html'
})
export class UploadPanelComponent {
  readonly i18n = inject(LanguageService);
  readonly sourceType = SourceType;
  @Input() busy = false;
  @Output() readonly processRequested = new EventEmitter<ProcessSelection>();

  csvFile: File | null = null;
  jsonFile: File | null = null;
  csvDropActive = false;
  jsonDropActive = false;
  csvError: string | null = null;
  jsonError: string | null = null;
  primarySource: SourceType = SourceType.JSON;
  fallbackMissingValues = true;
  readonly maxFileSize = 5_000_000;

  readonly sourceOptions = computed(() => [
    { label: this.i18n.t('common.csv'), value: SourceType.CSV },
    { label: this.i18n.t('common.json'), value: SourceType.JSON }
  ]);

  handleFileInput(event: Event, source: SourceType): void {
    const input = event.target as HTMLInputElement;
    this.setFile(input.files?.[0] ?? null, source);
    input.value = '';
  }

  dragOver(event: DragEvent, source: SourceType): void {
    event.preventDefault();
    this.setDropActive(source, true);
  }

  dragLeave(event: DragEvent, source: SourceType): void {
    event.preventDefault();
    this.setDropActive(source, false);
  }

  dropFile(event: DragEvent, source: SourceType): void {
    event.preventDefault();
    this.setDropActive(source, false);
    this.setFile(event.dataTransfer?.files?.[0] ?? null, source);
  }

  removeFile(source: SourceType): void {
    if (source === SourceType.CSV) {
      this.csvFile = null;
      this.csvError = null;
    } else {
      this.jsonFile = null;
      this.jsonError = null;
    }
  }

  isDropActive(source: SourceType): boolean {
    return source === SourceType.CSV ? this.csvDropActive : this.jsonDropActive;
  }

  fileError(source: SourceType): string | null {
    return source === SourceType.CSV ? this.csvError : this.jsonError;
  }

  dropDescription(source: SourceType): string {
    return source === SourceType.CSV
      ? this.i18n.t('upload.csvDescription')
      : this.i18n.t('upload.jsonDescription');
  }

  formatFileSize(size: number): string {
    return `${(size / 1024).toFixed(1)} KB`;
  }

  private setFile(file: File | null, source: SourceType): void {
    this.setFileError(source, null);
    if (!file) {
      return;
    }

    const extension = file.name.toLowerCase();
    const validType = source === SourceType.CSV ? extension.endsWith('.csv') : extension.endsWith('.json');
    if (!validType) {
      this.setFileError(source, this.i18n.t('upload.invalidType'));
      return;
    }

    if (file.size > this.maxFileSize) {
      this.setFileError(source, this.i18n.t('upload.invalidSize'));
      return;
    }

    if (source === SourceType.CSV) {
      this.csvFile = file;
    } else {
      this.jsonFile = file;
    }
  }

  private setDropActive(source: SourceType, active: boolean): void {
    if (source === SourceType.CSV) {
      this.csvDropActive = active;
    } else {
      this.jsonDropActive = active;
    }
  }

  private setFileError(source: SourceType, error: string | null): void {
    if (source === SourceType.CSV) {
      this.csvError = error;
    } else {
      this.jsonError = error;
    }
  }

  startProcessing(): void {
    if (!this.csvFile || !this.jsonFile || this.busy) {
      return;
    }
    this.processRequested.emit({
      csvFile: this.csvFile,
      jsonFile: this.jsonFile,
      primarySource: this.primarySource,
      fallbackMissingValues: this.fallbackMissingValues
    });
  }
}

import { Component } from '@angular/core';
import { Workbook } from 'exceljs';

type ComplianceStatus = 'ok' | 'warn' | 'danger';

interface CoilType {
  id: string;
  code: string;
  label: string;
  destination: string;
  quantity: number;
  weightT: number;
  outerDiameterM: number;
  widthM: number;
  color: string;
}

interface PlannedCoil {
  id: string;
  typeId: string;
  code: string;
  destination: string;
  weightT: number;
  outerDiameterM: number;
  widthM: number;
  color: string;
  holdId: number;
  row: number;
  tier: number;
  x: number;
  y: number;
}

interface Hold {
  id: number;
  name: string;
  lengthM: number;
  beamM: number;
  tanktopLimit: number;
  maxWeightT: number;
  lcgM: number;
  coils: PlannedCoil[];
}

interface RowSummary {
  row: number;
  coils: number;
  usedLength: number;
  clearance: number;
  dunnages: number;
  utilisation: number;
  status: ComplianceStatus;
}

interface HoldMetrics {
  weight: number;
  utilisation: number;
  tanktopPressure: number;
  tanktopStatus: ComplianceStatus;
  cogX: number;
  cogY: number;
  cogZ: number;
}

interface AlertItem {
  label: string;
  detail: string;
  status: ComplianceStatus;
}

@Component({
  selector: 'app-steel-coil-planner',
  templateUrl: './steel-coil-planner.component.html',
  styleUrls: ['./steel-coil-planner.component.css'],
  standalone: false
})
export class SteelCoilPlannerComponent {
  readonly ports = ['Santos', 'Rio Grande', 'Vitoria', 'Cartagena'];
  readonly rowsPerHold = 6;
  readonly maxTiers = 3;
  readonly dunnageSectionMm = '60 x 80';

  selectedHoldId = 1;
  sectionHoldId = 1;
  dragTypeId: string | null = null;
  showAutoModal = false;
  importError: string | null = null;
  manifestSource = 'Manifesto exemplo';

  coilTypes: CoilType[] = [
    { id: 'hrc-a', code: 'HRC-A', label: 'Hot rolled coil A', destination: 'Cartagena', quantity: 18, weightT: 24.5, outerDiameterM: 1.85, widthM: 1.55, color: '#58a6ff' },
    { id: 'hrc-b', code: 'HRC-B', label: 'Hot rolled coil B', destination: 'Vitoria', quantity: 16, weightT: 21.8, outerDiameterM: 1.72, widthM: 1.42, color: '#3fb950' },
    { id: 'crc-c', code: 'CRC-C', label: 'Cold rolled coil C', destination: 'Rio Grande', quantity: 14, weightT: 18.4, outerDiameterM: 1.48, widthM: 1.25, color: '#f78166' },
    { id: 'gal-d', code: 'GAL-D', label: 'Galvanized coil D', destination: 'Santos', quantity: 12, weightT: 16.2, outerDiameterM: 1.36, widthM: 1.18, color: '#d2a8ff' },
    { id: 'plate-e', code: 'PLT-E', label: 'Heavy plate coil E', destination: 'Cartagena', quantity: 10, weightT: 29.6, outerDiameterM: 1.95, widthM: 1.68, color: '#ffa657' }
  ];

  readonly acceptedColumns = [
    'coil_id',
    'codigo',
    'quantidade',
    'peso_toneladas',
    'largura_mm',
    'diametro_externo_mm',
    'porto_descarga',
    'cliente',
    'grade_aco',
    'observacoes'
  ];

  readonly holds: Hold[] = [
    { id: 1, name: 'Hold 1', lengthM: 24, beamM: 18, tanktopLimit: 18, maxWeightT: 620, lcgM: 28, coils: [] },
    { id: 2, name: 'Hold 2', lengthM: 28, beamM: 20, tanktopLimit: 21, maxWeightT: 820, lcgM: 58, coils: [] },
    { id: 3, name: 'Hold 3', lengthM: 30, beamM: 21, tanktopLimit: 22, maxWeightT: 920, lcgM: 88, coils: [] },
    { id: 4, name: 'Hold 4', lengthM: 28, beamM: 20, tanktopLimit: 21, maxWeightT: 820, lcgM: 118, coils: [] },
    { id: 5, name: 'Hold 5', lengthM: 24, beamM: 18, tanktopLimit: 18, maxWeightT: 620, lcgM: 148, coils: [] }
  ];

  get selectedHold(): Hold {
    return this.holds.find((hold) => hold.id === this.selectedHoldId) ?? this.holds[0];
  }

  get sectionHold(): Hold {
    return this.holds.find((hold) => hold.id === this.sectionHoldId) ?? this.holds[0];
  }

  get totalPlanned(): number {
    return this.holds.reduce((total, hold) => total + hold.coils.length, 0);
  }

  get totalWeight(): number {
    return this.holds.reduce((total, hold) => total + hold.coils.reduce((sum, coil) => sum + coil.weightT, 0), 0);
  }

  get totalAvailable(): number {
    return this.coilTypes.reduce((total, type) => total + type.quantity, 0);
  }

  get gm(): number {
    return Math.max(0.8, 2.8 - this.verticalCog / 8);
  }

  get trim(): number {
    const forward = this.holds.slice(0, 2).reduce((sum, hold) => sum + this.metricsFor(hold).weight, 0);
    const aft = this.holds.slice(3).reduce((sum, hold) => sum + this.metricsFor(hold).weight, 0);
    return (aft - forward) / 900;
  }

  get draftForward(): number {
    return 7.42 - this.trim / 2 + this.totalWeight / 4200;
  }

  get draftAft(): number {
    return 7.42 + this.trim / 2 + this.totalWeight / 4200;
  }

  get draftMean(): number {
    return (this.draftForward + this.draftAft) / 2;
  }

  get shearForcePercent(): number {
    const middleLoad = this.metricsFor(this.holds[2]).weight;
    const endsLoad = this.metricsFor(this.holds[0]).weight + this.metricsFor(this.holds[4]).weight;
    return Math.min(99, Math.abs(middleLoad - endsLoad / 2) / 8);
  }

  get bendingMomentPercent(): number {
    const moment = this.holds.reduce((sum, hold) => sum + this.metricsFor(hold).weight * Math.abs(hold.lcgM - 88), 0);
    return Math.min(99, moment / 680);
  }

  get alerts(): AlertItem[] {
    return [
      {
        label: 'LIFO sequence',
        detail: 'Destinos posteriores ficam mais baixos ou mais a re.',
        status: this.hasLifoConflict() ? 'warn' : 'ok'
      },
      {
        label: 'Pyramid loading',
        detail: 'Tiers superiores nao excedem a base na mesma row.',
        status: this.hasPyramidConflict() ? 'danger' : 'ok'
      },
      {
        label: 'Dunnage',
        detail: `Madeira ${this.dunnageSectionMm} mm aplicada por contato.`,
        status: this.totalPlanned > 0 ? 'ok' : 'warn'
      },
      {
        label: 'Coil-key positioning',
        detail: 'Bobinas-chave alternadas nas extremidades de rows.',
        status: this.totalPlanned > 0 ? 'ok' : 'warn'
      },
      {
        label: 'Steel bands',
        detail: 'Bobinas acima de 25 t exigem cintas adicionais.',
        status: this.hasHeavyCoils() ? 'warn' : 'ok'
      }
    ];
  }

  remainingFor(type: CoilType): number {
    const planned = this.holds.reduce((total, hold) => total + hold.coils.filter((coil) => coil.typeId === type.id).length, 0);
    return type.quantity - planned;
  }

  holdRows(hold: Hold): RowSummary[] {
    return Array.from({ length: this.rowsPerHold }, (_, index) => {
      const row = index + 1;
      const coils = hold.coils.filter((coil) => coil.row === row);
      const usedLength = coils.reduce((sum, coil) => sum + coil.widthM + 0.12, 0);
      const clearance = Math.max(0, hold.lengthM - usedLength);
      const utilisation = Math.min(100, Math.round((usedLength / hold.lengthM) * 100));
      return {
        row,
        coils: coils.length,
        usedLength,
        clearance,
        dunnages: coils.length * 2,
        utilisation,
        status: utilisation > 94 ? 'danger' : utilisation > 84 ? 'warn' : 'ok'
      };
    });
  }

  metricsFor(hold: Hold): HoldMetrics {
    const weight = hold.coils.reduce((sum, coil) => sum + coil.weightT, 0);
    const contactArea = Math.max(1, hold.coils.reduce((sum, coil) => sum + coil.widthM * 0.8, 0));
    const tanktopPressure = weight / contactArea;
    const cogX = hold.coils.length ? hold.coils.reduce((sum, coil) => sum + coil.x * coil.weightT, 0) / weight : hold.lengthM / 2;
    const cogY = hold.coils.length ? hold.coils.reduce((sum, coil) => sum + coil.y * coil.weightT, 0) / weight : 0;
    const cogZ = hold.coils.length ? hold.coils.reduce((sum, coil) => sum + (0.8 + coil.tier * 1.05) * coil.weightT, 0) / weight : 0;
    return {
      weight,
      utilisation: Math.round((weight / hold.maxWeightT) * 100),
      tanktopPressure,
      tanktopStatus: tanktopPressure > hold.tanktopLimit ? 'danger' : tanktopPressure > hold.tanktopLimit * 0.85 ? 'warn' : 'ok',
      cogX,
      cogY,
      cogZ
    };
  }

  setSelectedHold(holdId: number): void {
    this.selectedHoldId = holdId;
    this.sectionHoldId = holdId;
  }

  onDragStart(typeId: string): void {
    this.dragTypeId = typeId;
  }

  onDrop(hold: Hold): void {
    if (!this.dragTypeId) {
      return;
    }
    this.addCoilToHold(this.dragTypeId, hold);
    this.dragTypeId = null;
  }

  openAutoPlan(): void {
    this.showAutoModal = true;
  }

  cancelAutoPlan(): void {
    this.showAutoModal = false;
  }

  generateAutoPlan(): void {
    this.clearPlan();
    const sortedTypes = [...this.coilTypes].sort((a, b) => this.portRank(b.destination) - this.portRank(a.destination));
    for (const type of sortedTypes) {
      for (let index = 0; index < type.quantity; index++) {
        const targetHold = this.pickBestHold(type);
        if (!targetHold) {
          break;
        }
        this.addCoilToHold(type.id, targetHold);
      }
    }
    this.showAutoModal = false;
  }

  clearPlan(): void {
    for (const hold of this.holds) {
      hold.coils = [];
    }
  }

  async importManifest(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) {
      return;
    }

    this.importError = null;
    try {
      const rows = file.name.toLowerCase().endsWith('.csv')
        ? this.parseCsv(await file.text())
        : await this.parseWorkbook(file);
      const coilTypes = this.mapRowsToCoils(rows);
      if (coilTypes.length === 0) {
        this.importError = 'A planilha nao possui bobinas validas para importar.';
        return;
      }
      this.clearPlan();
      this.coilTypes = coilTypes;
      this.manifestSource = file.name;
    } catch (error) {
      this.importError = error instanceof Error ? error.message : 'Nao foi possivel importar a planilha.';
    }
  }

  downloadTemplate(): void {
    const lines = [
      this.acceptedColumns.join(','),
      'C-1001,HRC-A,1,24.5,1550,1850,Cartagena,Cliente A,SAE1006,',
      'C-1002,HRC-A,1,24.5,1550,1850,Cartagena,Cliente A,SAE1006,',
      'C-2001,CRC-C,2,18.4,1250,1480,Rio Grande,Cliente B,DX51D,'
    ];
    const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'modelo_manifesto_bobinas.csv';
    link.click();
    URL.revokeObjectURL(url);
  }

  coilCx(coil: PlannedCoil): number {
    return 28 + (coil.y + this.sectionHold.beamM / 2) * (264 / this.sectionHold.beamM);
  }

  coilCy(coil: PlannedCoil): number {
    return 158 - coil.tier * 36;
  }

  coilRadius(coil: PlannedCoil): number {
    return Math.max(12, coil.outerDiameterM * 10);
  }

  private addCoilToHold(typeId: string, hold: Hold): void {
    const type = this.coilTypes.find((item) => item.id === typeId);
    if (!type || this.remainingFor(type) <= 0) {
      return;
    }
    const slot = this.nextSlot(hold);
    if (!slot) {
      return;
    }
    hold.coils = [
      ...hold.coils,
      {
        id: `${type.id}-${Date.now()}-${hold.coils.length}`,
        typeId: type.id,
        code: type.code,
        destination: type.destination,
        weightT: type.weightT,
        outerDiameterM: type.outerDiameterM,
        widthM: type.widthM,
        color: type.color,
        holdId: hold.id,
        ...slot
      }
    ];
  }

  private async parseWorkbook(file: File): Promise<Record<string, string>[]> {
    const workbook = new Workbook();
    await workbook.xlsx.load(await file.arrayBuffer());
    const worksheet = workbook.worksheets[0];
    if (!worksheet) {
      return [];
    }
    const headers = this.readWorksheetHeaders(worksheet.getRow(1).values as unknown[]);
    const rows: Record<string, string>[] = [];
    worksheet.eachRow((row, rowNumber) => {
      if (rowNumber === 1) {
        return;
      }
      const values = row.values as unknown[];
      const item: Record<string, string> = {};
      headers.forEach((header, index) => {
        item[header] = this.cellText(values[index + 1]);
      });
      if (Object.values(item).some((value) => value.length > 0)) {
        rows.push(item);
      }
    });
    return rows;
  }

  private parseCsv(text: string): Record<string, string>[] {
    const rows = text
      .replace(/^\uFEFF/, '')
      .split(/\r?\n/)
      .filter((line) => line.trim().length > 0)
      .map((line) => this.splitCsvLine(line));
    if (rows.length < 2) {
      return [];
    }
    const headers = rows[0].map((header) => this.normalizeHeader(header));
    return rows.slice(1).map((row) => {
      const item: Record<string, string> = {};
      headers.forEach((header, index) => item[header] = row[index]?.trim() ?? '');
      return item;
    });
  }

  private mapRowsToCoils(rows: Record<string, string>[]): CoilType[] {
    const grouped = new Map<string, CoilType>();
    rows.forEach((row, index) => {
      const code = this.firstValue(row, ['codigo', 'coil_id', 'coil', 'id']) || `COIL-${index + 1}`;
      const destination = this.firstValue(row, ['porto_descarga', 'destino', 'destination', 'port']) || 'Santos';
      const quantity = Math.max(1, Math.round(this.numberValue(this.firstValue(row, ['quantidade', 'qty', 'quantity']), 1)));
      const weightT = this.numberValue(this.firstValue(row, ['peso_toneladas', 'peso_t', 'weight_t', 'weight']), 0);
      const widthM = this.dimensionToMeters(this.firstValue(row, ['largura_mm', 'largura', 'width_mm', 'width']), 1.2);
      const outerDiameterM = this.dimensionToMeters(this.firstValue(row, ['diametro_externo_mm', 'diametro', 'od_mm', 'outer_diameter_mm']), 1.5);
      if (weightT <= 0) {
        return;
      }

      const key = `${code}|${destination}|${weightT}|${widthM}|${outerDiameterM}`;
      const existing = grouped.get(key);
      if (existing) {
        existing.quantity += quantity;
        return;
      }
      grouped.set(key, {
        id: `import-${grouped.size + 1}`,
        code: code.slice(0, 12),
        label: `${code} - ${destination}`,
        destination,
        quantity,
        weightT,
        outerDiameterM,
        widthM,
        color: this.paletteColor(grouped.size)
      });
    });
    return Array.from(grouped.values());
  }

  private readWorksheetHeaders(values: unknown[]): string[] {
    return values.slice(1).map((value) => this.normalizeHeader(this.cellText(value)));
  }

  private splitCsvLine(line: string): string[] {
    const values: string[] = [];
    let current = '';
    let quoted = false;
    for (let index = 0; index < line.length; index++) {
      const char = line[index];
      const next = line[index + 1];
      if (char === '"' && next === '"') {
        current += '"';
        index++;
      } else if (char === '"') {
        quoted = !quoted;
      } else if (char === ',' && !quoted) {
        values.push(current);
        current = '';
      } else {
        current += char;
      }
    }
    values.push(current);
    return values;
  }

  private normalizeHeader(value: string): string {
    return value
      .normalize('NFKD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim()
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '');
  }

  private firstValue(row: Record<string, string>, keys: string[]): string {
    for (const key of keys) {
      const normalizedKey = this.normalizeHeader(key);
      if (row[normalizedKey]) {
        return row[normalizedKey];
      }
    }
    return '';
  }

  private numberValue(value: string, fallback: number): number {
    const normalized = value.replace(/\./g, '').replace(',', '.').replace(/[^0-9.-]/g, '');
    const parsed = Number(normalized);
    return Number.isFinite(parsed) ? parsed : fallback;
  }

  private dimensionToMeters(value: string, fallback: number): number {
    const parsed = this.numberValue(value, fallback);
    return parsed > 20 ? parsed / 1000 : parsed;
  }

  private cellText(value: unknown): string {
    if (value == null) {
      return '';
    }
    if (typeof value === 'object' && 'text' in value) {
      return String((value as { text?: unknown }).text ?? '');
    }
    if (typeof value === 'object' && 'result' in value) {
      return String((value as { result?: unknown }).result ?? '');
    }
    return String(value).trim();
  }

  private paletteColor(index: number): string {
    const colors = ['#58a6ff', '#3fb950', '#f78166', '#d2a8ff', '#ffa657', '#2dd4bf', '#facc15', '#fb7185'];
    return colors[index % colors.length];
  }

  private nextSlot(hold: Hold): Pick<PlannedCoil, 'row' | 'tier' | 'x' | 'y'> | null {
    for (let tier = 1; tier <= this.maxTiers; tier++) {
      for (let row = 1; row <= this.rowsPerHold; row++) {
        const rowCoils = hold.coils.filter((coil) => coil.row === row && coil.tier === tier);
        const capacity = tier === 1 ? 5 : tier === 2 ? 4 : 3;
        if (rowCoils.length < capacity) {
          const lateralStep = hold.beamM / (capacity + 1);
          return {
            row,
            tier,
            x: (row - 0.5) * (hold.lengthM / this.rowsPerHold),
            y: -hold.beamM / 2 + lateralStep * (rowCoils.length + 1)
          };
        }
      }
    }
    return null;
  }

  private pickBestHold(type: CoilType): Hold | null {
    return [...this.holds]
      .filter((hold) => this.nextSlot(hold) && this.remainingFor(type) > 0)
      .sort((a, b) => {
        const metricA = this.metricsFor(a);
        const metricB = this.metricsFor(b);
        return metricA.utilisation - metricB.utilisation || Math.abs(a.id - 3) - Math.abs(b.id - 3);
      })[0] ?? null;
  }

  private get verticalCog(): number {
    if (this.totalWeight === 0) {
      return 0;
    }
    return this.holds.reduce((sum, hold) => sum + this.metricsFor(hold).cogZ * this.metricsFor(hold).weight, 0) / this.totalWeight;
  }

  private portRank(port: string): number {
    return this.ports.indexOf(port);
  }

  private hasHeavyCoils(): boolean {
    return this.holds.some((hold) => hold.coils.some((coil) => coil.weightT >= 25));
  }

  private hasPyramidConflict(): boolean {
    return this.holds.some((hold) => {
      for (let row = 1; row <= this.rowsPerHold; row++) {
        const base = hold.coils.filter((coil) => coil.row === row && coil.tier === 1).length;
        const top = hold.coils.filter((coil) => coil.row === row && coil.tier > 1).length;
        if (top > base) {
          return true;
        }
      }
      return false;
    });
  }

  private hasLifoConflict(): boolean {
    return this.holds.some((hold) => hold.coils.some((coil) => {
      const rank = this.portRank(coil.destination);
      return hold.coils.some((other) => other.row === coil.row && other.tier < coil.tier && this.portRank(other.destination) < rank);
    }));
  }
}

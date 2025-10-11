from __future__ import annotations
import csv, re, sys
from pathlib import Path
from typing import Iterable
from PyPDF2 import PdfReader

# === Import robusto del OCR (Paquete o script) ===
try:
    from .ocr_extractor import ocr_pdf_to_text  # cuando se ejecuta como paquete
except Exception:
    sys.path.append(str(Path(__file__).resolve().parent))
    from ocr_extractor import ocr_pdf_to_text   # cuando se llama .py directo

# === Rutas base ===
BASE_DIR = Path(__file__).resolve().parents[1]  # ...\denuncia_maria_fatima
RUTA_PDFS = BASE_DIR / "evidencia" / "pdfs"
SALIDA_CSV = BASE_DIR / "resultados" / "cronologia.csv"
TXT_DIR    = BASE_DIR / "resultados" / "textos"

def extraer_datos(pdf_path: Path) -> dict[str, str]:
    try:
        # 1) Extrae texto embebido
        reader = PdfReader(str(pdf_path))
        texto = ""
        for page in reader.pages:
            texto += page.extract_text() or ""

        # 2) Fallback OCR si no hay texto
        if not texto.strip():
            texto = ocr_pdf_to_text(pdf_path, dpi=240, lang="spa+eng")

        # Guarda el texto para trazabilidad
        TXT_DIR.mkdir(parents=True, exist_ok=True)
        (TXT_DIR / (pdf_path.stem + ".txt")).write_text(texto or "", encoding="utf-8")

        # Patrones simples (ajustaremos con tus docs reales)
        patron_fecha     = r"(\d{1,2}\s+de\s+[A-Za-záéíóúÁÉÍÓÚ]+\s+de\s+\d{4})"
        patron_notario   = r"Notario(?:a)?\s+P[úu]blico[^\n]*"
        patron_protocolo = r"[Ff]olio[s]?\s+n[oº°]?\s*\d+"

        fecha     = re.search(patron_fecha, texto or "")
        notario   = re.search(patron_notario, texto or "")
        protocolo = re.search(patron_protocolo, texto or "")

        return {
            "archivo":   pdf_path.name,
            "fecha":     fecha.group(0) if fecha else "",
            "notario":   notario.group(0) if notario else "",
            "protocolo": protocolo.group(0) if protocolo else "",
        }
    except Exception as e:
        return {"archivo": pdf_path.name, "fecha": "", "notario": "", "protocolo": f"ERROR: {e}"}

def procesar_pdf_en_carpeta(ruta: Path) -> list[dict[str, str]]:
    filas: list[dict[str, str]] = []
    if not ruta.is_dir():
        print(f"⚠️ Carpeta no encontrada: {ruta}")
        return filas
    for archivo in sorted(ruta.iterdir()):
        if archivo.suffix.lower() == ".pdf":
            filas.append(extraer_datos(archivo))
    return filas

def escribir_csv(destino: Path, filas: Iterable[dict[str, str]]) -> None:
    destino.parent.mkdir(parents=True, exist_ok=True)
    with destino.open("w", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=["archivo", "fecha", "notario", "protocolo"])
        writer.writeheader()
        writer.writerows(filas)

def main(argv=None) -> int:
    print(f"Procesando PDFs en: {RUTA_PDFS}")
    filas = procesar_pdf_en_carpeta(RUTA_PDFS)
    escribir_csv(SALIDA_CSV, filas)
    print(f"✅ Procesados {len(filas)} archivos. Resultados guardados en {SALIDA_CSV}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())

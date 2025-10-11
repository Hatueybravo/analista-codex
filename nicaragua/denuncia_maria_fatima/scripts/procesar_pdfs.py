"""Procesa los PDFs del caso y genera un CSV con la información relevante."""

from __future__ import annotations

import csv
from pathlib import Path
from typing import Iterable

try:  # pragma: no cover - permite `python archivo.py`
    from .ocr_extractor import OCRDependencyError, ocr_pdf_to_text
except ImportError:  # pragma: no cover - ejecución directa sin paquete
    from ocr_extractor import OCRDependencyError, ocr_pdf_to_text


# === CONFIGURACIÓN BASE ===
BASE_DIR = Path(__file__).resolve().parent.parent
RUTA_PDFS = BASE_DIR / "evidencia" / "pdfs"
SALIDA_CSV = BASE_DIR / "resultados" / "cronologia.csv"


def _buscar_patrones(texto: str) -> dict[str, str]:
    import re

    patron_fecha = r"(\d{1,2}\s+de\s+[A-Za-záéíóúÁÉÍÓÚ]+\s+de\s+\d{4})"
    patron_notario = r"Notario(?:a)?\s+P[úu]blico[^\n]*"
    patron_protocolo = r"[Ff]olio[s]?\s+n[oº°]?\s*\d+"

    fecha = re.search(patron_fecha, texto)
    notario = re.search(patron_notario, texto)
    protocolo = re.search(patron_protocolo, texto)

    return {
        "fecha": fecha.group(0) if fecha else "",
        "notario": notario.group(0) if notario else "",
        "protocolo": protocolo.group(0) if protocolo else "",
    }


def extraer_datos(pdf_path: Path) -> dict[str, str]:
    """Extrae la información relevante de un PDF."""

    try:
        texto = ocr_pdf_to_text(pdf_path)
        datos = _buscar_patrones(texto)
        datos.update({"archivo": pdf_path.name, "texto": texto, "error": ""})
        return datos
    except OCRDependencyError as err:
        return {"archivo": pdf_path.name, "error": str(err)}
    except Exception as err:  # pragma: no cover - ruta defensiva
        return {"archivo": pdf_path.name, "error": str(err)}


def procesar_pdf_en_carpeta(ruta: Path) -> list[dict[str, str]]:
    datos: list[dict[str, str]] = []
    if not ruta.exists():
        print(f"⚠️ Carpeta no encontrada: {ruta}")
        return datos
    for archivo in sorted(ruta.iterdir()):
        if archivo.suffix.lower() == ".pdf":
            datos.append(extraer_datos(archivo))
    return datos


def escribir_csv(destino: Path, filas: Iterable[dict[str, str]]) -> None:
    destino.parent.mkdir(parents=True, exist_ok=True)
    with destino.open("w", newline="", encoding="utf-8-sig") as f:
        fieldnames = ["archivo", "fecha", "notario", "protocolo", "texto", "error"]
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for fila in filas:
            writer.writerow(fila)


def main() -> None:
    print(f"Procesando PDFs en: {RUTA_PDFS}")
    filas = procesar_pdf_en_carpeta(RUTA_PDFS)
    escribir_csv(SALIDA_CSV, filas)
    print(
        f"✅ Procesados {len(filas)} archivos. Resultados guardados en {SALIDA_CSV}"
    )


if __name__ == "__main__":  # pragma: no cover - punto de entrada de script
    main()


import os
import re
import csv
from pathlib import Path
from PyPDF2 import PdfReader

# === CONFIGURACIÓN BASE ===
BASE_DIR = Path(__file__).resolve().parent.parent
RUTA_PDFS = BASE_DIR / "evidencia" / "pdfs"
SALIDA_CSV = BASE_DIR / "resultados" / "cronologia.csv"

def extraer_datos(pdf_path: Path) -> dict[str, str]:
    try:
        reader = PdfReader(str(pdf_path))
        texto = "".join(page.extract_text() or "" for page in reader.pages)

        patron_fecha = r"(\\d{1,2}\\s+de\\s+[A-Za-záéíóúÁÉÍÓÚ]+\\s+de\\s+\\d{4})"
        patron_notario = r"Notario(?:a)?\\s+P[úu]blico[^\\n]*"
        patron_protocolo = r"[Ff]olio[s]?\\s+n[oº°]?\\s*\\d+"

        fecha = re.search(patron_fecha, texto)
        notario = re.search(patron_notario, texto)
        protocolo = re.search(patron_protocolo, texto)

        return {
            "archivo": pdf_path.name,
            "fecha": fecha.group(0) if fecha else "",
            "notario": notario.group(0) if notario else "",
            "protocolo": protocolo.group(0) if protocolo else "",
        }
    except Exception as e:
        return {"archivo": pdf_path.name, "error": str(e)}

def procesar_pdf_en_carpeta(ruta: Path) -> list[dict[str, str]]:
    datos = []
    if not ruta.exists():
        print(f"⚠️ Carpeta no encontrada: {ruta}")
        return datos
    for archivo in ruta.iterdir():
        if archivo.suffix.lower() == ".pdf":
            datos.append(extraer_datos(archivo))
    return datos

def escribir_csv(destino: Path, filas: list[dict[str, str]]) -> None:
    destino.parent.mkdir(parents=True, exist_ok=True)
    with destino.open("w", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=["archivo","fecha","notario","protocolo"])
        writer.writeheader()
        writer.writerows(filas)

if __name__ == "__main__":
    print(f"Procesando PDFs en: {RUTA_PDFS}")
    filas = procesar_pdf_en_carpeta(RUTA_PDFS)
    escribir_csv(SALIDA_CSV, filas)
    print(f"✅ Procesados {len(filas)} archivos. Resultados guardados en {SALIDA_CSV}")

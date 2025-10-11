from pathlib import Path
from typing import Optional
import io

import fitz  # PyMuPDF
import pytesseract
from PIL import Image

# Ajusta si tu tesseract está en otra ruta:
TESSERACT_EXE_CANDIDATES = [
    r"C:\Program Files\Tesseract-OCR\tesseract.exe",
    r"C:\Program Files (x86)\Tesseract-OCR\tesseract.exe",
]

for _p in TESSERACT_EXE_CANDIDATES:
    p = Path(_p)
    if p.exists():
        pytesseract.pytesseract.tesseract_cmd = str(p)
        break

def ocr_pdf_to_text(pdf_path: Path, dpi: int = 240, lang: str = "spa+eng") -> str:
    """
    Renderiza cada página a imagen y pasa OCR. Retorna el texto concatenado.
    - lang: usa modelos español+inglés (puedes poner 'spa' si quieres forzar español).
    """
    pdf_path = Path(pdf_path)
    if not pdf_path.exists():
        return ""

    text_parts = []
    with fitz.open(pdf_path) as doc:
        # scale ≈ dpi/72
        zoom = dpi / 72.0
        mat = fitz.Matrix(zoom, zoom)
        for page in doc:
            pix = page.get_pixmap(matrix=mat, alpha=False)
            img = Image.open(io.BytesIO(pix.tobytes("png")))
            # Config de OCR (suave). Puedes añadir --oem / --psm si hace falta.
            txt = pytesseract.image_to_string(img, lang=lang)
            if txt:
                text_parts.append(txt)

    return "\n".join(text_parts).strip()

from reportlab.lib.pagesizes import LETTER
from reportlab.pdfgen import canvas
from pathlib import Path

BASE = Path(__file__).resolve().parents[1]
DEST = BASE / "evidencia" / "pdfs"
DEST.mkdir(parents=True, exist_ok=True)

docs = [
    ("escritura_001.pdf",
     "En la ciudad de Matagalpa, a los 12 de junio de 2023, comparece la parte interesada.\n"
     "Acto seguido, el Notario Público Juan Pérez certifica lo actuado.\n"
     "Se asienta en Folios No 123 del protocolo correspondiente."),
    ("escritura_002.pdf",
     "En Sébaco, a los 3 de agosto de 2024, la compareciente otorga poder especial.\n"
     "La Notaria Pública María López hace constar lo siguiente.\n"
     "Queda registrado en folios n° 987.\n"
     "Se menciona estelionato y uso de documento falso.")
]

for fname, text in docs:
    c = canvas.Canvas(str(DEST / fname), pagesize=LETTER)
    width, height = LETTER
    y = height - 72
    for line in text.splitlines():
        c.drawString(72, y, line)
        y -= 18
    c.showPage()
    c.save()

print(f"✅ PDFs de prueba generados en: {DEST}")

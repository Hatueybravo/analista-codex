from pathlib import Path
import json
import re
import pandas as pd

BASE_DIR = Path(__file__).resolve().parents[1]
RUTA_CRONO = BASE_DIR / "resultados" / "cronologia_normalizada.csv"
RUTA_CPP = BASE_DIR / "config" / "cpp_catalogo.json"
RUTA_LOG = BASE_DIR / "resultados" / "cpp_sugerencias.log"

# Palabras que suelen acompañar tipos penales cuando no están en catálogo
SEEDS = [
    r"hurto(?!\s*agravado)", r"falsificaci[oó]n", r"uso de documento",
    r"estelionato", r"estafa", r"apropiaci[oó]n indebida", r"amenazas",
    r"coacci[oó]n", r"usura", r"violencia patrimonial", r"fraude"
]

def load_catalog():
    with open(RUTA_CPP, "r", encoding="utf-8-sig") as f:
        data = json.load(f).get("catalogo_cpp", [])
    return {c["delito"].lower(): c for c in data}

def scan_text(row):
    text = " ".join(str(row.get(k, "")) for k in ["archivo","notario","protocolo","texto","fecha"]).lower()
    hits = set()
    for patt in SEEDS:
        if re.search(patt, text):
            hits.add(patt)
    # Detecta posibles referencias a artículos: art 2xx/3xx
    arts = re.findall(r"\bart(?:[íi]culo)?\.?\s*(\d{2,3})\b", text)
    return hits, set(arts)

def main():
    if not RUTA_CRONO.exists() or RUTA_CRONO.stat().st_size == 0:
        print("⚠️ No hay cronología para analizar; no se generan sugerencias.")
        return
    df = pd.read_csv(RUTA_CRONO, dtype=str, encoding="utf-8-sig").fillna("")
    catalog = load_catalog()
    suggestions = []
    for _, row in df.iterrows():
        hits, arts = scan_text(row)
        if hits or arts:
            suggestions.append({
                "archivo": row.get("archivo",""),
                "coincidencias": sorted(list(hits)),
                "articulos_mencionados": sorted(list(arts))
            })
    RUTA_LOG.parent.mkdir(parents=True, exist_ok=True)
    with open(RUTA_LOG, "a", encoding="utf-8") as f:
        f.write(json.dumps({"sugerencias": suggestions}, ensure_ascii=False) + "\n")
    print(f"🧠 Sugerencias generadas: {len(suggestions)} → {RUTA_LOG}")

if __name__ == "__main__":
    main()

from pathlib import Path
import pandas as pd
import json

BASE_DIR = Path(__file__).resolve().parents[1]
RUTA_CRONO = BASE_DIR / "resultados" / "cronologia_normalizada.csv"
RUTA_XLSX = BASE_DIR / "resultados" / "matriz_evidencia.xlsx"
RUTA_CPP = BASE_DIR / "config" / "cpp_catalogo.json"

def cargar_cpp_catalogo(path):
    if not path.exists():
        print(f"⚠️ Catálogo CPP no encontrado: {path}")
        return []
    try:
        with open(path, "r", encoding="utf-8-sig") as f:
            data = json.load(f)
            return data.get("catalogo_cpp", [])
    except Exception as e:
        print(f"⚠️ Error leyendo catálogo CPP: {e}")
        return []

def cargar_cronologia(path):
    if not path.exists() or path.stat().st_size == 0:
        print("⚠️ No hay cronología disponible. Se generará plantilla vacía.")
        return pd.DataFrame(columns=["archivo", "fecha", "notario", "protocolo", "texto"])
    return pd.read_csv(path, dtype=str, encoding="utf-8-sig").fillna("")

def detectar_delito(row, catalogo):
    # Fusiona campos para buscar coincidencias
    texto = " ".join(str(row.get(k, "")) for k in ["archivo","notario","protocolo","texto","fecha"]).lower()
    for item in catalogo:
        # Coincidencia simple por inclusión; luego podemos pasar a regex/lemmas
        if item["delito"] in texto:
            return item["delito"], item["articulo"], item.get("descripcion","")
    return "", "", ""

if __name__ == "__main__":
    cpp_catalogo = cargar_cpp_catalogo(RUTA_CPP)
    df = cargar_cronologia(RUTA_CRONO)

    columnas = [
        "Archivo", "Fecha", "Notario", "Protocolo/Folio",
        "Delito_Imputado", "Artículo_CPP", "Descripción_CPP",
        "Observaciones"
    ]
    salida = pd.DataFrame(columns=columnas)

    for _, row in df.iterrows():
        delito, articulo, descripcion = detectar_delito(row, cpp_catalogo)
        # descripción ya viene de detectar_delito()
        salida.loc[len(salida)] = [
            row.get("archivo", ""), row.get("fecha", ""), row.get("notario", ""),
            row.get("protocolo", ""), delito, articulo, descripcion, ""
        ]

    RUTA_XLSX.parent.mkdir(parents=True, exist_ok=True)
    with pd.ExcelWriter(RUTA_XLSX, engine="openpyxl") as writer:
        salida.to_excel(writer, sheet_name="Matriz_Evidencia", index=False)

    print(f"✅ Matriz de evidencia creada con catálogo CPP: {RUTA_XLSX}")
    print(f"📚 Catálogo cargado: {len(cpp_catalogo)} delitos reconocidos")


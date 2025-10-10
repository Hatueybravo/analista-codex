from pathlib import Path
import pandas as pd
from pandas.errors import EmptyDataError

# === BASE DE RUTAS SEGURAS ===
BASE_DIR = Path(__file__).resolve().parent.parent
RUTA_ENTRADA = BASE_DIR / "resultados" / "cronologia.csv"
RUTA_SALIDA = BASE_DIR / "resultados" / "cronologia_normalizada.csv"

def leer_csv_seguro(path: Path) -> pd.DataFrame:
    if not path.exists() or path.stat().st_size < 10:
        print(f"⚠️ Archivo vacío o no encontrado: {path}")
        return pd.DataFrame(columns=["archivo", "fecha", "notario", "protocolo"])
    try:
        df = pd.read_csv(path, dtype=str, encoding="utf-8-sig").fillna("")
        return df
    except EmptyDataError:
        print(f"⚠️ Sin datos válidos en: {path}")
        return pd.DataFrame(columns=["archivo", "fecha", "notario", "protocolo"])

def normalizar_fecha(fecha_texto: str) -> str:
    meses = {
        "enero": 1, "febrero": 2, "marzo": 3, "abril": 4, "mayo": 5,
        "junio": 6, "julio": 7, "agosto": 8, "septiembre": 9,
        "octubre": 10, "noviembre": 11, "diciembre": 12
    }
    try:
        partes = fecha_texto.lower().replace(" de ", " ").split()
        if len(partes) == 3:
            dia, mes, anio = partes
            mes_num = meses.get(mes, 0)
            if mes_num:
                return f"{int(anio):04d}-{mes_num:02d}-{int(dia):02d}"
    except Exception:
        pass
    return ""

if __name__ == "__main__":
    print(f"Procesando cronología desde: {RUTA_ENTRADA}")
    df = leer_csv_seguro(RUTA_ENTRADA)
    if df.empty:
        print("⚠️ No hay filas para normalizar.")
    else:
        df["fecha_iso"] = df["fecha"].apply(normalizar_fecha)
        RUTA_SALIDA.parent.mkdir(parents=True, exist_ok=True)
        df.to_csv(RUTA_SALIDA, index=False, encoding="utf-8-sig")
        print(f"✅ Cronología normalizada guardada en: {RUTA_SALIDA}")

import subprocess
import sys
from pathlib import Path

# === CONFIGURACIÓN BASE ===
BASE_DIR = Path(__file__).resolve().parent
SCRIPTS_DIR = BASE_DIR / "scripts"

# === UTILIDAD DE COLORES ===
def color(text, tone="info"):
    if tone == "ok":
        return f"\033[92m{text}\033[0m"   # Verde
    elif tone == "warn":
        return f"\033[93m{text}\033[0m"  # Amarillo
    elif tone == "error":
        return f"\033[91m{text}\033[0m"  # Rojo
    else:
        return text

def run_script(name: str):
    script_path = SCRIPTS_DIR / name
    print(color(f"▶ Ejecutando {name} ...", "info"))
    try:
        subprocess.run([sys.executable, str(script_path)], check=True)
        print(color(f"✅ {name} completado correctamente", "ok"))
    except subprocess.CalledProcessError as e:
        print(color(f"❌ Error ejecutando {name}: {e}", "error"))
    except Exception as e:
        print(color(f"⚠️ Excepción en {name}: {e}", "warn"))
    print("-" * 60)

if __name__ == "__main__":
    print(color("🚀 INICIANDO PIPELINE CODEX — CASO MARÍA FÁTIMA", "info"))
    run_script("procesar_pdfs.py")
    run_script("cronologia_automatica.py")
    run_script("resumen_legal.py")
    print(color("🏁 PIPELINE COMPLETADO", "ok"))

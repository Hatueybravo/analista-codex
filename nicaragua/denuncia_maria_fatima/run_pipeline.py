from __future__ import annotations

import subprocess
import sys
import os

STEPS = [
    ("procesar_pdfs",                 "▶ Ejecutando procesar_pdfs.py ..."),
    ("scripts.cronologia_automatica", "▶ Ejecutando cronologia_automatica.py ..."),
    ("scripts.resumen_legal",         "▶ Ejecutando resumen_legal.py ..."),
]

def run_step(script_name: str, label: str) -> None:
    print(label)
    try:
        script_path = os.path.join(os.path.dirname(__file__), script_name.replace('scripts.', 'scripts\\') + '.py') if 'scripts' in script_name else os.path.join(os.path.dirname(__file__), script_name + '.py')
        # Capturar salida con manejo flexible de codificación
        proc = subprocess.run([sys.executable, script_path], check=True, capture_output=True, text=False, env={**os.environ, 'PYTHONIOENCODING': 'utf-8'})
        if proc.stdout:
            try:
                print(proc.stdout.decode('utf-8', errors='replace').strip())
            except UnicodeDecodeError:
                print("Salida binaria detectada, omitiendo decodificación.")
        if proc.stderr:
            try:
                print(proc.stderr.decode('utf-8', errors='replace').strip())
            except UnicodeDecodeError:
                print("Error binario detectado, omitiendo decodificación.")
        print(f"✅ {script_name.split('.')[-1]}.py completado correctamente")
    except subprocess.CalledProcessError as e:
        if e.stdout:
            try:
                print(e.stdout.decode('utf-8', errors='replace') or "")
            except UnicodeDecodeError:
                print("Salida binaria detectada, omitiendo decodificación.")
        if e.stderr:
            try:
                print(e.stderr.decode('utf-8', errors='replace') or "")
            except UnicodeDecodeError:
                print("Error binario detectado, omitiendo decodificación.")
        print(f"❌ Error ejecutando {script_name.split('.')[-1]}.py: {e}")
    print("-" * 60)

def main() -> None:
    print("🚀 INICIANDO PIPELINE CODEX — CASO MARÍA FÁTIMA")
    for script_name, label in STEPS:
        run_step(script_name, label)
    print("🏁 PIPELINE COMPLETADO")

if __name__ == "__main__":
    main()

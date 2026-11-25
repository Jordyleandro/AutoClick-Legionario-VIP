# AutoClicker para Windows

Aplicación de escritorio nueva e independiente. No contiene el proyecto Android anterior.

## Funciones
- Panel flotante siempre visible.
- Captura de posición del cursor (F6).
- Inicio y detención (F8 / F9).
- Velocidad configurable de 1 a 50 clics por segundo.
- Clic izquierdo o derecho.
- Contador de clics.

## Compilar localmente
```powershell
py -m pip install -r requirements.txt
py -m PyInstaller --onefile --noconsole --name AutoClicker-Legionario main.py
```
El ejecutable se genera en `dist/AutoClicker-Legionario.exe`.

## Uso
1. Ejecuta la aplicación.
2. Coloca el cursor sobre el punto deseado y pulsa F6. La captura ocurre tras 3,5 segundos.
3. Elige el botón y la velocidad.
4. Pulsa INICIAR o F8. Pulsa DETENER o F9 para parar.

La herramienta realiza clics normales del sistema. No incluye ocultación, evasión de detección ni mecanismos para eludir bloqueos.

import threading
import time
import tkinter as tk
from tkinter import ttk, messagebox

import pyautogui
from pynput import keyboard

MAX_CPS = 50
pyautogui.PAUSE = 0
pyautogui.FAILSAFE = True


class AutoClickerApp:
    def __init__(self, root):
        self.root = root
        self.root.title("AUTOCLICKER")
        self.root.geometry("360x440")
        self.root.resizable(False, False)
        self.root.attributes("-topmost", True)
        self.root.configure(bg="#11151c")
        self.target = None
        self.running = threading.Event()
        self.click_count = 0
        self.listener = None
        self.closing = False
        self.cps_var = tk.IntVar(value=10)
        self.button_var = tk.StringVar(value="left")
        self.status_var = tk.StringVar(value="DETENIDO")
        self.point_var = tk.StringVar(value="Sin coordenadas")
        self.count_var = tk.StringVar(value="0")
        self._build_ui()
        self._start_hotkeys()
        self.root.protocol("WM_DELETE_WINDOW", self.close)

    def _build_ui(self):
        style = ttk.Style()
        style.theme_use("clam")
        style.configure("TCombobox", fieldbackground="#202833",
                        background="#202833", foreground="white")
        header = tk.Frame(self.root, bg="#11151c")
        header.pack(fill="x", padx=20, pady=(18, 8))
        tk.Label(header, text="AUTOCLICKER", bg="#11151c", fg="#f4b942",
                 font=("Segoe UI", 19, "bold")).pack(anchor="w")
        tk.Label(header, text="Windows desktop utility", bg="#11151c",
                 fg="#9aa7b7", font=("Segoe UI", 9)).pack(anchor="w")
        card = tk.Frame(self.root, bg="#1b222c", highlightbackground="#303b49",
                        highlightthickness=1)
        card.pack(fill="x", padx=18, pady=10)
        tk.Label(card, text="PUNTO DE CLIC", bg="#1b222c", fg="#dce3ec",
                 font=("Segoe UI", 10, "bold")).pack(anchor="w", padx=14, pady=(12, 3))
        tk.Label(card, textvariable=self.point_var, bg="#1b222c", fg="#f4b942",
                 font=("Segoe UI", 10)).pack(anchor="w", padx=14)
        self._button(card, "CAPTURAR PUNTO  (F6)", self.capture_point,
                     "#354457").pack(fill="x", padx=14, pady=10)
        settings = tk.Frame(self.root, bg="#1b222c", highlightbackground="#303b49",
                            highlightthickness=1)
        settings.pack(fill="x", padx=18, pady=8)
        tk.Label(settings, text="VELOCIDAD", bg="#1b222c", fg="#dce3ec",
                 font=("Segoe UI", 10, "bold")).pack(anchor="w", padx=14, pady=(12, 0))
        self.speed_label = tk.Label(settings, text="10 CPS", bg="#1b222c",
                                    fg="#f4b942", font=("Segoe UI", 11, "bold"))
        self.speed_label.pack(anchor="e", padx=14)
        tk.Scale(settings, from_=1, to=MAX_CPS, orient="horizontal",
                 variable=self.cps_var, command=self.update_speed,
                 bg="#1b222c", fg="white", troughcolor="#354457",
                 highlightthickness=0, activebackground="#f4b942",
                 length=290).pack(padx=12, pady=(0, 8))
        row = tk.Frame(settings, bg="#1b222c")
        row.pack(fill="x", padx=14, pady=(0, 12))
        tk.Label(row, text="Botón:", bg="#1b222c", fg="#dce3ec").pack(side="left")
        ttk.Combobox(row, textvariable=self.button_var, state="readonly",
                     values=("left", "right"), width=10).pack(side="right")
        controls = tk.Frame(self.root, bg="#11151c")
        controls.pack(fill="x", padx=18, pady=(10, 4))
        self._button(controls, "INICIAR  (F8)", self.start, "#16734b").pack(
            side="left", expand=True, fill="x", padx=(0, 5))
        self._button(controls, "DETENER  (F9)", self.stop, "#a8323e").pack(
            side="left", expand=True, fill="x", padx=(5, 0))
        footer = tk.Frame(self.root, bg="#11151c")
        footer.pack(fill="x", padx=20, pady=12)
        tk.Label(footer, text="Estado:", bg="#11151c", fg="#9aa7b7").pack(side="left")
        tk.Label(footer, textvariable=self.status_var, bg="#11151c",
                 fg="#f4b942", font=("Segoe UI", 10, "bold")).pack(side="left", padx=6)
        tk.Label(footer, text="Clics:", bg="#11151c", fg="#9aa7b7").pack(
            side="left", padx=(18, 4))
        tk.Label(footer, textvariable=self.count_var, bg="#11151c",
                 fg="white", font=("Segoe UI", 10, "bold")).pack(side="left")
        tk.Label(self.root, text="F6 capturar  •  F8 iniciar/detener  •  F9 detener",
                 bg="#11151c", fg="#778596", font=("Segoe UI", 8)).pack(pady=(0, 8))

    def _button(self, parent, text, command, color):
        return tk.Button(parent, text=text, command=command, bg=color, fg="white",
                         activebackground=color, activeforeground="white",
                         relief="flat", bd=0, padx=10, pady=11,
                         font=("Segoe UI", 9, "bold"), cursor="hand2")

    def update_speed(self, _value=None):
        self.speed_label.config(text=f"{self.cps_var.get()} CPS")

    def capture_point(self):
        if self.running.is_set():
            self.stop()
        self.root.withdraw()
        self.root.after(3500, self._finish_capture)

    def _finish_capture(self):
        if self.closing:
            return
        try:
            x, y = pyautogui.position()
            self.target = (int(x), int(y))
            self.point_var.set(f"X: {x}    Y: {y}")
        except Exception as exc:
            messagebox.showerror("Captura", str(exc))
        self.root.deiconify()
        self.root.attributes("-topmost", True)

    def _start_hotkeys(self):
        def on_press(key):
            try:
                if key == keyboard.Key.f6:
                    self.root.after(0, self.capture_point)
                elif key == keyboard.Key.f8:
                    self.root.after(0, self.toggle)
                elif key == keyboard.Key.f9:
                    self.root.after(0, self.stop)
            except Exception:
                pass
        self.listener = keyboard.Listener(on_press=on_press)
        self.listener.daemon = True
        self.listener.start()

    def toggle(self):
        self.stop() if self.running.is_set() else self.start()

    def start(self):
        if self.running.is_set():
            return
        if self.target is None:
            messagebox.showinfo("Falta el punto", "Primero captura las coordenadas con F6.")
            return
        self.running.set()
        self.status_var.set("EJECUTANDO")
        threading.Thread(target=self._click_loop, daemon=True).start()

    def _click_loop(self):
        while self.running.is_set():
            started = time.perf_counter()
            try:
                pyautogui.click(x=self.target[0], y=self.target[1],
                                button=self.button_var.get())
                self.click_count += 1
                count = self.click_count
                self.root.after(0, lambda n=count: self.count_var.set(str(n)))
            except Exception as exc:
                self.root.after(0, lambda e=str(exc): self._show_click_error(e))
                self.running.clear()
                break
            interval = 1.0 / max(1, min(MAX_CPS, int(self.cps_var.get())))
            time.sleep(max(0, interval - (time.perf_counter() - started)))
        if not self.closing:
            self.root.after(0, self._set_stopped)

    def _show_click_error(self, error):
        self._set_stopped()
        if not self.closing:
            messagebox.showerror("Error de clic", error)

    def _set_stopped(self):
        self.status_var.set("DETENIDO")

    def stop(self):
        self.running.clear()
        self.status_var.set("DETENIDO")

    def close(self):
        self.closing = True
        self.running.clear()
        if self.listener:
            self.listener.stop()
        self.root.destroy()


if __name__ == "__main__":
    window = tk.Tk()
    app = AutoClickerApp(window)
    window.mainloop()

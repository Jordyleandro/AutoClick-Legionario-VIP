import threading
import time
import tkinter as tk
from pynput import keyboard, mouse

MAX_CPS = 30
PAUSE_OVER_PANEL = True

COLORS = {
    "bg": "#11151c", "panel": "#1b222c", "edge": "#354457",
    "gold": "#f4b942", "text": "#eef2f7", "muted": "#9aa7b7",
    "green": "#16734b", "red": "#a8323e", "blue": "#354b66"
}


class FloatingAutoClicker:
    def __init__(self, root):
        self.root = root
        self.root.overrideredirect(True)
        self.root.attributes("-topmost", True)
        self.root.geometry("290x310+80+100")
        self.root.configure(bg=COLORS["bg"], highlightthickness=1,
                            highlightbackground=COLORS["gold"])

        self.mouse = mouse.Controller()
        self.running = threading.Event()
        self.listener = None
        self.closing = False
        self.click_count = 0
        self.compact = False
        self.drag_offset = (0, 0)

        self.cps = tk.IntVar(value=10)
        self.button = tk.StringVar(value="Izquierdo")
        self.status = tk.StringVar(value="DETENIDO")
        self.counter = tk.StringVar(value="0")
        self._build()
        self._hotkeys()
        self.root.protocol("WM_DELETE_WINDOW", self.close)

    def _build(self):
        self.header = tk.Frame(self.root, bg=COLORS["bg"], height=38)
        self.header.pack(fill="x")
        self.header.pack_propagate(False)
        title = tk.Label(self.header, text="  LEGIONARIO  •  AUTOCLICK",
                         bg=COLORS["bg"], fg=COLORS["gold"],
                         font=("Segoe UI", 10, "bold"))
        title.pack(side="left", padx=5)
        self._small_button(self.header, "—", self.minimize).pack(side="right", padx=3, pady=4)
        self._small_button(self.header, "×", self.close).pack(side="right", padx=3, pady=4)
        for widget in (self.header, title):
            widget.bind("<ButtonPress-1>", self._drag_start)
            widget.bind("<B1-Motion>", self._drag)

        self.body = tk.Frame(self.root, bg=COLORS["bg"])
        self.body.pack(fill="both", expand=True, padx=12, pady=(4, 10))

        statusrow = tk.Frame(self.body, bg=COLORS["panel"])
        statusrow.pack(fill="x", pady=(0, 9))
        tk.Label(statusrow, text="ESTADO", bg=COLORS["panel"], fg=COLORS["muted"],
                 font=("Segoe UI", 9, "bold")).pack(side="left", padx=10, pady=9)
        tk.Label(statusrow, textvariable=self.status, bg=COLORS["panel"],
                 fg=COLORS["gold"], font=("Segoe UI", 9, "bold")).pack(side="right", padx=10)

        tk.Label(self.body, text="Clic en la posición actual del cursor",
                 bg=COLORS["bg"], fg=COLORS["text"],
                 font=("Segoe UI", 9)).pack(anchor="w", pady=(1, 8))

        speedrow = tk.Frame(self.body, bg=COLORS["bg"])
        speedrow.pack(fill="x")
        tk.Label(speedrow, text="VELOCIDAD", bg=COLORS["bg"], fg=COLORS["muted"],
                 font=("Segoe UI", 9, "bold")).pack(side="left")
        self.speed_text = tk.Label(speedrow, text="10 CPS", bg=COLORS["bg"],
                                   fg=COLORS["gold"], font=("Segoe UI", 9, "bold"))
        self.speed_text.pack(side="right")
        tk.Scale(self.body, from_=1, to=MAX_CPS, orient="horizontal",
                 variable=self.cps, command=self._speed_changed,
                 bg=COLORS["bg"], fg=COLORS["text"], troughcolor=COLORS["edge"],
                 highlightthickness=0, activebackground=COLORS["gold"],
                 showvalue=False, bd=0).pack(fill="x", pady=(0, 7))

        row = tk.Frame(self.body, bg=COLORS["bg"])
        row.pack(fill="x", pady=(0, 10))
        tk.Label(row, text="BOTÓN", bg=COLORS["bg"], fg=COLORS["muted"],
                 font=("Segoe UI", 9, "bold")).pack(side="left")
        menu = tk.OptionMenu(row, self.button, "Izquierdo", "Derecho")
        menu.config(bg=COLORS["panel"], fg=COLORS["text"], activebackground=COLORS["edge"],
                    activeforeground="white", relief="flat", highlightthickness=0)
        menu["menu"].config(bg=COLORS["panel"], fg=COLORS["text"])
        menu.pack(side="right")

        buttons = tk.Frame(self.body, bg=COLORS["bg"])
        buttons.pack(fill="x", pady=(2, 9))
        self._button(buttons, "INICIAR  F8", self.start, COLORS["green"]).pack(
            side="left", expand=True, fill="x", padx=(0, 4))
        self._button(buttons, "DETENER  F9", self.stop, COLORS["red"]).pack(
            side="left", expand=True, fill="x", padx=(4, 0))

        bottom = tk.Frame(self.body, bg=COLORS["panel"])
        bottom.pack(fill="x")
        tk.Label(bottom, text="CLICS", bg=COLORS["panel"], fg=COLORS["muted"],
                 font=("Segoe UI", 9, "bold")).pack(side="left", padx=10, pady=8)
        tk.Label(bottom, textvariable=self.counter, bg=COLORS["panel"],
                 fg=COLORS["text"], font=("Segoe UI", 10, "bold")).pack(side="right", padx=10)

        tk.Label(self.body, text="Mueve el cursor libremente por el juego.",
                 bg=COLORS["bg"], fg=COLORS["muted"],
                 font=("Segoe UI", 8)).pack(anchor="w", pady=(8, 0))

    def _button(self, parent, text, command, color):
        return tk.Button(parent, text=text, command=command, bg=color, fg="white",
                         activebackground=color, activeforeground="white",
                         relief="flat", bd=0, padx=8, pady=10,
                         font=("Segoe UI", 9, "bold"), cursor="hand2")

    def _small_button(self, parent, text, command):
        return tk.Button(parent, text=text, command=command, bg=COLORS["panel"],
                         fg=COLORS["text"], activebackground=COLORS["edge"],
                         relief="flat", bd=0, width=3, font=("Segoe UI", 9, "bold"))

    def _drag_start(self, event):
        self.drag_offset = (event.x_root - self.root.winfo_x(),
                            event.y_root - self.root.winfo_y())

    def _drag(self, event):
        x = event.x_root - self.drag_offset[0]
        y = event.y_root - self.drag_offset[1]
        self.root.geometry(f"+{x}+{y}")

    def _speed_changed(self, _value=None):
        self.speed_text.config(text=f"{self.cps.get()} CPS")

    def minimize(self):
        if self.compact:
            return
        self.compact = True
        self.body.pack_forget()
        self.root.geometry("210x38")
        self.header.winfo_children()[0].config(text="  LEGIONARIO  •  MOSTRAR")
        self.header.winfo_children()[0].bind("<Button-1>", lambda _e: self.restore())

    def restore(self):
        if not self.compact:
            return
        self.compact = False
        self.root.geometry("290x310")
        self.header.winfo_children()[0].config(text="  LEGIONARIO  •  AUTOCLICK")
        self.header.winfo_children()[0].bind("<Button-1>", self._drag_start)
        self.header.winfo_children()[0].bind("<B1-Motion>", self._drag)
        self.body.pack(fill="both", expand=True, padx=12, pady=(4, 10))

    def _hotkeys(self):
        def on_press(key):
            if key == keyboard.Key.f8:
                self.root.after(0, self.toggle)
            elif key == keyboard.Key.f9:
                self.root.after(0, self.stop)
        self.listener = keyboard.Listener(on_press=on_press)
        self.listener.daemon = True
        self.listener.start()

    def toggle(self):
        if self.running.is_set():
            self.stop()
        else:
            self.start()

    def start(self):
        if self.running.is_set():
            return
        self.running.set()
        self.status.set("ACTIVO")
        threading.Thread(target=self._loop, daemon=True).start()

    def _loop(self):
        while self.running.is_set():
            began = time.perf_counter()
            try:
                x, y = self.mouse.position
                if not (PAUSE_OVER_PANEL and
                        self.root.winfo_rootx() <= x <= self.root.winfo_rootx() + self.root.winfo_width() and
                        self.root.winfo_rooty() <= y <= self.root.winfo_rooty() + self.root.winfo_height()):
                    btn = mouse.Button.left if self.button.get() == "Izquierdo" else mouse.Button.right
                    self.mouse.click(btn, 1)
                    self.click_count += 1
                    count = self.click_count
                    self.root.after(0, lambda n=count: self.counter.set(str(n)))
            except Exception:
                self.running.clear()
                break
            delay = 1.0 / max(1, min(MAX_CPS, self.cps.get()))
            self.running.wait(max(0, delay - (time.perf_counter() - began)))
        if not self.closing:
            self.root.after(0, lambda: self.status.set("DETENIDO"))

    def stop(self):
        self.running.clear()
        self.status.set("DETENIDO")

    def close(self):
        self.closing = True
        self.running.clear()
        if self.listener:
            self.listener.stop()
        self.root.destroy()


if __name__ == "__main__":
    app_window = tk.Tk()
    app = FloatingAutoClicker(app_window)
    app_window.mainloop()

# -*- coding: utf-8 -*-
"""FamilyOS Key Issuer — SEGAL COMMUNICATIONS.

Generates signed Premium keys that the Android app verifies without a rebuild.
Must keep SECRET in sync with LicenseKey.kt.
"""
from __future__ import annotations

import csv
import hmac
import hashlib
import secrets
import sys
from datetime import datetime
from pathlib import Path
import tkinter as tk
from tkinter import messagebox, ttk

SECRET = "FamilyOS.SEGAL.COMMUNICATIONS.license.v1.2026"
BLUE = "#2563EB"
INK = "#0F172A"
MUTED = "#475569"
BG = "#F8FAFC"

DURATIONS = (
    ("1 month", 30),
    ("1 year", 365),
    ("Lifetime", 0),
)


def app_dir() -> Path:
    if getattr(sys, "frozen", False):
        return Path(sys.executable).resolve().parent
    return Path(__file__).resolve().parent


def signature(nonce: str, days: int) -> str:
    payload = f"{nonce.upper()}:{days}".encode("utf-8")
    digest = hmac.new(SECRET.encode("utf-8"), payload, hashlib.sha256).hexdigest()
    return digest[:8].upper()


def generate_key(days: int) -> str:
    nonce = secrets.token_hex(4).upper()
    return f"FOS-{nonce}-{days}-{signature(nonce, days)}"


def log_path() -> Path:
    return app_dir() / "issued_keys.csv"


def append_log(customer: str, days: int, key: str) -> None:
    path = log_path()
    new_file = not path.exists()
    with path.open("a", newline="", encoding="utf-8") as fh:
        writer = csv.writer(fh)
        if new_file:
            writer.writerow(["issued_at", "customer", "days", "key"])
        writer.writerow(
            [datetime.now().strftime("%Y-%m-%d %H:%M:%S"), customer, days, key]
        )


def load_log(limit: int = 40) -> list[list[str]]:
    path = log_path()
    if not path.exists():
        return []
    with path.open(newline="", encoding="utf-8") as fh:
        rows = list(csv.reader(fh))
    return rows[-limit:] if len(rows) > 1 else rows


class IssuerApp(tk.Tk):
    def __init__(self) -> None:
        super().__init__()
        self.title("FamilyOS Key Issuer — SEGAL COMMUNICATIONS")
        self.configure(bg=BG)
        self.geometry("720x560")
        self.minsize(640, 520)
        self.duration_days = tk.IntVar(value=365)
        self.customer = tk.StringVar()
        self.key_var = tk.StringVar(value="Press Issue key")
        self._build()
        self.refresh_log()

    def _build(self) -> None:
        pad = {"padx": 24, "pady": 8}
        header = tk.Frame(self, bg=BLUE)
        header.pack(fill="x")
        tk.Label(
            header,
            text="FamilyOS",
            fg="white",
            bg=BLUE,
            font=("Segoe UI", 22, "bold"),
        ).pack(anchor="w", padx=24, pady=(16, 0))
        tk.Label(
            header,
            text="SEGAL COMMUNICATIONS  ·  Premium key issuer",
            fg="#DBEAFE",
            bg=BLUE,
            font=("Segoe UI", 11),
        ).pack(anchor="w", padx=24, pady=(0, 16))

        body = tk.Frame(self, bg=BG)
        body.pack(fill="both", expand=True)

        tk.Label(body, text="Customer (name or email, optional)", bg=BG, fg=MUTED, font=("Segoe UI", 9)).pack(
            anchor="w", **pad
        )
        tk.Entry(body, textvariable=self.customer, font=("Segoe UI", 12)).pack(fill="x", padx=24)

        tk.Label(body, text="Duration", bg=BG, fg=MUTED, font=("Segoe UI", 9)).pack(anchor="w", **pad)
        dur = tk.Frame(body, bg=BG)
        dur.pack(anchor="w", padx=24)
        for label, days in DURATIONS:
            tk.Radiobutton(
                dur,
                text=label,
                value=days,
                variable=self.duration_days,
                bg=BG,
                font=("Segoe UI", 11),
                activebackground=BG,
            ).pack(side="left", padx=(0, 16))

        btns = tk.Frame(body, bg=BG)
        btns.pack(fill="x", padx=24, pady=16)
        tk.Button(
            btns,
            text="Issue key",
            command=self.issue,
            bg=BLUE,
            fg="white",
            font=("Segoe UI", 12, "bold"),
            relief="flat",
            padx=18,
            pady=8,
        ).pack(side="left")
        tk.Button(
            btns,
            text="Copy",
            command=self.copy_key,
            font=("Segoe UI", 11),
            relief="flat",
            padx=16,
            pady=8,
        ).pack(side="left", padx=8)

        tk.Label(body, text="Key for the customer", bg=BG, fg=MUTED, font=("Segoe UI", 9)).pack(
            anchor="w", padx=24
        )
        tk.Entry(
            body,
            textvariable=self.key_var,
            font=("Consolas", 16, "bold"),
            fg=INK,
            justify="center",
            relief="solid",
        ).pack(fill="x", padx=24, pady=(0, 8), ipady=10)

        tk.Label(
            body,
            text="Customer enters this code in FamilyOS → Premium → Activation code.\nNo app rebuild. Log is saved next to this program: issued_keys.csv",
            bg=BG,
            fg=MUTED,
            font=("Segoe UI", 9),
            justify="left",
        ).pack(anchor="w", padx=24, pady=(0, 8))

        columns = ("issued_at", "customer", "days", "key")
        self.tree = ttk.Treeview(body, columns=columns, show="headings", height=8)
        for col, title, width in (
            ("issued_at", "Issued", 140),
            ("customer", "Customer", 160),
            ("days", "Days", 60),
            ("key", "Key", 280),
        ):
            self.tree.heading(col, text=title)
            self.tree.column(col, width=width, stretch=True)
        self.tree.pack(fill="both", expand=True, padx=24, pady=(0, 20))
        self.tree.bind("<Double-1>", lambda _e: self.copy_selected())

    def issue(self) -> None:
        days = int(self.duration_days.get())
        key = generate_key(days)
        customer = self.customer.get().strip() or "—"
        self.key_var.set(key)
        append_log(customer, days, key)
        self.refresh_log()
        self.clipboard_clear()
        self.clipboard_append(key)
        self.update()
        label = "lifetime" if days == 0 else f"{days} days"
        messagebox.showinfo("Key issued", f"{label}\n\n{key}\n\nCopied to clipboard.")

    def copy_key(self) -> None:
        key = self.key_var.get().strip()
        if not key.startswith("FOS-"):
            messagebox.showwarning("No key", "Issue a key first.")
            return
        self.clipboard_clear()
        self.clipboard_append(key)
        self.update()

    def copy_selected(self) -> None:
        sel = self.tree.selection()
        if not sel:
            return
        key = self.tree.item(sel[0], "values")[-1]
        self.key_var.set(key)
        self.clipboard_clear()
        self.clipboard_append(key)
        self.update()

    def refresh_log(self) -> None:
        for row in self.tree.get_children():
            self.tree.delete(row)
        rows = load_log()
        if rows and rows[0] and rows[0][0] == "issued_at":
            rows = rows[1:]
        for row in reversed(rows):
            if len(row) >= 4:
                self.tree.insert("", "end", values=row)


if __name__ == "__main__":
    IssuerApp().mainloop()

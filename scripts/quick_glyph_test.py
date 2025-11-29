#!/usr/bin/env python3
"""
Quick Glyph Test Script
=======================
A fast, standalone test that launches the Glyph Glance app and monitors
Glyph activity through ADB logcat in real-time.

This script does NOT require building instrumented tests - it works with
the installed app directly.

Usage:
  python quick_glyph_test.py          # Run basic connectivity test
  python quick_glyph_test.py --live   # Live monitor mode (continuous)
"""

import os
import subprocess
import sys
import time
from datetime import datetime

# ============================================================================
# Configuration
# ============================================================================

APP_PACKAGE = "com.example.glyph_glance"
MAIN_ACTIVITY = f"{APP_PACKAGE}.MainActivity"
LOGCAT_TAGS = ["LiveLogger:*", "System.out:I", "GlyphManager:*", "*:S"]


class Colors:
    RED = "\033[91m"
    GREEN = "\033[92m"
    YELLOW = "\033[93m"
    BLUE = "\033[94m"
    CYAN = "\033[96m"
    MAGENTA = "\033[95m"
    BOLD = "\033[1m"
    DIM = "\033[2m"
    RESET = "\033[0m"


def c(text: str, color: str) -> str:
    if sys.platform == "win32":
        os.system("")  # Enable ANSI on Windows
    return f"{color}{text}{Colors.RESET}"


# ============================================================================
# ADB Utilities
# ============================================================================


def find_adb():
    """Find ADB executable."""
    paths = [
        os.path.expanduser(r"~\AppData\Local\Android\Sdk\platform-tools\adb.exe"),
        r"C:\Android\android-sdk\platform-tools\adb.exe",
        os.path.expanduser("~/Android/Sdk/platform-tools/adb"),
        "adb",
    ]
    for p in paths:
        try:
            if p == "adb":
                r = subprocess.run([p, "version"], capture_output=True, text=True)
                if r.returncode == 0:
                    return p
            elif os.path.exists(p):
                return p
        except FileNotFoundError:
            continue
    return None


def adb(adb_path: str, args: list, timeout: int = 30):
    """Run ADB command and return (success, output)."""
    try:
        r = subprocess.run(
            [adb_path] + args, capture_output=True, text=True, timeout=timeout
        )
        return r.returncode == 0, r.stdout + r.stderr
    except Exception as e:
        return False, str(e)


def get_device_info(adb_path: str) -> dict:
    """Get device info as dictionary."""
    info = {}
    props = {
        "manufacturer": "ro.product.manufacturer",
        "model": "ro.product.model",
        "android": "ro.build.version.release",
    }
    for key, prop in props.items():
        ok, out = adb(adb_path, ["shell", "getprop", prop])
        info[key] = out.strip() if ok else "Unknown"
    info["is_nothing"] = (
        "nothing" in info["manufacturer"].lower() or "nothing" in info["model"].lower()
    )
    return info


def send_test_notification(adb_path: str) -> bool:
    """
    Send a test notification via ADB to trigger Glyph.
    Uses 'cmd notification post' which is available on Android 10+.
    The notification will be intercepted by the NotificationListenerService
    and routed through BufferEngine -> IntelligenceEngine -> GlyphManager.
    """
    print(c("  📤 Sending test notification...", Colors.CYAN))

    # Post a high-priority notification that will trigger HIGH_STROBE pattern
    # Using bigtext style for better visibility
    ok, out = adb(
        adb_path,
        [
            "shell",
            "cmd",
            "notification",
            "post",
            "-S",
            "bigtext",
            "-t",
            "URGENT TEST",
            "glyph_test_tag",
            "This is an urgent test message to trigger Glyph HIGH_STROBE pattern",
        ],
    )

    if ok:
        print(c("  ✓ Test notification sent", Colors.GREEN))
    else:
        # Fallback: try using 'am broadcast' to trigger a simple notification
        print(c("  ⚠ cmd notification failed, trying alternative...", Colors.YELLOW))
        # Alternative: use service call or broadcast
        ok2, _ = adb(
            adb_path,
            [
                "shell",
                "am",
                "broadcast",
                "-a",
                "android.intent.action.BOOT_COMPLETED",
                "-p",
                "com.android.systemui",
            ],
        )
        if not ok2:
            print(
                c("  ⚠ Could not send test notification automatically", Colors.YELLOW)
            )
            return False

    return ok


# ============================================================================
# Test Functions
# ============================================================================


def print_header():
    print(c("╔" + "═" * 58 + "╗", Colors.BLUE))
    print(
        c("║", Colors.BLUE)
        + c(" QUICK GLYPH TEST ", Colors.BOLD + Colors.CYAN).center(68)
        + c("║", Colors.BLUE)
    )
    print(c("╚" + "═" * 58 + "╝", Colors.BLUE))


def check_prerequisites(adb_path: str) -> bool:
    """Check device connection and app installation."""
    print(c("\n▸ Checking prerequisites...", Colors.CYAN))

    # Check device
    ok, out = adb(adb_path, ["devices"])
    if not ok or "device" not in out.split("\n", 1)[-1]:
        print(c("  ✗ No device connected", Colors.RED))
        return False

    info = get_device_info(adb_path)
    print(
        f"  ✓ Device: {info['manufacturer']} {info['model']} (Android {info['android']})"
    )

    if info["is_nothing"]:
        print(c("  ✓ Nothing Phone detected - Hardware Glyph mode", Colors.GREEN))
    else:
        print(c("  ⚠ Not a Nothing Phone - Mock mode", Colors.YELLOW))

    # Check app
    ok, out = adb(adb_path, ["shell", "pm", "list", "packages", APP_PACKAGE])
    if APP_PACKAGE not in out:
        print(c(f"  ✗ App not installed: {APP_PACKAGE}", Colors.RED))
        print("    Run: ./gradlew installDebug")
        return False
    print(f"  ✓ App installed: {APP_PACKAGE}")

    # Enable glyph debug
    adb(
        adb_path,
        ["shell", "settings", "put", "global", "nt_glyph_interface_debug_enable", "1"],
    )
    print("  ✓ Glyph debug mode enabled")

    return True


def launch_app(adb_path: str):
    """Launch the main app activity."""
    print(c("\n▸ Launching Glyph Glance app...", Colors.CYAN))
    ok, _ = adb(
        adb_path, ["shell", "am", "start", "-n", f"{APP_PACKAGE}/.MainActivity"]
    )
    if ok:
        print("  ✓ App launched")
    else:
        print(c("  ⚠ Could not launch app", Colors.YELLOW))
    time.sleep(2)


def capture_logs(adb_path: str, duration: int = 8, auto_trigger: bool = True) -> list:
    """
    Capture logcat for specified duration.
    If auto_trigger is True, automatically sends a test notification to trigger Glyph.
    """
    print(c(f"\n▸ Capturing Glyph logs for {duration} seconds...", Colors.CYAN))

    # Clear and start fresh
    adb(adb_path, ["logcat", "-c"])

    logs = []
    start = time.time()
    notification_sent = False

    # Start logcat process
    process = subprocess.Popen(
        [adb_path, "logcat", "-v", "time"] + LOGCAT_TAGS,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
    )

    try:
        # Send test notification after a brief delay for service binding
        if auto_trigger:
            time.sleep(0.5)
            print(
                c(
                    "\n  ⚡ AUTO-TRIGGERING GLYPH via test notification ⚡\n",
                    Colors.YELLOW + Colors.BOLD,
                )
            )
            notification_sent = send_test_notification(adb_path)
            if notification_sent:
                print(c("  👀 Watching for Glyph activation...\n", Colors.CYAN))

        while time.time() - start < duration:
            if process.stdout:
                line = process.stdout.readline()
                if line:
                    line = line.strip()
                    # Filter for interesting logs
                    if any(
                        kw in line.lower()
                        for kw in [
                            "glyph",
                            "livelogger",
                            "pattern",
                            "connected",
                            "strobe",
                            "pulse",
                            "intercepted",
                            "triggered",
                            "flushing",
                        ]
                    ):
                        logs.append(line)
                        # Print in real-time with colors
                        if "connected" in line.lower():
                            print(c(f"  → {line[-80:]}", Colors.GREEN))
                        elif "error" in line.lower():
                            print(c(f"  → {line[-80:]}", Colors.RED))
                        elif (
                            "playing" in line.lower()
                            or "strobe" in line.lower()
                            or "pulse" in line.lower()
                            or "triggered" in line.lower()
                        ):
                            print(c(f"  → {line[-80:]}", Colors.MAGENTA))
                        elif (
                            "intercepted" in line.lower() or "flushing" in line.lower()
                        ):
                            print(c(f"  → {line[-80:]}", Colors.CYAN))
                        else:
                            print(f"  → {line[-80:]}")
            time.sleep(0.1)
    finally:
        process.terminate()

    return logs


def analyze_results(logs: list) -> dict:
    """Analyze captured logs for test results."""
    results = {
        "connected": any("connected" in log.lower() for log in logs),
        "registered": any("registered" in log.lower() for log in logs),
        "notification_intercepted": any("intercepted" in log.lower() for log in logs),
        "buffer_flushed": any("flushing" in log.lower() for log in logs),
        "patterns_triggered": sum(
            1
            for log in logs
            if "playing" in log.lower()
            or "strobe" in log.lower()
            or "pulse" in log.lower()
            or "triggered" in log.lower()
        ),
        "errors": [
            log for log in logs if "error" in log.lower() or "failed" in log.lower()
        ],
        "total_logs": len(logs),
    }
    return results


def print_results(results: dict):
    """Print test results summary."""
    print(c("\n" + "─" * 60, Colors.BLUE))
    print(c(" TEST RESULTS", Colors.BOLD + Colors.CYAN))
    print(c("─" * 60, Colors.BLUE))

    print(
        f"\n  Glyph Service Connected:  {c('✓ YES', Colors.GREEN) if results['connected'] else c('✗ NO', Colors.RED)}"
    )
    print(
        f"  SDK Registered:           {c('✓ YES', Colors.GREEN) if results['registered'] else c('✗ NO', Colors.RED)}"
    )
    print(
        f"  Notification Intercepted: {c('✓ YES', Colors.GREEN) if results['notification_intercepted'] else c('✗ NO', Colors.RED)}"
    )
    print(
        f"  Buffer Flushed:           {c('✓ YES', Colors.GREEN) if results['buffer_flushed'] else c('✗ NO', Colors.RED)}"
    )
    print(f"  Patterns Triggered:       {results['patterns_triggered']}")
    print(f"  Total Glyph Logs:         {results['total_logs']}")

    if results["errors"]:
        print(c(f"\n  Errors ({len(results['errors'])}):", Colors.RED))
        for err in results["errors"][:5]:
            print(c(f"    • {err[-70:]}", Colors.DIM))

    # Overall verdict
    print()
    if results["patterns_triggered"] > 0:
        print(c("  ══════════════════════════════════════", Colors.GREEN))
        print(c("  ✓ GLYPH TRIGGERED SUCCESSFULLY!", Colors.GREEN + Colors.BOLD))
        print(c("  ══════════════════════════════════════", Colors.GREEN))
    elif results["notification_intercepted"]:
        print(c("  ══════════════════════════════════════", Colors.YELLOW))
        print(c("  ⚠ Notification received but Glyph not triggered", Colors.YELLOW))
        print(c("    - Check if Glyph SDK is registered", Colors.DIM))
        print(c("    - Verify Glyph Interface is ON in Settings", Colors.DIM))
        print(c("  ══════════════════════════════════════", Colors.YELLOW))
    elif results["connected"]:
        print(c("  ══════════════════════════════════════", Colors.YELLOW))
        print(c("  ⚠ Connected but notification not intercepted", Colors.YELLOW))
        print(c("    - Grant notification listener permission", Colors.DIM))
        print(c("    - Try 'cmd notification' may need Android 10+", Colors.DIM))
        print(c("  ══════════════════════════════════════", Colors.YELLOW))
    else:
        print(c("  ══════════════════════════════════════", Colors.YELLOW))
        print(c("  ⚠ Could not verify Glyph connection", Colors.YELLOW))
        print(c("    - Ensure Glyph Interface is ON in Settings", Colors.DIM))
        print(c("    - Grant notification access permission", Colors.DIM))
        print(c("    - If not a Nothing Phone, this is expected", Colors.DIM))
        print(c("  ══════════════════════════════════════", Colors.YELLOW))


def live_monitor(adb_path: str):
    """Continuous live log monitoring mode."""
    print(c("\n▸ Live Monitor Mode - Press Ctrl+C to stop", Colors.CYAN))
    print(c("  Showing all Glyph-related activity...\n", Colors.DIM))

    adb(adb_path, ["logcat", "-c"])

    process = subprocess.Popen(
        [adb_path, "logcat", "-v", "time"] + LOGCAT_TAGS,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
    )

    try:
        while True:
            if process.stdout:
                line = process.stdout.readline()
                if line:
                    line = line.strip()
                    if any(
                        kw in line.lower()
                        for kw in [
                            "glyph",
                            "livelogger",
                            "pattern",
                            "connected",
                            "strobe",
                            "pulse",
                            "nothing",
                        ]
                    ):
                        ts = datetime.now().strftime("%H:%M:%S")
                        if "connected" in line.lower():
                            print(c(f"[{ts}] {line[-70:]}", Colors.GREEN))
                        elif "error" in line.lower():
                            print(c(f"[{ts}] {line[-70:]}", Colors.RED))
                        elif "playing" in line.lower():
                            print(c(f"[{ts}] {line[-70:]}", Colors.MAGENTA))
                        else:
                            print(f"[{ts}] {line[-70:]}")
    except KeyboardInterrupt:
        print(c("\n  Monitor stopped.", Colors.CYAN))
    finally:
        process.terminate()


# ============================================================================
# Main
# ============================================================================


def main():
    print_header()

    # Check for live mode
    live_mode = "--live" in sys.argv or "-l" in sys.argv

    # Find ADB
    adb_path = find_adb()
    if not adb_path:
        print(c("\n✗ ADB not found. Install Android SDK platform-tools.", Colors.RED))
        return 1

    # Prerequisites
    if not check_prerequisites(adb_path):
        return 1

    if live_mode:
        live_monitor(adb_path)
    else:
        # Standard test with auto-trigger
        launch_app(adb_path)
        logs = capture_logs(adb_path, duration=8, auto_trigger=True)
        results = analyze_results(logs)
        print_results(results)

    print(c("\n" + "═" * 60, Colors.BLUE))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print(c("\n\nCancelled.", Colors.YELLOW))
        sys.exit(130)

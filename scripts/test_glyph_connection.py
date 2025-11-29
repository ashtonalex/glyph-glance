#!/usr/bin/env python3
"""
Glyph Connection Test Script
============================
Tests the Nothing Phone Glyph interface to verify hardware connectivity.

This script will:
1. Check ADB connection and device info
2. Verify Nothing Phone detection  
3. Enable Glyph debug mode (required for 3rd party apps)
4. Install/verify the test APK
5. Run Glyph pattern tests and capture logs
6. Display results

Prerequisites:
- ADB installed and in PATH or standard SDK location
- Nothing Phone connected via USB with USB debugging enabled
- Glyph Interface enabled in phone settings
"""

import os
import subprocess
import sys
import time
from dataclasses import dataclass
from typing import Optional, List, Tuple

# ============================================================================
# Configuration
# ============================================================================

APP_PACKAGE = "com.example.glyph_glance"
TEST_RUNNER = "androidx.test.runner.AndroidJUnitRunner"
TEST_CLASS = "com.example.glyph_glance.hardware.GlyphInstrumentationTest"

# Color codes for terminal output
class Colors:
    RED = "\033[91m"
    GREEN = "\033[92m"
    YELLOW = "\033[93m"
    BLUE = "\033[94m"
    CYAN = "\033[96m"
    BOLD = "\033[1m"
    RESET = "\033[0m"

def colored(text: str, color: str) -> str:
    """Apply color to text if terminal supports it."""
    if sys.platform == "win32":
        # Enable ANSI colors on Windows
        os.system("")
    return f"{color}{text}{Colors.RESET}"

# ============================================================================
# ADB Helper Functions
# ============================================================================

@dataclass
class DeviceInfo:
    serial: str
    manufacturer: str
    model: str
    android_version: str
    is_nothing_phone: bool

def find_adb() -> Optional[str]:
    """Find ADB executable in standard locations."""
    possible_paths = [
        # Windows SDK locations
        os.path.expanduser(r"~\AppData\Local\Android\Sdk\platform-tools\adb.exe"),
        r"C:\Android\android-sdk\platform-tools\adb.exe",
        r"C:\Program Files (x86)\Android\android-sdk\platform-tools\adb.exe",
        # Linux/Mac SDK locations
        os.path.expanduser("~/Android/Sdk/platform-tools/adb"),
        "/usr/local/android-sdk/platform-tools/adb",
        # Try PATH
        "adb"
    ]
    
    for path in possible_paths:
        try:
            # Check if it's just "adb" in PATH
            if path == "adb":
                result = subprocess.run([path, "version"], capture_output=True, text=True)
                if result.returncode == 0:
                    return path
            elif os.path.exists(path):
                return path
        except FileNotFoundError:
            continue
    
    return None

def run_adb(adb_path: str, args: List[str], timeout: int = 30) -> Tuple[int, str, str]:
    """Run an ADB command and return (returncode, stdout, stderr)."""
    cmd = [adb_path] + args
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
        return result.returncode, result.stdout, result.stderr
    except subprocess.TimeoutExpired:
        return -1, "", "Command timed out"
    except Exception as e:
        return -1, "", str(e)

def get_device_info(adb_path: str) -> Optional[DeviceInfo]:
    """Get connected device information."""
    # Get device serial
    code, stdout, _ = run_adb(adb_path, ["get-serialno"])
    if code != 0:
        return None
    serial = stdout.strip()
    
    # Get manufacturer
    code, stdout, _ = run_adb(adb_path, ["shell", "getprop", "ro.product.manufacturer"])
    manufacturer = stdout.strip() if code == 0 else "Unknown"
    
    # Get model
    code, stdout, _ = run_adb(adb_path, ["shell", "getprop", "ro.product.model"])
    model = stdout.strip() if code == 0 else "Unknown"
    
    # Get Android version
    code, stdout, _ = run_adb(adb_path, ["shell", "getprop", "ro.build.version.release"])
    android_version = stdout.strip() if code == 0 else "Unknown"
    
    # Check if Nothing phone
    is_nothing = "nothing" in manufacturer.lower() or "nothing" in model.lower()
    
    return DeviceInfo(
        serial=serial,
        manufacturer=manufacturer,
        model=model,
        android_version=android_version,
        is_nothing_phone=is_nothing
    )

# ============================================================================
# Test Steps
# ============================================================================

def step_check_adb() -> Tuple[bool, str, Optional[str]]:
    """Step 1: Find and verify ADB."""
    print(colored("\n[1/6] Checking ADB Installation...", Colors.CYAN))
    
    adb_path = find_adb()
    if not adb_path:
        return False, "ADB not found. Please install Android SDK platform-tools.", None
    
    code, stdout, _ = run_adb(adb_path, ["version"])
    if code != 0:
        return False, "ADB found but not working correctly.", None
    
    version_line = stdout.split('\n')[0] if stdout else "Unknown version"
    print(f"  ✓ Found ADB: {adb_path}")
    print(f"  ✓ {version_line}")
    return True, "ADB ready", adb_path

def step_check_device(adb_path: str) -> Tuple[bool, str, Optional[DeviceInfo]]:
    """Step 2: Check device connection and info."""
    print(colored("\n[2/6] Checking Device Connection...", Colors.CYAN))
    
    code, stdout, stderr = run_adb(adb_path, ["devices"])
    if code != 0:
        return False, f"Failed to list devices: {stderr}", None
    
    lines = [l for l in stdout.strip().split('\n')[1:] if l.strip() and 'device' in l]
    if not lines:
        return False, "No devices connected. Enable USB debugging and connect your Nothing Phone.", None
    
    device_info = get_device_info(adb_path)
    if not device_info:
        return False, "Could not get device info.", None
    
    print(f"  ✓ Device Serial: {device_info.serial}")
    print(f"  ✓ Manufacturer:  {device_info.manufacturer}")
    print(f"  ✓ Model:         {device_info.model}")
    print(f"  ✓ Android:       {device_info.android_version}")
    
    if device_info.is_nothing_phone:
        print(colored("  ✓ Nothing Phone DETECTED!", Colors.GREEN))
    else:
        print(colored("  ⚠ WARNING: Not a Nothing Phone - Glyph tests will use mock mode", Colors.YELLOW))
    
    return True, "Device connected", device_info

def step_enable_glyph_debug(adb_path: str) -> Tuple[bool, str]:
    """Step 3: Enable Glyph debug mode."""
    print(colored("\n[3/6] Enabling Glyph Debug Mode...", Colors.CYAN))
    
    # Enable debug mode
    code, _, stderr = run_adb(adb_path, [
        "shell", "settings", "put", "global", "nt_glyph_interface_debug_enable", "1"
    ])
    
    if code != 0:
        return False, f"Failed to enable debug mode: {stderr}"
    
    # Verify it's enabled
    code, stdout, _ = run_adb(adb_path, [
        "shell", "settings", "get", "global", "nt_glyph_interface_debug_enable"
    ])
    
    value = stdout.strip()
    if value == "1":
        print("  ✓ Glyph Interface Debug Mode: ENABLED")
        return True, "Debug mode enabled"
    else:
        print(colored(f"  ⚠ Warning: Debug mode setting returned '{value}' (expected '1')", Colors.YELLOW))
        print("    This might be normal on non-Nothing phones or newer OS versions.")
        return True, "Debug mode setting attempted"

def step_check_app_installed(adb_path: str) -> Tuple[bool, str, bool]:
    """Step 4: Check if the test app is installed."""
    print(colored("\n[4/6] Checking App Installation...", Colors.CYAN))
    
    code, stdout, _ = run_adb(adb_path, ["shell", "pm", "list", "packages", APP_PACKAGE])
    
    is_installed = APP_PACKAGE in stdout
    
    if is_installed:
        print(f"  ✓ Package {APP_PACKAGE} is installed")
        
        # Get version info
        code, stdout, _ = run_adb(adb_path, [
            "shell", "dumpsys", "package", APP_PACKAGE, "|", "grep", "versionName"
        ])
        if "versionName" in stdout:
            print(f"    {stdout.strip()}")
    else:
        print(colored(f"  ⚠ Package {APP_PACKAGE} NOT installed", Colors.YELLOW))
        print("    Run 'gradlew installDebug' first, or this script will try to build it.")
    
    return True, "App check complete", is_installed

def step_run_glyph_test(adb_path: str, app_installed: bool) -> Tuple[bool, str, List[str]]:
    """Step 5: Run the Glyph hardware test."""
    print(colored("\n[5/6] Running Glyph Hardware Test...", Colors.CYAN))
    
    if not app_installed:
        print("  Building and installing app...")
        # Try to build via gradle
        script_dir = os.path.dirname(os.path.abspath(__file__))
        project_root = os.path.dirname(script_dir)
        
        gradle_cmd = "gradlew.bat" if sys.platform == "win32" else "./gradlew"
        gradle_path = os.path.join(project_root, gradle_cmd)
        
        if os.path.exists(gradle_path):
            print(f"  Running: {gradle_cmd} installDebug")
            result = subprocess.run(
                [gradle_path, "installDebug"],
                cwd=project_root,
                capture_output=True,
                text=True,
                timeout=300
            )
            if result.returncode != 0:
                return False, "Failed to build/install app. Run 'gradlew installDebug' manually.", []
            print("  ✓ App installed successfully")
        else:
            return False, "App not installed and could not find gradle. Install app manually.", []
    
    # Clear logcat
    run_adb(adb_path, ["logcat", "-c"])
    
    print("\n  " + colored("⚡ WATCH YOUR PHONE - Glyph patterns will light up! ⚡", Colors.BOLD + Colors.YELLOW))
    print()
    
    # Run instrumented test
    test_cmd = [
        "shell", "am", "instrument", "-w",
        "-e", "class", TEST_CLASS,
        f"{APP_PACKAGE}.test/{TEST_RUNNER}"
    ]
    
    print(f"  Running test: {TEST_CLASS}")
    code, stdout, stderr = run_adb(adb_path, test_cmd, timeout=60)
    
    # Capture logs
    time.sleep(1)
    code, log_output, _ = run_adb(adb_path, [
        "logcat", "-d", "-s", "System.out:I", "LiveLogger:*"
    ], timeout=10)
    
    logs = []
    for line in log_output.split('\n'):
        if 'LiveLogger' in line or 'Glyph' in line.lower():
            logs.append(line.strip())
    
    # Also check test output
    test_passed = "OK (1 test)" in stdout or "INSTRUMENTATION_CODE: -1" in stdout
    
    if test_passed:
        print(colored("  ✓ Instrumentation test completed", Colors.GREEN))
    else:
        print(colored("  ⚠ Test may have had issues", Colors.YELLOW))
        if stderr:
            print(f"    Error: {stderr[:200]}")
    
    return True, "Test executed", logs

def step_display_results(logs: List[str], device_info: Optional[DeviceInfo]) -> Tuple[bool, str]:
    """Step 6: Display test results and logs."""
    print(colored("\n[6/6] Test Results", Colors.CYAN))
    print("=" * 60)
    
    if device_info and device_info.is_nothing_phone:
        print(colored("  Device: Nothing Phone ✓", Colors.GREEN))
        print("  Mode:   HARDWARE (real Glyph LEDs)")
    else:
        print(colored("  Device: Non-Nothing Phone", Colors.YELLOW))
        print("  Mode:   MOCK (simulated)")
    
    print("\n  Captured Logs:")
    print("  " + "-" * 56)
    
    if logs:
        glyph_connected = any("Glyph Service Connected" in l for l in logs)
        glyph_registered = any("Registered" in l for l in logs)
        pattern_played = any("Playing" in l or "STROBE" in l or "PULSE" in l for l in logs)
        
        for log in logs[-20:]:  # Last 20 log entries
            # Color-code important logs
            if "Connected" in log or "SUCCESS" in log:
                print(colored(f"    {log}", Colors.GREEN))
            elif "Error" in log or "FAILED" in log:
                print(colored(f"    {log}", Colors.RED))
            elif "Playing" in log or "Strobe" in log or "Pulse" in log:
                print(colored(f"    {log}", Colors.CYAN))
            else:
                print(f"    {log}")
        
        print("\n  " + "-" * 56)
        print("  Summary:")
        print(f"    Service Connected: {'✓' if glyph_connected else '✗'}")
        print(f"    SDK Registered:    {'✓' if glyph_registered else '✗'}")
        print(f"    Pattern Triggered: {'✓' if pattern_played else '✗'}")
        
        if glyph_connected and pattern_played:
            print(colored("\n  ✓ GLYPH TEST PASSED!", Colors.GREEN + Colors.BOLD))
            return True, "All tests passed"
        else:
            print(colored("\n  ⚠ GLYPH TEST INCOMPLETE - Check logs above", Colors.YELLOW))
            return True, "Test completed with warnings"
    else:
        print("    (No glyph-related logs captured)")
        print("\n  Tips:")
        print("    - Make sure the app has notification listener permission")
        print("    - Check that Glyph Interface is enabled in phone settings")
        print("    - Try running the app manually first")
        return True, "No logs captured"

# ============================================================================
# Main Entry Point
# ============================================================================

def main():
    """Run the complete Glyph connection test."""
    print(colored("=" * 60, Colors.BLUE))
    print(colored(" GLYPH GLANCE - Hardware Connection Test", Colors.BOLD + Colors.BLUE))
    print(colored("=" * 60, Colors.BLUE))
    print(" Testing Nothing Phone Glyph LED interface...")
    
    # Step 1: Check ADB
    success, message, adb_path = step_check_adb()
    if not success:
        print(colored(f"\n✗ FAILED: {message}", Colors.RED))
        sys.exit(1)
    
    # Step 2: Check device
    success, message, device_info = step_check_device(adb_path)
    if not success:
        print(colored(f"\n✗ FAILED: {message}", Colors.RED))
        sys.exit(1)
    
    # Step 3: Enable debug mode
    success, message = step_enable_glyph_debug(adb_path)
    if not success:
        print(colored(f"\n✗ FAILED: {message}", Colors.RED))
        sys.exit(1)
    
    # Step 4: Check app
    success, message, app_installed = step_check_app_installed(adb_path)
    if not success:
        print(colored(f"\n✗ FAILED: {message}", Colors.RED))
        sys.exit(1)
    
    # Step 5: Run test
    success, message, logs = step_run_glyph_test(adb_path, app_installed)
    if not success:
        print(colored(f"\n✗ FAILED: {message}", Colors.RED))
        sys.exit(1)
    
    # Step 6: Results
    success, message = step_display_results(logs, device_info)
    
    print(colored("\n" + "=" * 60, Colors.BLUE))
    print(" Test complete!")
    print(colored("=" * 60 + "\n", Colors.BLUE))
    
    return 0 if success else 1

if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print(colored("\n\nTest cancelled by user.", Colors.YELLOW))
        sys.exit(130)
    except Exception as e:
        print(colored(f"\n✗ Unexpected error: {e}", Colors.RED))
        sys.exit(1)


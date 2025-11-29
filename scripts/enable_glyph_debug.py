import os
import subprocess
import time

# Try to find adb
possible_paths = [
    os.path.expanduser(r"~\AppData\Local\Android\Sdk\platform-tools\adb.exe"),
    r"C:\Android\android-sdk\platform-tools\adb.exe",
    r"C:\Program Files (x86)\Android\android-sdk\platform-tools\adb.exe"
]

adb_path = None
for path in possible_paths:
    if os.path.exists(path):
        adb_path = path
        break

if not adb_path:
    print("Could not find adb.exe.")
else:
    print(f"Found ADB at: {adb_path}")
    print("Enabling Nothing Glyph Debug Mode...")
    
    # Run the command to enable 3rd party glyph control
    cmd = [adb_path, "shell", "settings", "put", "global", "nt_glyph_interface_debug_enable", "1"]
    result = subprocess.run(cmd, capture_output=True, text=True)
    
    if result.returncode == 0:
        print("SUCCESS: Glyph Interface Debug Mode ENABLED.")
        print("You may need to restart your phone or toggle the Glyph Interface switch in Settings.")
    else:
        print(f"FAILED: {result.stderr}")
        
    print("\nChecking current value:")
    subprocess.run([adb_path, "shell", "settings", "get", "global", "nt_glyph_interface_debug_enable"])

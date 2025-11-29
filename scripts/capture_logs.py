import os
import subprocess
import time

# Try to find adb in standard Android SDK locations
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
    print("Could not find adb.exe. Please check your Android SDK installation.")
else:
    print(f"Found ADB at: {adb_path}")
    print("Clearing logcat...")
    subprocess.run([adb_path, "logcat", "-c"])
    
    print("\nCapturing logs for 15 seconds... PLEASE RUN THE TEST NOW!")
    
    # Capture logs to a file
    with open("glyph_logs.txt", "w") as f:
        process = subprocess.Popen(
            [adb_path, "logcat", "-s", "System.out", "LiveLogger"], 
            stdout=f, 
            stderr=subprocess.STDOUT
        )
        time.sleep(15)
        process.terminate()
        
    print("\n--- LOGS CAPTURED ---")
    with open("glyph_logs.txt", "r") as f:
        content = f.read()
        if content:
            print(content)
        else:
            print("No logs found. Did you run the test?")

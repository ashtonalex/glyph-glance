import subprocess
import time
import sys
import shutil
import os

def get_adb_path():
    """Finds the ADB executable path on Windows."""
    if shutil.which("adb"):
        return "adb"
    
    user_home = os.path.expanduser("~")
    possible_paths = [
        os.path.join(user_home, "AppData", "Local", "Android", "Sdk", "platform-tools", "adb.exe"),
        "C:\\Android\\platform-tools\\adb.exe",
        "C:\\Program Files (x86)\\Android\\android-sdk\\platform-tools\\adb.exe"
    ]
    
    for path in possible_paths:
        if os.path.exists(path):
            return f'"{path}"'
    return None

ADB_CMD = get_adb_path()

def send_notification(title, content):
    if not ADB_CMD:
        print("Error: ADB not found.")
        return

    print(f"Sending Notification: [{title}] {content}")
    # Escape single quotes
    safe_title = title.replace("'", "'\\''")
    safe_content = content.replace("'", "'\\''")
    
    # Send via ADB
    cmd = f"{ADB_CMD} shell cmd notification post -S bigtext -t '{safe_title}' 'Tag' '{safe_content}'"
    
    try:
        subprocess.run(cmd, check=True, capture_output=True, shell=True)
        print("Success! Notification sent.")
    except subprocess.CalledProcessError as e:
        print(f"Error: {e}")

def main():
    print("=== Glyph Activator ===")
    print("This script sends a notification to trigger the Glyph Interface.")
    print("Ensure the 'Glyph Glance' app has Notification Access enabled in Android Settings.")
    
    # Send a message that is likely to trigger "URGENT" or "MATRIX"
    # Based on GlyphIntelligenceEngine logic, urgency >= 4 triggers it.
    # We'll use keywords that might be flagged as urgent by a mock AI or rule.
    
    print("\nSending 'URGENT' trigger...")
    send_notification("Boss", "URGENT: Server is down! Fix it ASAP immediately.")
    
    time.sleep(2)
    
    print("\nSending 'MATRIX' trigger...")
    send_notification("Morpheus", "The Matrix has you. Follow the white rabbit.")

    print("\nDone. Check your device.")

if __name__ == "__main__":
    main()

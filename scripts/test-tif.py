import subprocess
import time
import random
import shutil
import sys
import os

def get_adb_path():
    """Finds the ADB executable path on Windows."""
    # Check if adb is already in PATH
    if shutil.which("adb"):
        return "adb"
    
    # Common Android SDK paths on Windows
    user_home = os.path.expanduser("~")
    possible_paths = [
        os.path.join(user_home, "AppData", "Local", "Android", "Sdk", "platform-tools", "adb.exe"),
        "C:\\Android\\platform-tools\\adb.exe",
        "C:\\Program Files (x86)\\Android\\android-sdk\\platform-tools\\adb.exe"
    ]
    
    for path in possible_paths:
        if os.path.exists(path):
            return f'"{path}"'  # Quote path for shell execution
            
    return None

ADB_CMD = get_adb_path()

def check_adb_connection():
    """Checks if an ADB device is connected."""
    if not ADB_CMD:
        print("Error: 'adb' executable not found. Please install Android Platform Tools or add to PATH.")
        return False
    
    try:
        # We need to run this command carefully since ADB_CMD might contain quotes
        cmd = f"{ADB_CMD} devices"
        result = subprocess.run(cmd, capture_output=True, text=True, shell=True)
        lines = result.stdout.strip().split('\n')
        # Filter out "List of devices attached" and empty lines
        devices = [line for line in lines[1:] if line.strip() and "\tdevice" in line]
        
        if not devices:
            print("Warning: No ADB devices connected. Notifications will fail.")
            return False
        
        print(f"Connected to {len(devices)} device(s).")
        return True
    except Exception as e:
        print(f"Error checking ADB: {e}")
        return False

def send_adb_notification(title, content):
    """
    Sends a notification via ADB using the 'cmd notification post' command.
    Escapes single quotes for the Android shell.
    """
    if not ADB_CMD:
        print("Error: ADB not found.")
        return

    # Escape single quotes for Android shell usage
    safe_title = title.replace("'", "'\\''")
    safe_content = content.replace("'", "'\\''")
    
    # Construct the command
    # adb shell cmd notification post -S bigtext -t '{title}' 'Tag' '{content}'
    cmd = f"{ADB_CMD} shell cmd notification post -S bigtext -t '{safe_title}' 'Tag' '{safe_content}'"
    
    try:
        # Use shell=True for Windows to find the adb command. 
        # Added text=True to get readable output.
        result = subprocess.run(cmd, check=True, capture_output=True, shell=True, text=True)
        print(f" -> Sent: [{title}] {content}")
    except subprocess.CalledProcessError as e:
        print(f"Error sending notification: {e}")
        print(f"STDOUT: {e.stdout}")
        print(f"STDERR: {e.stderr}")

def simulate_split_texter():
    """Simulates a split texter sending multiple short messages rapidly."""
    print("\n--- Simulating Split Texter ---")
    messages = ["Hey", "Are you", "There?", "Urgent", "Pls pick up"]
    for msg in messages:
        send_adb_notification("Split Texter", msg)
        delay = random.uniform(0.2, 0.8)
        time.sleep(delay)
    print("--- Split Texter Simulation Complete ---\n")

def simulate_urgent_boss():
    """Simulates an urgent message to trigger High Priority logic."""
    print("\n--- Simulating Urgent Boss ---")
    send_adb_notification("Boss", "We have a Deadline ASAP. Fix the Error now!")
    print("--- Urgent Boss Simulation Complete ---\n")

def simulate_casual_chat():
    """Simulates a casual, low-urgency message."""
    print("\n--- Simulating Casual Chat ---")
    send_adb_notification("Friend", "See you for dinner at 7")
    print("--- Casual Chat Simulation Complete ---\n")

def simulate_matrix_code():
    """Simulates a message that triggers the Matrix Rain effect."""
    print("\n--- Simulating Matrix Code ---")
    send_adb_notification("Operator", "Wake up, Neo... The Matrix has you.")
    print("--- Matrix Code Simulation Complete ---\n")

def main():
    print("Initializing Mock Notifier for Glyph-Glance...")
    check_adb_connection()
    
    while True:
        print("\n=== Mock Notifier Menu ===")
        print("[1] Split Text Burst")
        print("[2] Urgent Alert")
        print("[3] Casual Chat")
        print("[4] Matrix Code")
        print("[q] Quit")
        
        try:
            choice = input("Select an option: ").strip().lower()
        except KeyboardInterrupt:
            print("\nExiting...")
            break
        
        if choice == '1':
            simulate_split_texter()
        elif choice == '2':
            simulate_urgent_boss()
        elif choice == '3':
            simulate_casual_chat()
        elif choice == '4':
            simulate_matrix_code()
        elif choice == 'q':
            print("Exiting...")
            break
        else:
            print("Invalid option, please try again.")

if __name__ == "__main__":
    main()
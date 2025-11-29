import subprocess
import time
import random
import shutil
import os
import sys

# --- Configuration & Utility Functions ---

def get_adb_path():
    """Finds the ADB executable path on Windows or assumes 'adb' on POSIX systems."""
    # Check if adb is already in PATH
    if shutil.which("adb"):
        return "adb"
    
    # Common Android SDK paths (for Windows fallback)
    user_home = os.path.expanduser("~")
    possible_paths = [
        os.path.join(user_home, "AppData", "Local", "Android", "Sdk", "platform-tools", "adb.exe"),
        "C:\\Android\\platform-tools\\adb.exe",
    ]
    
    for path in possible_paths:
        if os.path.exists(path):
            return f'"{path}"'  # Quote path for shell execution
            
    return None

ADB_CMD = get_adb_path()

def check_adb_connection():
    """Checks if an ADB device is connected and confirms ADB is found."""
    if not ADB_CMD:
        print("Error: 'adb' executable not found. Please install Android Platform Tools or add to PATH.")
        return False
    
    try:
        cmd = f"{ADB_CMD} devices"
        # Running the command with shell=True for path resolution
        result = subprocess.run(cmd, capture_output=True, text=True, shell=True, check=False)
        
        if result.returncode != 0:
            print(f"Error running '{cmd}':\n{result.stderr.strip()}")
            return False

        lines = result.stdout.strip().split('\n')
        # Filter for lines indicating a device is connected
        devices = [line for line in lines[1:] if line.strip() and "\tdevice" in line]
        
        if not devices:
            print("Warning: No ADB devices connected. Notifications will fail.")
            print("Please ensure your device is connected and USB debugging is enabled.")
            return False
        
        print(f"✅ Connected to {len(devices)} device(s).")
        return True
    except Exception as e:
        print(f"Fatal Error checking ADB connection: {e}")
        return False

def send_adb_notification(title, content, app_package="com.example.friend_app"):
    """
    Sends a notification via ADB using the 'cmd notification post' command.
    The 'post' command uses the system shell package by default, which your listener should catch.
    """
    if not ADB_CMD:
        print("Error: ADB not found.")
        return

    # Escape single quotes for Android shell usage
    safe_title = title.replace("'", "'\\''")
    safe_content = content.replace("'", "'\\''")
    
    # -S bigtext provides enough space for long content
    # -p allows spoofing the package name (useful if we want to test app-specific logic later)
    # However, 'cmd notification post' often uses the shell package. We'll use a tag instead.
    
    # Construct the command
    cmd = f"{ADB_CMD} shell cmd notification post -S bigtext -t '{safe_title}' 'GLYPH_TEST' '{safe_content}'"
    
    try:
        subprocess.run(cmd, check=True, capture_output=True, shell=True, text=True)
        print(f" -> Sent: [Sender: {title}] Content: '{content.strip()}'")
    except subprocess.CalledProcessError as e:
        print(f"Error sending notification (Exit Code {e.returncode}):")
        print(f"STDOUT: {e.stdout.strip()}")
        print(f"STDERR: {e.stderr.strip()}")
    except Exception as e:
        print(f"An unexpected error occurred: {e}")


# --- Test Scenarios (Mapping to Intent-Based Categories) ---

def simulate_split_texter():
    """
    Simulates a split texter (SOCIAL) to test the Buffer Engine (Feature 1).
    Expected: Notifications are suppressed, then a single alert fires after 20s.
    """
    print("\n--- 1. Simulating Split Texter (Testing Buffer Engine) ---")
    messages = [
        "Hey", 
        "Are you there?", 
        "Just saw that message", 
        "Can you check your email?", 
        "Pls pick up"
    ]
    
    send_adb_notification("Friend Alex", "Starting rapid fire...")
    time.sleep(1) 
    
    for i, msg in enumerate(messages):
        send_adb_notification("Friend Alex", msg)
        delay = random.uniform(0.2, 0.8)
        time.sleep(delay)
        if i < len(messages) - 1:
            sys.stdout.write(f"\r   ...Waiting {delay:.2f}s before next text...")
            sys.stdout.flush()
    
    print("\n--- Split Texter Simulation Complete (Waiting for Flush) ---\n")
    print("Monitor the phone for a single Glyph flash after the 20s buffer timeout.")


def simulate_critical_action():
    """
    Simulates a CRITICAL ACTION message (e.g., OTP/Delivery) to trigger high urgency.
    Expected: Rapid Strobe (Red/White) - bypasses buffer.
    """
    print("\n--- 2. Simulating CRITICAL ACTION ---")
    send_adb_notification("Bank Alert", "Your one-time verification code is 438192. Do not share this.")
    send_adb_notification("Uber Eats", "Your driver is 1 minute away and running a bit late.")
    print("--- Critical Action Simulation Complete ---\n")


def simulate_social_conversation():
    """
    Simulates a SOCIAL CONVERSATION message.
    Expected: Double Pulse (Breathing) or low urgency.
    """
    print("\n--- 3. Simulating SOCIAL CONVERSATION ---")
    send_adb_notification("Group Chat", "Lmao! Check out this video link.")
    send_adb_notification("Mom", "Just checking in. Hope you had a nice day.")
    print("--- Social Conversation Simulation Complete ---\n")


def simulate_passive_update():
    """
    Simulates a PASSIVE UPDATE message (informational/low priority).
    Expected: Slow Fill & Fade, or filtered (minimal distraction).
    """
    print("\n--- 4. Simulating PASSIVE UPDATE ---")
    send_adb_notification("Amazon", "Your order #81234 has shipped and will arrive tomorrow.")
    send_adb_notification("Calendar", "Reminder: Meeting starting in 5 minutes.")
    print("--- Passive Update Simulation Complete ---\n")


def simulate_noise_filter():
    """
    Simulates a NOISE message (marketing/spam) to test the filter rules.
    Expected: No Glyph flash (filtered).
    """
    print("\n--- 5. Simulating NOISE FILTER ---")
    send_adb_notification("PromoDeals", "FLASH SALE! 50% OFF all items if you order in the next hour.")
    send_adb_notification("GameCenter", "Your stamina has refilled! Log in to play now.")
    print("--- Noise Filter Simulation Complete ---\n")


# --- Main Execution ---

def main():
    print("╔════════════════════════════════════════╗")
    print("║   GLYPH-GLANCE MOCK NOTIFICATION TOOL  ║")
    print("╚════════════════════════════════════════╝")
    
    if not check_adb_connection():
        # If not connected, continue but warn user commands will fail.
        pass
    
    while True:
        print("\n=== Test Scenarios Menu ===")
        print("[1] Split Text Burst (Buffer Test)")
        print("[2] CRITICAL ACTION (OTP, Driver Near)")
        print("[3] SOCIAL CONVERSATION (Friend/Family Check-in)")
        print("[4] PASSIVE UPDATE (Shipping, Calendar Reminder)")
        print("[5] NOISE FILTER (Sale/Game Spam)")
        print("[q] Quit")
        
        try:
            choice = input("\nSelect an option: ").strip().lower()
        except KeyboardInterrupt:
            print("\nExiting...")
            break
        
        if choice == '1':
            simulate_split_texter()
        elif choice == '2':
            simulate_critical_action()
        elif choice == '3':
            simulate_social_conversation()
        elif choice == '4':
            simulate_passive_update()
        elif choice == '5':
            simulate_noise_filter()
        elif choice == 'q':
            print("Exiting...")
            break
        else:
            print("Invalid option, please try again.")

if __name__ == "__main__":
    main()
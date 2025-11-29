#!/usr/bin/env python3
"""
GLYPH-GLANCE Notification Testing Tool
=======================================
Send test notifications via ADB to test urgency scoring, semantic matching,
buffer engine, and Glyph LED patterns.

Usage:
    python test-tif.py              # Interactive menu
    python test-tif.py --burst 10   # Send 10 rapid notifications
    python test-tif.py --scenario 2 # Run scenario 2 directly
"""

import subprocess
import time
import random
import shutil
import os
import sys
import argparse
from datetime import datetime
from typing import Optional, List, Tuple

# --- ANSI Colors for Terminal Output ---
class Colors:
    HEADER = '\033[95m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    RED = '\033[91m'
    BOLD = '\033[1m'
    DIM = '\033[2m'
    RESET = '\033[0m'
    
    @staticmethod
    def disable():
        """Disable colors for non-supporting terminals"""
        Colors.HEADER = Colors.BLUE = Colors.CYAN = ''
        Colors.GREEN = Colors.YELLOW = Colors.RED = ''
        Colors.BOLD = Colors.DIM = Colors.RESET = ''

# Detect Windows and disable colors if needed
if os.name == 'nt':
    try:
        os.system('color')  # Enable ANSI on Windows 10+
    except:
        Colors.disable()

# --- Configuration ---
NOTIFICATION_PRESETS = {
    # Format: (sender, message, expected_urgency, category)
    "critical": [
        ("Bank Security", "🚨 Unusual login detected from new device. Verify now: 847291", 6, "CRITICAL"),
        ("Emergency Alert", "AMBER ALERT: Missing child in your area. Details inside.", 6, "CRITICAL"),
        ("Google", "Your password was just changed. If this wasn't you, secure your account immediately.", 6, "CRITICAL"),
        ("PayPal", "Suspicious activity detected. Your account has been temporarily limited.", 6, "CRITICAL"),
        ("Fire Department", "Evacuation order issued for your neighborhood.", 6, "CRITICAL"),
    ],
    "urgent": [
        ("Uber Eats", "Your driver is arriving in 1 minute!", 5, "ACTION"),
        ("Bank Alert", "Your one-time verification code is 438192", 5, "ACTION"),
        ("DoorDash", "Your order is being prepared and will arrive in 15 mins", 4, "ACTION"),
        ("Mom", "Call me back ASAP, it's important!", 5, "SOCIAL"),
        ("Boss", "Need you to join the call now, client is waiting", 5, "WORK"),
    ],
    "social": [
        ("Friend Alex", "Hey! Are you free tonight?", 3, "SOCIAL"),
        ("Group Chat", "Lmao check out this meme 😂", 2, "SOCIAL"),
        ("Instagram", "john_doe liked your photo", 2, "SOCIAL"),
        ("Mom", "Just checking in, hope you had a nice day ❤️", 3, "SOCIAL"),
        ("Discord", "New message in #general", 2, "SOCIAL"),
    ],
    "passive": [
        ("e", "e", 0, "e"),
    ],
    "noise": [
        ("PromoDeals", "🔥 FLASH SALE! 50% OFF everything!", 1, "NOISE"),
        ("GameCenter", "Your stamina has refilled! Play now!", 1, "NOISE"),
        ("AdNetwork", "You won't believe this one weird trick!", 1, "NOISE"),
        ("Casino App", "🎰 Spin the wheel for FREE coins!", 1, "NOISE"),
        ("Shopping App", "Items in your cart are selling fast!", 1, "NOISE"),
    ],
}

# Semantic keywords that should trigger urgency 6 (instant priority)
URGENCY_6_KEYWORDS = [
    "emergency", "urgent", "immediately", "asap", "critical",
    "911", "help", "dying", "hospital", "accident", "fire",
    "amber alert", "security alert", "breach", "hacked",
]

# --- Utility Functions ---

def get_adb_path() -> Optional[str]:
    """Finds the ADB executable path."""
    if shutil.which("adb"):
        return "adb"
    
    user_home = os.path.expanduser("~")
    possible_paths = [
        os.path.join(user_home, "AppData", "Local", "Android", "Sdk", "platform-tools", "adb.exe"),
        os.path.join(user_home, "Library", "Android", "sdk", "platform-tools", "adb"),
        "C:\\Android\\platform-tools\\adb.exe",
        "/usr/local/bin/adb",
    ]
    
    for path in possible_paths:
        if os.path.exists(path):
            return f'"{path}"' if ' ' in path else path
    
    return None

ADB_CMD = get_adb_path()

def print_header(text: str, char: str = "═"):
    """Print a styled header."""
    width = 50
    print(f"\n{Colors.CYAN}{char * width}")
    print(f"{Colors.BOLD}  {text}")
    print(f"{Colors.CYAN}{char * width}{Colors.RESET}")

def print_success(text: str):
    print(f"{Colors.GREEN}✓ {text}{Colors.RESET}")

def print_error(text: str):
    print(f"{Colors.RED}✗ {text}{Colors.RESET}")

def print_warning(text: str):
    print(f"{Colors.YELLOW}⚠ {text}{Colors.RESET}")

def print_info(text: str):
    print(f"{Colors.DIM}  {text}{Colors.RESET}")

def check_adb_connection() -> bool:
    """Check if an ADB device is connected."""
    if not ADB_CMD:
        print_error("ADB executable not found. Install Android Platform Tools or add to PATH.")
        return False
    
    try:
        result = subprocess.run(
            f"{ADB_CMD} devices", 
            capture_output=True, text=True, shell=True, check=False
        )
        
        if result.returncode != 0:
            print_error(f"ADB error: {result.stderr.strip()}")
            return False
        
        lines = result.stdout.strip().split('\n')
        devices = [l for l in lines[1:] if l.strip() and "\tdevice" in l]
        
        if not devices:
            print_warning("No ADB devices connected.")
            print_info("Ensure USB debugging is enabled and device is connected.")
            return False
        
        device_id = devices[0].split('\t')[0]
        print_success(f"Connected to device: {device_id}")
        return True
        
    except Exception as e:
        print_error(f"Fatal ADB error: {e}")
        return False

def send_notification(title: str, content: str, tag: str = "GLYPH_TEST") -> bool:
    """Send a notification via ADB."""
    if not ADB_CMD:
        return False
    
    safe_title = title.replace("'", "'\\''").replace('"', '\\"')
    safe_content = content.replace("'", "'\\''").replace('"', '\\"')
    
    cmd = f"{ADB_CMD} shell cmd notification post -S bigtext -t '{safe_title}' '{tag}' '{safe_content}'"
    
    try:
        result = subprocess.run(cmd, check=True, capture_output=True, shell=True, text=True)
        return True
    except subprocess.CalledProcessError as e:
        print_error(f"Send failed: {e.stderr.strip()}")
        return False

def send_with_log(title: str, content: str, expected_urgency: int = None, category: str = None):
    """Send notification with formatted logging."""
    success = send_notification(title, content)
    
    urgency_color = Colors.GREEN
    if expected_urgency:
        if expected_urgency >= 5:
            urgency_color = Colors.RED
        elif expected_urgency >= 3:
            urgency_color = Colors.YELLOW
    
    urgency_str = f" [{urgency_color}U:{expected_urgency}{Colors.RESET}]" if expected_urgency else ""
    category_str = f" ({Colors.DIM}{category}{Colors.RESET})" if category else ""
    
    status = Colors.GREEN + "✓" if success else Colors.RED + "✗"
    print(f"  {status}{Colors.RESET} [{Colors.BOLD}{title}{Colors.RESET}]{urgency_str}{category_str}")
    print(f"      {Colors.DIM}\"{content[:60]}{'...' if len(content) > 60 else ''}\"{Colors.RESET}")
    
    return success

# --- Test Scenarios ---

def scenario_split_texter():
    """Test buffer engine with rapid successive messages."""
    print_header("SPLIT TEXTER TEST (Buffer Engine)", "─")
    print_info("Sending 5 messages rapidly from same sender...")
    print_info("Expected: Messages buffered, single Glyph flash after timeout\n")
    
    messages = [
        "Hey",
        "Are you there?",
        "Just saw your message",
        "Can you check your email?",
        "Please respond when you can"
    ]
    
    start_time = time.time()
    
    for i, msg in enumerate(messages, 1):
        send_with_log("Friend Alex", msg, 3, "SOCIAL")
        if i < len(messages):
            delay = random.uniform(0.3, 0.8)
            print(f"      {Colors.DIM}...waiting {delay:.1f}s...{Colors.RESET}")
            time.sleep(delay)
    
    elapsed = time.time() - start_time
    print(f"\n  {Colors.CYAN}All messages sent in {elapsed:.1f}s{Colors.RESET}")
    print_info("Watch for buffered Glyph flash after 20s timeout")

def scenario_critical():
    """Test critical/urgent notifications (should bypass buffer)."""
    print_header("CRITICAL NOTIFICATIONS (Urgency 5-6)", "─")
    print_info("These should trigger immediate high-priority Glyph patterns\n")
    
    for sender, msg, urgency, cat in NOTIFICATION_PRESETS["critical"][:3]:
        send_with_log(sender, msg, urgency, cat)
        time.sleep(0.5)

def scenario_social():
    """Test social/casual notifications."""
    print_header("SOCIAL NOTIFICATIONS (Urgency 2-3)", "─")
    print_info("Casual messages - moderate priority\n")
    
    for sender, msg, urgency, cat in NOTIFICATION_PRESETS["social"]:
        send_with_log(sender, msg, urgency, cat)
        time.sleep(0.3)

def scenario_passive():
    """Test passive/informational notifications."""
    print_header("PASSIVE UPDATES (Urgency 1-2)", "─")
    print_info("Informational - low priority, minimal distraction\n")
    
    for sender, msg, urgency, cat in NOTIFICATION_PRESETS["passive"]:
        send_with_log(sender, msg, urgency, cat)
        time.sleep(0.3)

def scenario_noise():
    """Test noise/spam notifications (should be filtered)."""
    print_header("NOISE FILTER TEST (Should be filtered)", "─")
    print_info("Marketing/spam - should be filtered or minimal\n")
    
    for sender, msg, urgency, cat in NOTIFICATION_PRESETS["noise"]:
        send_with_log(sender, msg, urgency, cat)
        time.sleep(0.3)

def scenario_urgency_keywords():
    """Test semantic keywords that trigger urgency 6."""
    print_header("URGENCY 6 KEYWORD TEST (Instant Priority)", "─")
    print_info("Messages with critical keywords - AI should be skipped\n")
    
    test_messages = [
        ("Security Alert", "EMERGENCY: Your account has been compromised!", 6),
        ("Mom", "Come to the hospital immediately, it's dad", 6),
        ("Home Security", "CRITICAL: Motion detected at back door", 6),
        ("Work", "This is urgent - client meeting moved to now", 6),
        ("Bank", "Security breach detected. Change password ASAP", 6),
    ]
    
    for sender, msg, urgency in test_messages:
        send_with_log(sender, msg, urgency, "CRITICAL")
        time.sleep(0.5)

def scenario_burst(count: int = 10, delay_ms: int = 200):
    """Rapid fire notifications for stress testing."""
    print_header(f"BURST TEST ({count} notifications @ {delay_ms}ms)", "─")
    print_info("Testing notification handling under load\n")
    
    all_presets = []
    for category in NOTIFICATION_PRESETS.values():
        all_presets.extend(category)
    
    start_time = time.time()
    success_count = 0
    
    for i in range(count):
        sender, msg, urgency, cat = random.choice(all_presets)
        sender = f"Burst #{i+1}: {sender}"
        
        if send_notification(sender, msg):
            success_count += 1
            urgency_color = Colors.RED if urgency >= 5 else Colors.YELLOW if urgency >= 3 else Colors.GREEN
            print(f"  {Colors.GREEN}✓{Colors.RESET} [{i+1}/{count}] {urgency_color}U:{urgency}{Colors.RESET} {sender[:30]}")
        else:
            print(f"  {Colors.RED}✗{Colors.RESET} [{i+1}/{count}] Failed")
        
        if i < count - 1:
            time.sleep(delay_ms / 1000)
    
    elapsed = time.time() - start_time
    rate = count / elapsed if elapsed > 0 else 0
    
    print(f"\n  {Colors.CYAN}━━━ Results ━━━{Colors.RESET}")
    print(f"  Sent: {success_count}/{count} ({success_count/count*100:.0f}%)")
    print(f"  Time: {elapsed:.2f}s ({rate:.1f} notifications/sec)")

def scenario_custom():
    """Send a custom notification."""
    print_header("CUSTOM NOTIFICATION", "─")
    
    try:
        sender = input(f"  {Colors.CYAN}Sender:{Colors.RESET} ").strip() or "Test App"
        message = input(f"  {Colors.CYAN}Message:{Colors.RESET} ").strip() or "Test message"
        
        print()
        send_with_log(sender, message)
    except KeyboardInterrupt:
        print("\n  Cancelled.")

def scenario_mixed_priority():
    """Send a mix of all priority levels."""
    print_header("MIXED PRIORITY TEST", "─")
    print_info("Random mix of all urgency levels\n")
    
    all_messages = []
    for presets in NOTIFICATION_PRESETS.values():
        all_messages.extend(presets)
    
    random.shuffle(all_messages)
    
    for sender, msg, urgency, cat in all_messages[:8]:
        send_with_log(sender, msg, urgency, cat)
        time.sleep(random.uniform(0.3, 0.8))

def run_all_scenarios():
    """Run all test scenarios in sequence."""
    print_header("RUNNING ALL SCENARIOS", "═")
    
    scenarios = [
        ("Social", scenario_social),
        ("Passive", scenario_passive),
        ("Noise", scenario_noise),
        ("Critical", scenario_critical),
        ("Split Texter", scenario_split_texter),
    ]
    
    for i, (name, func) in enumerate(scenarios, 1):
        print(f"\n{Colors.BOLD}[{i}/{len(scenarios)}] Running: {name}{Colors.RESET}")
        func()
        if i < len(scenarios):
            print_info("Waiting 3s before next scenario...")
            time.sleep(3)
    
    print_header("ALL SCENARIOS COMPLETE", "═")

# --- Main Menu ---

def show_menu():
    """Display interactive menu."""
    print(f"""
{Colors.CYAN}╔══════════════════════════════════════════════════╗
║{Colors.BOLD}       GLYPH-GLANCE NOTIFICATION TESTER           {Colors.CYAN}║
╚══════════════════════════════════════════════════╝{Colors.RESET}

  {Colors.BOLD}Test Scenarios:{Colors.RESET}
  {Colors.YELLOW}[1]{Colors.RESET} Split Texter (Buffer Engine)
  {Colors.YELLOW}[2]{Colors.RESET} Critical Notifications (Urgency 5-6)
  {Colors.YELLOW}[3]{Colors.RESET} Social Messages (Urgency 2-3)
  {Colors.YELLOW}[4]{Colors.RESET} Passive Updates (Urgency 1-2)
  {Colors.YELLOW}[5]{Colors.RESET} Noise Filter (Spam)
  {Colors.YELLOW}[6]{Colors.RESET} Urgency 6 Keywords (Instant Priority)
  {Colors.YELLOW}[7]{Colors.RESET} Mixed Priority Shuffle

  {Colors.BOLD}Advanced:{Colors.RESET}
  {Colors.YELLOW}[8]{Colors.RESET} Burst Test (Rapid Fire)
  {Colors.YELLOW}[9]{Colors.RESET} Custom Notification
  {Colors.YELLOW}[A]{Colors.RESET} Run All Scenarios

  {Colors.DIM}[Q] Quit{Colors.RESET}
""")

def main():
    parser = argparse.ArgumentParser(description="Glyph-Glance Notification Tester")
    parser.add_argument("--burst", type=int, metavar="N", help="Send N burst notifications")
    parser.add_argument("--delay", type=int, default=200, metavar="MS", help="Delay between burst notifications (ms)")
    parser.add_argument("--scenario", type=int, metavar="N", help="Run scenario N directly")
    parser.add_argument("--no-color", action="store_true", help="Disable colored output")
    args = parser.parse_args()
    
    if args.no_color:
        Colors.disable()
    
    print(f"\n{Colors.BOLD}Checking ADB connection...{Colors.RESET}")
    connected = check_adb_connection()
    
    if not connected:
        print_warning("Continuing anyway - commands will fail without device.")
    
    # Direct command execution
    if args.burst:
        scenario_burst(args.burst, args.delay)
        return
    
    if args.scenario:
        scenarios = {
            1: scenario_split_texter,
            2: scenario_critical,
            3: scenario_social,
            4: scenario_passive,
            5: scenario_noise,
            6: scenario_urgency_keywords,
            7: scenario_mixed_priority,
            8: lambda: scenario_burst(10, 200),
            9: scenario_custom,
            10: run_all_scenarios,
        }
        if args.scenario in scenarios:
            scenarios[args.scenario]()
        else:
            print_error(f"Invalid scenario: {args.scenario}")
        return
    
    # Interactive menu
    while True:
        show_menu()
        
        try:
            choice = input(f"  {Colors.CYAN}Select option:{Colors.RESET} ").strip().lower()
        except (KeyboardInterrupt, EOFError):
            print(f"\n\n{Colors.DIM}Goodbye!{Colors.RESET}\n")
            break
        
        actions = {
            '1': scenario_split_texter,
            '2': scenario_critical,
            '3': scenario_social,
            '4': scenario_passive,
            '5': scenario_noise,
            '6': scenario_urgency_keywords,
            '7': scenario_mixed_priority,
            '8': lambda: scenario_burst(
                int(input(f"  {Colors.CYAN}Count [10]:{Colors.RESET} ").strip() or 10),
                int(input(f"  {Colors.CYAN}Delay ms [200]:{Colors.RESET} ").strip() or 200)
            ),
            '9': scenario_custom,
            'a': run_all_scenarios,
            'q': None,
        }
        
        if choice == 'q':
            print(f"\n{Colors.DIM}Goodbye!{Colors.RESET}\n")
            break
        elif choice in actions:
            try:
                actions[choice]()
            except (KeyboardInterrupt, ValueError) as e:
                print(f"\n  {Colors.DIM}Cancelled or invalid input.{Colors.RESET}")
        else:
            print_error("Invalid option. Please try again.")
        
        input(f"\n  {Colors.DIM}Press Enter to continue...{Colors.RESET}")

if __name__ == "__main__":
    main()

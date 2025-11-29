import zipfile
import os

jar_path = r"c:\Users\user\glyph-glance\composeApp\libs\KetchumSDK_Community_20250805.jar"

if not os.path.exists(jar_path):
    print(f"Error: File not found at {jar_path}")
else:
    print(f"Reading {jar_path}...")
    try:
        with zipfile.ZipFile(jar_path, 'r') as z:
            print("\n--- Classes found in JAR ---")
            for name in z.namelist():
                if name.endswith(".class"):
                    print(name)
    except Exception as e:
        print(f"Error reading zip: {e}")

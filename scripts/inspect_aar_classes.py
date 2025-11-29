import zipfile
import os
import sys

aar_path = r"c:\Users\user\glyph-glance\composeApp\libs\glyph-matrix-sdk-1.0.aar"

if not os.path.exists(aar_path):
    print(f"Error: File not found at {aar_path}")
    sys.exit(1)

print(f"Inspecting {aar_path}...")

try:
    with zipfile.ZipFile(aar_path, 'r') as z:
        if "classes.jar" in z.namelist():
            print("Found classes.jar, extracting...")
            z.extract("classes.jar", "temp_inspect")
            
            jar_path = "temp_inspect/classes.jar"
            if os.path.exists(jar_path):
                print("Inspecting classes.jar...")
                with zipfile.ZipFile(jar_path, 'r') as jar:
                    print("\n--- CLASSES FOUND ---")
                    for name in jar.namelist():
                        if name.endswith(".class"):
                            print(name)
                    print("---------------------")
            else:
                print("Error: Extracted classes.jar not found")
        else:
            print("classes.jar not found in AAR")

except Exception as e:
    print(f"Error: {e}")

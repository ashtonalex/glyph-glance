import zipfile
import os
import sys

aar_path = r"c:\Users\user\glyph-glance\composeApp\libs\glyph-matrix-sdk-1.0.aar"

try:
    if not os.path.exists(aar_path):
        print(f"File not found: {aar_path}")
        sys.exit(1)

    with zipfile.ZipFile(aar_path, 'r') as zip_ref:
        print("Listing contents:")
        for name in zip_ref.namelist():
            print(name)
            if name == "classes.jar":
                # Extract classes.jar to temp
                zip_ref.extract(name, "temp_libs")
                
    classes_jar = "temp_libs/classes.jar"
    if os.path.exists(classes_jar):
        print("\n--- classes.jar contents ---")
        with zipfile.ZipFile(classes_jar, 'r') as jar_ref:
             for name in jar_ref.namelist():
                 if name.endswith(".class"):
                     print(name)
except Exception as e:
    print(f"Error: {e}")

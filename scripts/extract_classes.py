import zipfile
import os
import subprocess

# Use absolute paths
base_dir = os.getcwd()
aar_rel_path = r"composeApp\libs\glyph-matrix-sdk-1.0.aar"
aar_path = os.path.join(base_dir, aar_rel_path)
output_path = os.path.join(base_dir, "extracted_classes.jar")

print(f"Working directory: {base_dir}")

if os.path.exists(aar_path):
    try:
        with zipfile.ZipFile(aar_path, 'r') as zip_ref:
            if "classes.jar" in zip_ref.namelist():
                with zip_ref.open("classes.jar") as source, open(output_path, "wb") as target:
                    target.write(source.read())
                print(f"SUCCESS: Extracted classes.jar to {output_path}")
                
                # Now run javap
                print("\n--- Inspecting GlyphFrame$Builder ---")
                subprocess.run(["javap", "-cp", output_path, "-p", "com.nothing.ketchum.GlyphFrame$Builder"], shell=True)
                
            else:
                print("FAILURE: classes.jar not found inside AAR")
    except Exception as e:
        print(f"ERROR: {e}")
else:
    print(f"FAILURE: AAR file does not exist at {aar_path}")

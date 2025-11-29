import os
import subprocess
import zipfile

def inspect_jar():
    base_dir = os.getcwd()
    aar_path = os.path.join(base_dir, "composeApp", "libs", "glyph-matrix-sdk-1.0.aar")
    jar_path = os.path.join(base_dir, "temp_classes.jar")
    output_file = os.path.join(base_dir, "jar_inspection.txt")

    print(f"Inspecting {aar_path}...")

    # 1. Extract classes.jar from AAR
    if os.path.exists(aar_path):
        try:
            with zipfile.ZipFile(aar_path, 'r') as z:
                with z.open("classes.jar") as source, open(jar_path, "wb") as target:
                    target.write(source.read())
            print("Extracted classes.jar")
        except Exception as e:
            print(f"Failed to extract jar: {e}")
            return
    else:
        print("AAR not found")
        return

    # 2. Run javap
    try:
        # Inspect GlyphFrame.Builder
        cmd = f"javap -p -cp {jar_path} com.nothing.ketchum.GlyphFrame$Builder"
        print(f"Running: {cmd}")
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
        
        with open(output_file, "w") as f:
            f.write("=== GlyphFrame.Builder ===\n")
            f.write(result.stdout)
            f.write(result.stderr)
            
        # Inspect GlyphManager
        cmd2 = f"javap -p -cp {jar_path} com.nothing.ketchum.GlyphManager"
        print(f"Running: {cmd2}")
        result2 = subprocess.run(cmd2, shell=True, capture_output=True, text=True)
        
        with open(output_file, "a") as f:
            f.write("\n\n=== GlyphManager ===\n")
            f.write(result2.stdout)
            f.write(result2.stderr)

        print(f"Inspection written to {output_file}")
            
    except Exception as e:
        print(f"Error running javap: {e}")

if __name__ == "__main__":
    inspect_jar()

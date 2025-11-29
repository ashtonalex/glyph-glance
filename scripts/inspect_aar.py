import zipfile
import os

aar_path = r"c:\Users\user\glyph-glance\composeApp\libs\glyph-matrix-sdk-1.0.aar"

if os.path.exists(aar_path):
    print(f"File found: {aar_path}")
    try:
        with zipfile.ZipFile(aar_path, 'r') as zip_ref:
            # List all files in the AAR (which is a zip)
            file_list = zip_ref.namelist()
            print("--- Files in AAR ---")
            for f in file_list:
                print(f)
                
            # if classes.jar exists, we can try to inspect that too (but might need extraction)
            if "classes.jar" in file_list:
                print("\nFound classes.jar, but cannot inspect internal classes without extracting.")
                
    except Exception as e:
        print(f"Error reading AAR: {e}")
else:
    print(f"File not found: {aar_path}")

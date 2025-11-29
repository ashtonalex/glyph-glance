import zipfile
import os

aar_path = r"composeApp\libs\glyph-matrix-sdk-1.0.aar"
log_file = "aar_content.txt"

with open(log_file, "w") as log:
    if os.path.exists(aar_path):
        try:
            with zipfile.ZipFile(aar_path, 'r') as zip_ref:
                files = zip_ref.namelist()
                log.write(f"Files in {aar_path}:\n")
                for f in files:
                    log.write(f"{f}\n")
                    
                if "classes.jar" in files:
                    zip_ref.extract("classes.jar", ".")
                    log.write("\nExtracted classes.jar to current directory.\n")
        except Exception as e:
            log.write(f"Error: {e}\n")
    else:
        log.write(f"AAR not found at {aar_path}\n")

import zipfile
import os

aar_path = r"c:\Users\user\glyph-glance\composeApp\libs\glyph-matrix-sdk-1.0.aar"
temp_dir = r"c:\Users\user\glyph-glance\temp_inspect"

if not os.path.exists(temp_dir):
    os.makedirs(temp_dir)

try:
    with zipfile.ZipFile(aar_path, 'r') as z:
        z.extract("classes.jar", temp_dir)
    
    jar_path = os.path.join(temp_dir, "classes.jar")
    with zipfile.ZipFile(jar_path, 'r') as jar:
        for name in jar.namelist():
            if name.endswith(".class"):
                print(name)
except Exception as e:
    print(f"Error: {e}")

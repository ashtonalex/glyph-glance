import os
print("Script starting...")
jar_path = r"c:\Users\user\glyph-glance\composeApp\libs\KetchumSDK_Community_20250805.jar"
if os.path.exists(jar_path):
    print("File exists!")
    import zipfile
    try:
        z = zipfile.ZipFile(jar_path)
        print("Zip opened. Files:")
        for n in z.namelist():
            if n.endswith(".class"):
                print(n)
    except Exception as e:
        print(f"Error: {e}")
else:
    print("File NOT found")

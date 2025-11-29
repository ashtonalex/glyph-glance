import os
print("CWD:", os.getcwd())
target = r"composeApp\libs"
if os.path.exists(target):
    print("Listing", target)
    print(os.listdir(target))
else:
    print("Target not found:", target)

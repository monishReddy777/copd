import os
import shutil

base_dir = r"c:\Users\sandh\AndroidStudioProjects\cdss2\app\src\main\java\com\simats\cdss"
api_dir = os.path.join(base_dir, "api")
api_models_dir = os.path.join(api_dir, "models")

network_dir = os.path.join(base_dir, "network")
models_dir = os.path.join(base_dir, "models")

os.makedirs(network_dir, exist_ok=True)
os.makedirs(models_dir, exist_ok=True)

# Move files
if os.path.exists(os.path.join(api_dir, "RetrofitClient.java")):
    shutil.move(os.path.join(api_dir, "RetrofitClient.java"), os.path.join(network_dir, "RetrofitClient.java"))

if os.path.exists(os.path.join(api_dir, "ApiService.java")):
    shutil.move(os.path.join(api_dir, "ApiService.java"), os.path.join(network_dir, "ApiService.java"))

if os.path.exists(api_models_dir):
    for f in os.listdir(api_models_dir):
        if f.endswith(".java"):
            shutil.move(os.path.join(api_models_dir, f), os.path.join(models_dir, f))
    os.rmdir(api_models_dir)

if os.path.exists(api_dir):
    try:
        os.rmdir(api_dir)
    except:
        pass

# Replace in files
def replace_in_file(filepath):
    with open(filepath, "r", encoding="utf-8") as file:
        content = file.read()
    
    modified = content.replace("package com.simats.cdss.api.models;", "package com.simats.cdss.models;")
    modified = modified.replace("package com.simats.cdss.api;", "package com.simats.cdss.network;")
    modified = modified.replace("import com.simats.cdss.api.models.", "import com.simats.cdss.models.")
    modified = modified.replace("import com.simats.cdss.api.RetrofitClient;", "import com.simats.cdss.network.RetrofitClient;")
    modified = modified.replace("import com.simats.cdss.api.ApiService;", "import com.simats.cdss.network.ApiService;")
    
    if modified != content:
        with open(filepath, "w", encoding="utf-8") as file:
            file.write(modified)

# Traverse all java files in base_dir
for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith(".java"):
            replace_in_file(os.path.join(root, file))

print("Refactoring complete.")

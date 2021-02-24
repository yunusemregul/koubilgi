import requests
import json

validSubdomains = []

with open("subdomains.txt") as f:
    data = json.load(f)
    for site in data:
        try:
            request = requests.head("http://"+site+"/duyurular.php", timeout=5)
        except Exception:
            print("-" + site)
            continue

        if request.status_code == 200:
            validSubdomains.append(site)
            print("+" + site)
        else:
            print("-" + site)

with open("validSubdomains.txt", "w") as f:
    json.dump(validSubdomains, f, indent=2, sort_keys=True)
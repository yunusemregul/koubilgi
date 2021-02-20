import requests
import json
from lxml.html import fromstring

validSubdomainTitles = {}
with open("validSubdomains.txt") as f:
    data = json.load(f)
    for site in data:
        try:
            request = requests.get("http://"+site+"/duyurular.php", timeout=2)
        except Exception:
            print("-" + site)
            continue

        if request.status_code == 200:
            tree = fromstring(request.content)
            title = tree.findtext('.//title')
            if title:
                validSubdomainTitles["http://"+site+"/duyurular.php"] = title
                print("+" + site + " = " + title)
            else:
                print("-" + site + " ! TITLE NOT FOUND IN PAGE !")
        else:
            print("-" + site)

with open("departmentDuyurularUrlAndTitles.txt", "w") as f:
    json.dump(validSubdomainTitles, f, indent=2, sort_keys=True, ensure_ascii=False)
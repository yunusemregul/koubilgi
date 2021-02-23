import requests
import json
from lxml.html import fromstring
import re
import codecs

avoidTitles = ["başkanlığı", "merkezi", "laboratuvarı", "birimi", "kurulu", "birliği", "yös", "kültür evi", "eua full costing project"]
validSubdomainTitles = {}
with open("validSubdomains.txt") as f:
    data = json.load(f)
    for site in data:
        try:
            request = requests.get("http://" + site + "/duyurular.php", timeout=2)
        except Exception:
            print("-" + site)
            continue

        if request.status_code == 200:
            tree = fromstring(request.content)
            title = tree.findtext('.//title')
            if title:
                title = re.sub(r'KOÜ?-?', '', title)
                title = title.strip().lower()

                if any(x in title for x in avoidTitles):
                    print("-" + site + " * is not valid for a department")
                    continue

                validSubdomainTitles["http://" + site + "/duyurular.php"] = title
                print("+" + site + " = " + title)
            else:
                print("-" + site + " * not found on page")
        else:
            print("-" + site)

with codecs.open("departmentDuyurularUrlAndTitles.txt", 'w', encoding="utf-8") as f:
    json.dump(validSubdomainTitles, f, indent=2, sort_keys=True, ensure_ascii=False)

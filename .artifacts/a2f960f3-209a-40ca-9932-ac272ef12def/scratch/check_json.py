import json
import sys

try:
    with open(r'C:/AndroidAppFiles/furawark20260827/app/src/main/assets/furawalk_map2.json', 'r', encoding='utf-8') as f:
        json.load(f)
    print("VALID")
except Exception as e:
    print(f"INVALID: {e}")

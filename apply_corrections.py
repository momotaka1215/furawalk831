import json
import os
import glob

def apply_corrections():
    corrections_file = 'corrections.json'
    assets_dir = 'app/src/main/assets'

    if not os.path.exists(corrections_file):
        print(f"Error: {corrections_file} not found. Please save the device export to the project root.")
        return

    with open(corrections_file, 'r', encoding='utf-8') as f:
        corrections = json.load(f)

    correction_map = {c['id']: c for c in corrections}
    updated_count = 0

    # Search all furawalk_map*.json files
    json_files = glob.glob(os.path.join(assets_dir, 'furawalk_map*.json'))

    for file_path in json_files:
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)

        modified = False
        for item in data:
            if item['id'] in correction_map:
                corr = correction_map[item['id']]
                print(f"Updating {item['id']} ({item['name']}):")
                print(f"  Lat: {item['latitude']} -> {corr['latitude']}")
                print(f"  Lng: {item['longitude']} -> {corr['longitude']}")

                item['latitude'] = corr['latitude']
                item['longitude'] = corr['longitude']
                # Optionally update radius/priority if needed
                if 'radiusMeter' in corr: item['radiusMeter'] = corr['radiusMeter']
                if 'priority' in corr: item['priority'] = corr['priority']

                modified = True
                updated_count += 1

        if modified:
            with open(file_path, 'w', encoding='utf-8') as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            print(f"Saved changes to {os.path.basename(file_path)}")

    print(f"\nFinished. Total items updated: {updated_count}")

if __name__ == "__main__":
    apply_corrections()

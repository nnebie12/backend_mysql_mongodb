"""
Inspection rapide de ai_training_data/recipe_features_cleaned.csv
À lancer depuis ml-service/ — ne modifie rien.
"""
import os
import pandas as pd

CANDIDATES = [
    "ai_training_data/recipe_features_cleaned.csv",
    "recipe_features_cleaned.csv",
]

found = None
for path in CANDIDATES:
    if os.path.isfile(path):
        found = path
        break

if not found:
    print("❌ Fichier introuvable dans :")
    for c in CANDIDATES:
        print(f"   - {os.path.abspath(c)}")
    print("\nContenu de ai_training_data/ (si le dossier existe) :")
    if os.path.isdir("ai_training_data"):
        for f in os.listdir("ai_training_data"):
            print(f"   {f}")
    raise SystemExit(1)

print(f"✅ Trouvé : {os.path.abspath(found)}")
size = os.path.getsize(found)
print(f"   Taille : {size:,} octets")

df = pd.read_csv(found, sep=None, engine="python")
df.columns = [str(c).strip() for c in df.columns]
print(f"   Lignes : {len(df)}")
print(f"   Colonnes ({len(df.columns)}) : {list(df.columns)}")

id_col = next((c for c in df.columns if c.lower() == "id"), None)
print(f"\n   Colonne 'id' trouvée : {id_col}")
if id_col:
    sample = df[id_col].dropna().head(10).tolist()
    print(f"   10 premiers IDs : {sample}")
    print(f"   Min/Max : {df[id_col].min()} / {df[id_col].max()}")
    print(f"   Type de données : {df[id_col].dtype}")
else:
    print("   ⚠️ Aucune colonne nommée 'id' — voici les 5 premières lignes :")
    print(df.head())
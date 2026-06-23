"""
=============================================================================
  SCRIPT DE DIAGNOSTIC — pourquoi le CSV n'est pas lu et pourquoi
  les POST /interactions échouent silencieusement
  ─────────────────────────────────────────────────────────────────────────
  À lancer DEPUIS le dossier ml-service/ (là où sont les autres scripts).
  Ne modifie rien en base — lecture seule + 1 seul POST de test à la fin.
=============================================================================
"""

import os
import sys
import requests
import pandas as pd

API_BASE       = "http://localhost:8080/api/v1"
ADMIN_EMAIL    = "dianekassi@admin.com"
ADMIN_PASSWORD = "Mydayana48"
CSV_FILE       = "recettes_clean.csv"


def section(title):
    print(f"\n{'─'*65}\n  {title}\n{'─'*65}")


# ── 1. Où est-on, et le fichier est-il vraiment là ? ────────────────────────
section("1. Répertoire de travail et présence du fichier")
cwd = os.getcwd()
print(f"  Répertoire courant (cwd) : {cwd}")
print(f"  Contenu du dossier :")
for f in sorted(os.listdir(cwd)):
    marker = "  ← CSV CIBLE" if f == CSV_FILE else ""
    print(f"    {f}{marker}")

csv_path = os.path.join(cwd, CSV_FILE)
exists = os.path.isfile(csv_path)
print(f"\n  os.path.isfile('{CSV_FILE}') = {exists}")

if not exists:
    print(f"\n  ❌ PROBLÈME CONFIRMÉ : le fichier n'est pas visible depuis cwd actuel.")
    print(f"     → Lancez ce script (et purge_and_reset_interactions.py) avec :")
    print(f"       cd /chemin/vers/ml-service && python diagnostic.py")
    sys.exit(1)
else:
    print(f"  ✅ Fichier trouvé.")

# ── 2. Taille et premières lignes brutes (avant tout parsing pandas) ───────
section("2. Contenu brut du fichier (avant parsing)")
size = os.path.getsize(csv_path)
print(f"  Taille du fichier : {size:,} octets")

if size == 0:
    print("  ❌ PROBLÈME CONFIRMÉ : le fichier est VIDE (0 octet).")
    sys.exit(1)

with open(csv_path, "rb") as f:
    raw_start = f.read(300)
print(f"  300 premiers octets bruts (repr) :\n  {raw_start!r}")

with open(csv_path, "r", encoding="utf-8", errors="replace") as f:
    first_lines = [f.readline() for _ in range(3)]
print(f"\n  3 premières lignes (texte) :")
for i, line in enumerate(first_lines):
    print(f"    [{i}] {line.rstrip()[:150]}")

# ── 3. Essai de parsing pandas avec plusieurs stratégies ────────────────────
section("3. Tentatives de parsing pandas")

strategies = [
    ("sep=None engine=python (méthode actuelle du script)",
     lambda: pd.read_csv(csv_path, sep=None, engine="python")),
    ("sep=',' standard",
     lambda: pd.read_csv(csv_path, sep=",")),
    ("sep=';'",
     lambda: pd.read_csv(csv_path, sep=";")),
    ("sep='\\t' (tabulation)",
     lambda: pd.read_csv(csv_path, sep="\t")),
]

df = None
working_strategy = None

for name, fn in strategies:
    try:
        candidate = fn()
        n_cols = len(candidate.columns)
        n_rows = len(candidate)
        print(f"  • {name}")
        print(f"      → {n_rows} lignes, {n_cols} colonnes")
        print(f"      → Colonnes : {list(candidate.columns)[:8]}"
              f"{'...' if n_cols > 8 else ''}")
        if n_rows > 0 and n_cols > 1 and df is None:
            df = candidate
            working_strategy = name
    except Exception as e:
        print(f"  • {name}")
        print(f"      ❌ Échec : {e}")

if df is None:
    print(f"\n  ❌ PROBLÈME CONFIRMÉ : aucune stratégie de parsing pandas ne")
    print(f"     produit un DataFrame valide (>0 lignes, >1 colonne).")
    print(f"     Le fichier existe mais son format n'est pas un CSV standard.")
    sys.exit(1)
else:
    print(f"\n  ✅ Stratégie qui fonctionne : {working_strategy}")

# ── 4. Détection de la colonne ID (logique du script original) ─────────────
section("4. Détection de la colonne ID")
df.columns = [str(c).strip() for c in df.columns]
print(f"  Colonnes (nettoyées) : {list(df.columns)}")

id_col = None
for col in df.columns:
    if col.lower() == "id":
        sample = df[col].dropna().head(5).tolist()
        print(f"  Colonne candidate '{col}' — échantillon : {sample}")
        try:
            ok = all(int(float(str(v))) > 0 for v in sample)
            print(f"    → tous convertibles en int positif ? {ok}")
            if ok:
                id_col = col
        except Exception as e:
            print(f"    → échec conversion : {e}")

if id_col:
    print(f"\n  ✅ Colonne ID détectée : '{id_col}'")
    recipe_ids = [int(float(v)) for v in df[id_col].dropna().tolist()]
    print(f"     {len(recipe_ids)} IDs valides, exemples : {recipe_ids[:10]}")
else:
    print(f"\n  ⚠️  Aucune colonne nommée exactement 'id' (insensible casse) trouvée.")
    print(f"     Colonnes disponibles : {list(df.columns)}")
    recipe_ids = []

# ── 5. Login + test direct de l'endpoint /interactions ─────────────────────
section("5. Test direct de l'API /interactions")

sess = requests.Session()
try:
    r = sess.post(
        f"{API_BASE}/auth/login",
        json={"email": ADMIN_EMAIL, "motDePasse": ADMIN_PASSWORD},
        timeout=10,
    )
    r.raise_for_status()
    token = r.json().get("token")
    sess.headers.update({"Authorization": f"Bearer {token}"})
    print(f"  ✅ Login OK, token reçu ({len(token)} caractères)")
except Exception as e:
    print(f"  ❌ Login échoué : {e}")
    sys.exit(1)

# Vérifier qu'au moins une recette existe vraiment côté API
test_recipe_id = recipe_ids[0] if recipe_ids else 1
print(f"\n  Vérification recette id={test_recipe_id} via GET /recettes/{{id}} :")
try:
    r = sess.get(f"{API_BASE}/recettes/{test_recipe_id}", timeout=10)
    print(f"    HTTP {r.status_code}")
    if r.status_code == 200:
        print(f"    Titre : {r.json().get('titre', 'N/A')}")
    else:
        print(f"    Corps : {r.text[:200]}")
except Exception as e:
    print(f"    ❌ Erreur : {e}")

# Vérifier la liste réelle des recettes en base (vérité terrain)
print(f"\n  Récupération de la liste réelle des recettes via GET /recettes/all :")
try:
    r = sess.get(f"{API_BASE}/recettes/all", timeout=15)
    print(f"    HTTP {r.status_code}")
    if r.status_code == 200:
        real_recipes = r.json()
        real_ids = [rec.get("id") for rec in real_recipes if rec.get("id")]
        print(f"    {len(real_ids)} recettes réelles en base")
        print(f"    Exemples d'IDs réels : {real_ids[:10]}")

        if recipe_ids:
            overlap = set(recipe_ids) & set(real_ids)
            print(f"\n    🔎 Intersection CSV ∩ base réelle : {len(overlap)} IDs communs")
            if len(overlap) == 0:
                print(f"    ❌ PROBLÈME TROUVÉ : les IDs du CSV ne correspondent à")
                print(f"       AUCUNE recette réelle en base ! C'est pour ça que")
                print(f"       toutes les requêtes POST échouaient (entiteId invalide).")
    else:
        print(f"    Corps : {r.text[:200]}")
        real_ids = []
except Exception as e:
    print(f"    ❌ Erreur : {e}")
    real_ids = []

# ── 6. Test d'un VRAI POST avec un ID garanti valide ────────────────────────
section("6. Test d'un POST /interactions avec un ID garanti valide")

if real_ids:
    test_id = real_ids[0]
    test_user = 1173  # premier user du fallback historique

    params = {
        "userId":            test_user,
        "typeInteraction":   "CONSULTATION",
        "entiteId":          test_id,
        "dureeConsultation": 120,
    }
    print(f"  POST /interactions avec params={params}")
    try:
        r = sess.post(f"{API_BASE}/interactions", params=params, timeout=10)
        print(f"    HTTP {r.status_code}")
        print(f"    Corps réponse : {r.text[:300]}")
        if r.status_code in (200, 201):
            print(f"\n  ✅ Le POST fonctionne avec un ID réel de la base.")
        else:
            print(f"\n  ❌ Le POST échoue même avec un ID réel — voir le corps")
            print(f"     de la réponse ci-dessus pour la cause exacte (validation,")
            print(f"     contrainte FK, userId inexistant, etc.)")
    except Exception as e:
        print(f"    ❌ Exception réseau : {e}")
else:
    print("  ⏭️  Pas d'ID réel disponible pour tester (voir section 5).")

section("DIAGNOSTIC TERMINÉ")
print("""
  Résumé à retenir :
  - Si section 1 a échoué (FileNotFoundError-like) → mauvais cwd au lancement.
  - Si section 3 a échoué → mauvais séparateur CSV, à corriger dans pandas.
  - Si section 4 a échoué → la colonne ID dans le CSV n'a pas le bon nom.
  - Si section 5 montre overlap = 0 → le CSV référence des recettes qui
    n'existent PAS dans VOTRE base MySQL actuelle (peut-être un CSV d'export
    d'une autre base, ou la table a été réinitialisée depuis).
  - Si section 6 échoue même avec un ID réel → problème côté Spring Boot
    (contrôleur InteractionUtilisateurController, validation, ou la route
    elle-même a changé de signature).
""")
"""
=============================================================================
  DIAGNOSTIC — État réel des commentaires en base
  ─────────────────────────────────────────────────────────────────────────
  Les commentaires existent à DEUX endroits distincts dans le code :

    1. CommentaireDocument (collection MongoDB séparée "commentaireDocument")
       via CommentaireController : GET /api/v1/commentaires/all
       → c'est ce que generate_all_data.py alimentait avec generate_commentaires()

    2. RecetteDetailsDocument.commentaires (liste IMBRIQUÉE dans chaque
       document de détails de recette, collection "recette_details")
       via RecetteController : GET /api/v1/recettes/{id}/commentaires
       → alimenté par RecetteServiceImpl.addCommentaire()

  Ces deux structures sont INDÉPENDANTES : un commentaire ajouté via l'une
  ne se retrouve PAS automatiquement dans l'autre. C'est une incohérence
  de design pré-existante (pas créée par nos scripts), mais qu'il vaut
  mieux objectiver avant de décider quoi corriger.

  Ce script ne modifie rien — lecture seule.
  Prérequis : Spring Boot sur http://localhost:8080
=============================================================================
"""

import requests
from collections import defaultdict

API_BASE       = "http://localhost:8080/api/v1"
ADMIN_EMAIL    = "dianekassi@admin.com"
ADMIN_PASSWORD = "Mydayana48"


def section(title):
    print(f"\n{'─'*65}\n  {title}\n{'─'*65}")


sess = requests.Session()

# ── Login ────────────────────────────────────────────────────────────────
section("0. Connexion")
try:
    r = sess.post(
        f"{API_BASE}/auth/login",
        json={"email": ADMIN_EMAIL, "motDePasse": ADMIN_PASSWORD},
        timeout=10,
    )
    r.raise_for_status()
    token = r.json().get("token")
    sess.headers.update({"Authorization": f"Bearer {token}"})
    print(f"  ✅ Connecté : {ADMIN_EMAIL}")
except Exception as e:
    print(f"  ❌ Login échoué : {e}")
    raise SystemExit(1)

# ── 1. CommentaireDocument (collection séparée) ────────────────────────────
section("1. CommentaireDocument — GET /api/v1/commentaires/all")
try:
    r = sess.get(f"{API_BASE}/commentaires/all", timeout=30)
    print(f"  HTTP {r.status_code}")
    if r.status_code == 200:
        comments = r.json()
        print(f"  Total : {len(comments):,} commentaires")

        if comments:
            sample = comments[0]
            print(f"\n  Exemple de structure (1er élément) :")
            for k, v in sample.items():
                v_str = str(v)[:80]
                print(f"    {k:<20} = {v_str}")

            # Répartition par recette
            by_recette = defaultdict(int)
            for c in comments:
                by_recette[c.get("recetteId") or "INCONNU"] += 1
            print(f"\n  Réparti sur {len(by_recette)} recettes distinctes")
            top5 = sorted(by_recette.items(), key=lambda x: -x[1])[:5]
            print(f"  Top 5 recettes les plus commentées :")
            for rid, count in top5:
                print(f"    recetteId={rid} → {count} commentaires")

            # Vérifier les userId vides/null
            n_user_null = sum(1 for c in comments if not c.get("userId"))
            n_userName_anon = sum(1 for c in comments if c.get("userName") == "Anonymous")
            print(f"\n  userId manquant/null : {n_user_null}/{len(comments)}")
            print(f"  userName = 'Anonymous' : {n_userName_anon}/{len(comments)}")
    else:
        print(f"  Corps : {r.text[:300]}")
        comments = []
except Exception as e:
    print(f"  ❌ Erreur : {e}")
    comments = []

# ── 2. RecetteDetailsDocument.commentaires (imbriqué) ──────────────────────
section("2. RecetteDetailsDocument.commentaires — GET /recettes/{id}/commentaires")
try:
    r = sess.get(f"{API_BASE}/recettes/all", timeout=30)
    recipes = r.json() if r.status_code == 200 else []
    print(f"  {len(recipes)} recettes trouvées via /recettes/all")
except Exception as e:
    print(f"  ❌ Erreur récupération recettes : {e}")
    recipes = []

if recipes:
    sample_size = min(30, len(recipes))
    print(f"  Échantillonnage sur {sample_size} recettes (sur {len(recipes)} au total)...")

    total_nested_comments = 0
    recipes_with_comments = 0
    errors = 0

    import random
    sample_recipes = random.sample(recipes, sample_size)

    for rec in sample_recipes:
        rid = rec.get("id")
        if not rid:
            continue
        try:
            r = sess.get(f"{API_BASE}/recettes/{rid}/commentaires", timeout=10)
            if r.status_code == 200:
                nested = r.json()
                if nested:
                    recipes_with_comments += 1
                    total_nested_comments += len(nested)
            else:
                errors += 1
        except Exception:
            errors += 1

    print(f"\n  Sur l'échantillon de {sample_size} recettes :")
    print(f"    Recettes avec ≥1 commentaire imbriqué : {recipes_with_comments}")
    print(f"    Total commentaires imbriqués trouvés  : {total_nested_comments}")
    print(f"    Erreurs de requête                    : {errors}")

    if sample_size > 0:
        moyenne_estimee = total_nested_comments / sample_size
        estimation_totale = round(moyenne_estimee * len(recipes))
        print(f"\n  📊 Estimation extrapolée sur les {len(recipes)} recettes totales :")
        print(f"     ≈ {estimation_totale:,} commentaires imbriqués (estimation)")

# ── 3. Comparaison et verdict ────────────────────────────────────────────
section("3. Verdict")
print(f"""
  CommentaireDocument (collection séparée)     : {len(comments):,} commentaires réels (compte exact)
  RecetteDetailsDocument.commentaires (imbriqué) : voir estimation ci-dessus (échantillon)

  Ces deux nombres sont probablement très différents car ce sont deux
  systèmes de stockage indépendants, alimentés par des routes différentes :
    - POST /commentaires                          → CommentaireDocument
    - POST /recettes/{{id}}/commentaires/user/{{uid}} → RecetteDetailsDocument (imbriqué)

  Aucune cible chiffrée n'existe dans le dossier PFE pour les commentaires,
  donc il n'y a pas d'"écart à corriger" au sens strict. La question est
  plutôt : voulez-vous que ces données soient cohérentes/présentes pour
  une démo en soutenance (ex: si on clique sur une recette et qu'on
  s'attend à voir des commentaires affichés) ?
""")
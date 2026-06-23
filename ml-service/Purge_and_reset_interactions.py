"""
=============================================================================
  SCRIPT 0 — PURGE & RESET DES INTERACTIONS (à lancer EN PREMIER)
  ─────────────────────────────────────────────────────────────────────────
  Contexte : la base contient 87 961+ interactions accumulées par
             d'anciens scripts (generate_all_data.py, generate_behavior.py
             exécutés avant ce nettoyage). La cible du dossier PFE est
             18 670. Ce script vide la collection MongoDB `interactions`
             puis regénère exactement 18 670 entrées propres.

  ⚠️  DESTRUCTIF : supprime TOUTES les interactions existantes.
      Une confirmation explicite est demandée avant suppression.
      Faites un mongodump avant si vous n'êtes pas sûr.

  Méthode de purge : deux options selon ce qui est accessible :
    A. Suppression directe via pymongo (rapide, recommandé si Mongo
       est accessible en local sans authentification complexe)
    B. Suppression via l'API, utilisateur par utilisateur, en
       utilisant DELETE /api/v1/interactions/user/{userId}
       (plus lent, mais ne nécessite que l'API Spring Boot)

  Ce script essaie A en premier, et bascule automatiquement sur B
  si pymongo n'est pas installé ou si la connexion échoue.

  Prérequis : Spring Boot sur http://localhost:8080
=============================================================================
"""

import sys
import time
import random
import requests
from datetime import datetime, timedelta, timezone
from collections import defaultdict

# ─── CONFIG ────────────────────────────────────────────────────────────────
API_BASE            = "http://localhost:8080/api/v1"
ADMIN_EMAIL         = "dianekassi@admin.com"
ADMIN_PASSWORD      = "Mydayana48"
TARGET_INTERACTIONS = 18_670
CSV_FILE            = "recettes_clean.csv"  # ⚠️ non utilisé depuis le correctif :
                                              #    les IDs viennent maintenant de l'API
DELAY               = 0.03

# Connexion Mongo directe (option A) — adaptez si besoin
MONGO_URI       = "mongodb://localhost:27017"   # ⚠️ adaptez si Mongo Atlas / auth
MONGO_DB        = "ProjetRecette"
MONGO_COLLECTION = "interactionUtilisateur"      # nom de la collection Spring Data (vérifiez le @Document)

INTERACTION_DISTRIBUTION = [
    ("CONSULTATION",   0.60),
    ("NOTE_POSEE",     0.20),
    ("FAVORI_AJOUTE",  0.15),
    ("PARTAGE",        0.05),
]


def confirm(message: str) -> bool:
    rep = input(f"\n{message} [tapez EXACTEMENT 'CONFIRMER' pour continuer] : ").strip()
    return rep == "CONFIRMER"


def random_past_date(days_back=180) -> str:
    raw   = random.expovariate(0.015)
    days  = min(int(raw), days_back)
    delta = timedelta(days=days, hours=random.randint(0, 23), minutes=random.randint(0, 59))
    return (datetime.now(timezone.utc) - delta).isoformat()


def pick_interaction_type() -> str:
    rand, cumul = random.random(), 0.0
    for itype, prob in INTERACTION_DISTRIBUTION:
        cumul += prob
        if rand < cumul:
            return itype
    return "CONSULTATION"


# ─── PURGE OPTION A — pymongo direct ───────────────────────────────────────
def purge_via_pymongo() -> bool:
    try:
        from pymongo import MongoClient
    except ImportError:
        print("ℹ️  pymongo non installé (pip install pymongo --break-system-packages)")
        return False

    try:
        client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=3000)
        client.server_info()  # force la connexion pour vérifier qu'elle marche
        db = client[MONGO_DB]

        # Découverte du nom réel de la collection si besoin
        existing_collections = db.list_collection_names()
        candidates = [c for c in existing_collections
                      if "interaction" in c.lower()]
        print(f"📂 Collections trouvées contenant 'interaction' : {candidates}")

        target_coll = MONGO_COLLECTION if MONGO_COLLECTION in existing_collections \
                      else (candidates[0] if candidates else None)

        if not target_coll:
            print("⚠️  Aucune collection 'interaction*' trouvée via pymongo.")
            return False

        count_before = db[target_coll].count_documents({})
        print(f"📊 Collection '{target_coll}' : {count_before:,} documents")

        if not confirm(f"Supprimer les {count_before:,} documents de '{target_coll}' ?"):
            print("⏹️  Purge annulée par l'utilisateur.")
            return False

        result = db[target_coll].delete_many({})
        print(f"🗑️  {result.deleted_count:,} documents supprimés.")
        client.close()
        return True

    except Exception as e:
        print(f"⚠️  Connexion pymongo échouée : {e}")
        return False


# ─── PURGE OPTION B — via API, user par user ───────────────────────────────
class ApiPurger:
    def __init__(self):
        self.sess = requests.Session()

    def login(self) -> bool:
        try:
            r = self.sess.post(
                f"{API_BASE}/auth/login",
                json={"email": ADMIN_EMAIL, "motDePasse": ADMIN_PASSWORD},
                timeout=10,
            )
            r.raise_for_status()
            token = r.json().get("token")
            if token:
                self.sess.headers.update({"Authorization": f"Bearer {token}"})
                print(f"✅ Connecté : {ADMIN_EMAIL}")
                return True
        except Exception as e:
            print(f"❌ Login : {e}")
        return False

    def get_all_user_ids_with_interactions(self) -> list:
        """Récupère tous les userId distincts ayant des interactions."""
        try:
            r = self.sess.get(f"{API_BASE}/interactions/all", timeout=60)
            if r.status_code == 200:
                data = r.json()
                ids = sorted({i.get("userId") for i in data if i.get("userId")})
                print(f"📊 {len(data):,} interactions trouvées sur {len(ids)} utilisateurs")
                return ids
        except Exception as e:
            print(f"⚠️  Erreur récupération : {e}")
        return []

    def purge_via_api(self) -> bool:
        user_ids = self.get_all_user_ids_with_interactions()
        if not user_ids:
            print("ℹ️  Aucune interaction à supprimer (ou endpoint inaccessible).")
            return True

        if not confirm(f"Supprimer les interactions de {len(user_ids)} utilisateurs via l'API ?"):
            print("⏹️  Purge annulée par l'utilisateur.")
            return False

        deleted_users = 0
        for idx, uid in enumerate(user_ids, 1):
            try:
                r = self.sess.delete(f"{API_BASE}/interactions/user/{uid}", timeout=10)
                if r.status_code in (200, 204):
                    deleted_users += 1
                time.sleep(DELAY)
            except Exception:
                pass

            if idx % 50 == 0 or idx == len(user_ids):
                print(f"  [{idx}/{len(user_ids)}] purgés", end="\r")

        print(f"\n🗑️  Interactions supprimées pour {deleted_users}/{len(user_ids)} utilisateurs")
        return True


# ─── REGÉNÉRATION PROPRE ────────────────────────────────────────────────────
class CleanRegenerator:
    def __init__(self, sess: requests.Session):
        self.sess       = sess
        self.recipe_ids = []
        self.user_ids   = []
        self.stats      = defaultdict(int)

    def load_data(self):
        """
        CORRECTIF : on charge désormais les IDs de recettes directement
        depuis l'API (/recettes/all), qui est la source de vérité.
        Le CSV recettes_clean.csv n'a PAS de colonne 'id' (c'est un export
        brut de scraping Marmiton, antérieur à l'insertion en base) —
        confirmé par diagnostic.py. On ne s'appuie donc plus sur un CSV
        pour les IDs, ce qui élimine ce problème définitivement, quel que
        soit le fichier CSV présent dans le dossier (recettes_clean.csv,
        recipe_features_cleaned.csv, ou autre).
        """
        try:
            r = self.sess.get(f"{API_BASE}/recettes/all", timeout=30)
            if r.status_code == 200:
                recipes = r.json()
                self.recipe_ids = [rec["id"] for rec in recipes if rec.get("id")]
                print(f"📖 {len(self.recipe_ids)} recettes réelles chargées depuis l'API")
        except Exception as e:
            print(f"⚠️  Erreur chargement recettes via API : {e}")

        if not self.recipe_ids:
            print("❌ Aucune recette récupérée via l'API. Impossible de continuer")
            print("   sans IDs valides — vérifiez que GET /recettes/all répond.")

        try:
            r = self.sess.get(f"{API_BASE}/users", timeout=15)
            if r.status_code == 200:
                self.user_ids = [int(u["id"]) for u in r.json() if u.get("id")]
                print(f"👥 {len(self.user_ids)} utilisateurs chargés depuis l'API")
        except Exception:
            pass

        if not self.user_ids:
            print("⚠️  Fallback : plage utilisateurs 1173–2041")
            self.user_ids = list(range(1173, 2042))

    def post_interaction(self, user_id: int, recipe_id: int, itype: str) -> bool:
        """
        CORRECTIF : le contrôleur Spring (InteractionUtilisateurController)
        attend un paramètre nommé EXACTEMENT 'recetteId', pas 'entiteId'.
        Confirmé par la stack trace serveur :
          MissingServletRequestParameterException: Required request
          parameter 'recetteId' for method parameter type Long is not present
        L'ancien code envoyait 'entiteId' → Spring renvoyait une erreur
        (mal traduite en 500 par le GlobalExceptionHandler générique au
        lieu d'un 400), et toutes les requêtes échouaient silencieusement.
        """
        params = {"userId": user_id, "typeInteraction": itype, "recetteId": recipe_id}
        if itype == "CONSULTATION":
            params["dureeConsultation"] = random.randint(15, 420)
        try:
            r = self.sess.post(f"{API_BASE}/interactions", params=params, timeout=10)
            time.sleep(DELAY)
            return r.status_code in (200, 201)
        except Exception:
            return False

    def run(self, target: int):
        print(f"\n🎯 Régénération propre : {target:,} interactions")

        if not self.recipe_ids:
            print("❌ Impossible de régénérer : aucune recette disponible.")
            print("   load_data() n'a pas réussi à charger d'IDs via l'API.")
            return
        if not self.user_ids:
            print("❌ Impossible de régénérer : aucun utilisateur disponible.")
            return

        n_pop = max(5, len(self.recipe_ids) // 10)
        popular = random.sample(self.recipe_ids, n_pop)

        generated = 0
        for i in range(target):
            user_id   = random.choice(self.user_ids)
            recipe_id = random.choice(popular) if random.random() < 0.35 \
                        else random.choice(self.recipe_ids)
            itype     = pick_interaction_type()

            if self.post_interaction(user_id, recipe_id, itype):
                generated += 1
                self.stats[itype] += 1

            if (i + 1) % 500 == 0 or (i + 1) == target:
                pct = (i + 1) / target * 100
                print(f"  [{i+1:,}/{target:,}] {pct:.1f}% — générées : {generated:,}", end="\r")

        print(f"\n\n✅ {generated:,} interactions générées proprement.")
        print("Répartition :")
        for k, v in self.stats.items():
            pct = v / generated * 100 if generated else 0
            print(f"   • {k:<20} → {v:>6,}  ({pct:.1f}%)")


# ─── ORCHESTRATION ──────────────────────────────────────────────────────────
def main():
    print("\n" + "═" * 65)
    print("  🧹  PURGE & RESET DES INTERACTIONS")
    print("═" * 65)
    print(f"""
  Cible finale : {TARGET_INTERACTIONS:,} interactions propres

  ⚠️  Ce script va SUPPRIMER toutes les interactions existantes
      puis en regénérer {TARGET_INTERACTIONS:,} depuis zéro.

  Une confirmation explicite sera demandée avant toute suppression.
""")

    purger = ApiPurger()
    if not purger.login():
        print("❌ Connexion impossible. Arrêt.")
        sys.exit(1)

    # Tentative purge directe Mongo, sinon fallback API
    print("\n── Étape 1/2 : Purge ──")
    purged = purge_via_pymongo()
    if not purged:
        print("\n↪️  Bascule sur la purge via API (plus lente mais fiable)...")
        purged = purger.purge_via_api()

    if not purged:
        print("\n⏹️  Purge non effectuée. Arrêt (rien n'a été régénéré).")
        sys.exit(1)

    # Vérification post-purge
    remaining = purger.get_all_user_ids_with_interactions()
    if remaining:
        print(f"\n⚠️  Attention : il reste des interactions sur {len(remaining)} utilisateurs.")
        if not confirm("Continuer malgré tout vers la régénération ?"):
            sys.exit(1)
    else:
        print("\n✅ Purge confirmée : 0 interaction résiduelle détectée via l'API.")

    print("\n── Étape 2/2 : Régénération propre ──")
    regen = CleanRegenerator(purger.sess)
    regen.load_data()
    regen.run(TARGET_INTERACTIONS)

    print("\n" + "═" * 65)
    print("  🏁  PURGE + RESET TERMINÉS")
    print("═" * 65)
    print(f"\n  Relancez verify_coherence_final.py pour confirmer le total exact.")


if __name__ == "__main__":
    main()
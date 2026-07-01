"""
=============================================================================
  SCRIPT 0 (v2) — PURGE & RESET DES INTERACTIONS (endpoint utilisateurs corrigé)
  ─────────────────────────────────────────────────────────────────────────
  CORRECTIF v2 : la v1 utilisait GET /api/v1/users pour charger la liste
  des utilisateurs, mais cet endpoint exige hasRole('ADMIN') strictement
  (cf. commentaire dans Generate_nested_comments.py : le compte admin a le
  rôle ADMINISTRATEUR, pas ADMIN — Role.java ne définit même pas de valeur
  ADMIN). Résultat : /users renvoie systématiquement 403, et le script
  retombait sur un fallback arbitraire (plage 1173–2041, 869 IDs) qui ne
  correspond PAS exactement aux 797 vrais utilisateurs. Une partie des
  interactions générées visait donc des userId inexistants, diluant
  fortement le volume réel par utilisateur — ce qui explique un score
  d'engagement anormalement bas après recalcul (99% DEBUTANT malgré
  18 670 interactions "générées").

  Ce script utilise désormais GET /api/administrateur/users (protégé par
  hasAnyRole('ADMIN', 'ADMINISTRATEUR'), qui accepte le rôle réel du
  compte), exactement comme recalculer_profils.py — garantissant que
  toutes les interactions ciblent bien les 797 vrais comptes.

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
ADMIN_API_BASE       = "http://localhost:8080/api/administrateur"
ADMIN_EMAIL         = "dianekassi@admin.com"
ADMIN_PASSWORD      = "Mydayana48"
TARGET_INTERACTIONS = 18_670
DELAY               = 0.03

MONGO_URI        = "mongodb://localhost:27017"
MONGO_DB         = "ProjetRecette"
MONGO_COLLECTION = "interactionUtilisateur"

INTERACTION_DISTRIBUTION = [
    ("CONSULTATION",   0.60),
    ("NOTE_POSEE",     0.20),
    ("FAVORI_AJOUTE",  0.15),
    ("PARTAGE",        0.05),
]


def confirm(message: str) -> bool:
    rep = input(f"\n{message} [tapez EXACTEMENT 'CONFIRMER' pour continuer] : ").strip()
    return rep == "CONFIRMER"


def pick_interaction_type() -> str:
    rand, cumul = random.random(), 0.0
    for itype, prob in INTERACTION_DISTRIBUTION:
        cumul += prob
        if rand < cumul:
            return itype
    return "CONSULTATION"


def purge_via_pymongo() -> bool:
    try:
        from pymongo import MongoClient
    except ImportError:
        print("ℹ️  pymongo non installé (pip install pymongo --break-system-packages)")
        return False

    try:
        client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=3000)
        client.server_info()
        db = client[MONGO_DB]

        existing_collections = db.list_collection_names()
        candidates = [c for c in existing_collections if "interaction" in c.lower()]
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


class CleanRegenerator:
    def __init__(self, sess: requests.Session):
        self.sess       = sess
        self.recipe_ids = []
        self.user_ids   = []
        self.stats      = defaultdict(int)

    def load_data(self):
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

        # ─────────────────────────────────────────────────────────────
        # ✅ CORRECTIF v2 : /api/v1/users exige hasRole('ADMIN') que ce
        # compte n'a pas (il a ADMINISTRATEUR). On utilise directement
        # /api/administrateur/users, qui accepte hasAnyRole('ADMIN',
        # 'ADMINISTRATEUR') — le MÊME endpoint que recalculer_profils.py,
        # garantissant que les deux scripts ciblent EXACTEMENT la même
        # liste de 797 vrais utilisateurs, sans fallback approximatif.
        # ─────────────────────────────────────────────────────────────
        try:
            r = self.sess.get(f"{ADMIN_API_BASE}/users", timeout=15)
            if r.status_code == 200:
                data = r.json()
                self.user_ids = [int(u["id"]) for u in data if u.get("id")]
                print(f"👥 {len(self.user_ids)} utilisateurs réels chargés "
                      f"depuis /api/administrateur/users")
            else:
                print(f"⚠️  GET /api/administrateur/users → HTTP {r.status_code}")
        except Exception as e:
            print(f"⚠️  Erreur chargement utilisateurs : {e}")

        if not self.user_ids:
            print("❌ Impossible de charger la vraie liste d'utilisateurs.")
            print("   Arrêt plutôt que d'utiliser un fallback approximatif")
            print("   qui fausserait les statistiques (cf. bug précédent).")

    def post_interaction(self, user_id: int, recipe_id: int, itype: str) -> bool:
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
            return
        if not self.user_ids:
            print("❌ Impossible de régénérer : aucun utilisateur réel disponible.")
            print("   (Le script s'arrête plutôt que d'utiliser une plage")
            print("    arbitraire qui polluerait les statistiques.)")
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
        print(f"   Réparties sur {len(self.user_ids)} vrais utilisateurs "
              f"→ moyenne {generated / len(self.user_ids):.1f} interactions/utilisateur")
        print("Répartition :")
        for k, v in self.stats.items():
            pct = v / generated * 100 if generated else 0
            print(f"   • {k:<20} → {v:>6,}  ({pct:.1f}%)")


def main():
    print("\n" + "═" * 65)
    print("  🧹  PURGE & RESET DES INTERACTIONS (v2 — endpoint utilisateurs corrigé)")
    print("═" * 65)
    print(f"""
  Cible finale : {TARGET_INTERACTIONS:,} interactions propres,
  réparties sur les VRAIS utilisateurs (via /api/administrateur/users).

  ⚠️  Ce script va SUPPRIMER toutes les interactions existantes
      puis en regénérer {TARGET_INTERACTIONS:,} depuis zéro.
""")

    purger = ApiPurger()
    if not purger.login():
        print("❌ Connexion impossible. Arrêt.")
        sys.exit(1)

    print("\n── Étape 1/2 : Purge ──")
    purged = purge_via_pymongo()
    if not purged:
        print("\n↪️  Bascule sur la purge via API (plus lente mais fiable)...")
        purged = purger.purge_via_api()

    if not purged:
        print("\n⏹️  Purge non effectuée. Arrêt (rien n'a été régénéré).")
        sys.exit(1)

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
    print(f"\n  👉 Lancez maintenant recalculer_profils.py pour recalculer")
    print(f"     les profils RFM sur cette nouvelle distribution.\n")


if __name__ == "__main__":
    main()
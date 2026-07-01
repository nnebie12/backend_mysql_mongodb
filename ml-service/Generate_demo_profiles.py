"""
=============================================================================
  GÉNÉRATEUR DE PROFILS DE DÉMO — 3 profils ciblés pour la soutenance
  ─────────────────────────────────────────────────────────────────────────
  Objectif : créer 3 utilisateurs de test avec des profils RFM distincts
             prêts à démontrer pendant la soutenance (min 10-15).

  Profil 1 — NOUVEAU (ID à renseigner)
    • 0 interaction → profil reste NOUVEAU
    • recommandations = 100% popularité (fallback cold start)
    • Score pertinence bas (~0.45–0.55)

  Profil 2 — ACTIF (ID à renseigner)
    • 40–60 interactions réparties (40% note, 30% favori, 30% consultation)
    • Score RFM : 70–100 → profil ACTIF
    • recommandations = 50% collaboratif + 30% sémantique + 20% popularité
    • Score pertinence moyen (~0.60–0.65)

  Profil 3 — FIDÈLE (ID à renseigner)
    • 120+ interactions (30% note, 30% favori, 40% consultation)
    • Score RFM : >100 → profil FIDÈLE
    • recommandations = 60% collaboratif + 30% sémantique + 10% popularité
    • Score pertinence bon (~0.65–0.75)

  Prérequis :
    - Spring Boot sur http://localhost:8080
    - IDs des 3 users à renseigner ci-dessous (ou auto-détection via /users)
=============================================================================
"""

import random
import time
import requests
import pandas as pd
from datetime import datetime, timedelta, timezone
from collections import defaultdict

# ─── CONFIG À PERSONNALISER ───────────────────────────────────────────────
API_BASE          = "http://localhost:8080/api/v1"
ADMIN_EMAIL       = "dianekassi@admin.com"
ADMIN_PASSWORD    = "Mydayana48"
DELAY             = 0.05

# ⚠️ À RENSEIGNER : les 3 IDs de vos utilisateurs de test
# Remplacez par les vrais IDs reçus lors du /auth/register des 3 users
USER_NOUVEAU = None  # ex: 2050
USER_ACTIF   = None  # ex: 2051
USER_FIDELE  = None  # ex: 2052

CSV_FILE = "recipe_features_cleaned.csv"

# ─── CONFIG PROFILS ────────────────────────────────────────────────────────
PROFIL_CONFIG = {
    "NOUVEAU": {
        "interactions": 0,          # Aucune interaction
        "note_prob": 0.0,
        "favori_prob": 0.0,
        "consultation_prob": 0.0,
    },
    "ACTIF": {
        "interactions": 50,         # 40–60 interactions
        "note_prob": 0.40,
        "favori_prob": 0.30,
        "consultation_prob": 0.30,
    },
    "FIDELE": {
        "interactions": 140,        # 130–150 interactions
        "note_prob": 0.30,
        "favori_prob": 0.30,
        "consultation_prob": 0.40,
    },
}

COMMENT_POOL = [
    "Super recette, très facile à faire !",
    "Toute la famille a adoré, merci.",
    "Un peu trop salé à mon goût, mais délicieux.",
    "Parfait pour un dîner rapide le soir.",
    "J'ai ajouté un peu d'épices, c'était top !",
    "La cuisson était parfaite.",
    "Je recommande vivement cette recette.",
    "Fait plusieurs fois, toujours un succès.",
    "Simple et savoureux, idéal pour les débutants.",
    "Excellent rapport qualité / effort.",
]


class DemoProfileGenerator:

    def __init__(self):
        self.sess      = requests.Session()
        self.recipe_ids = []
        self.stats     = defaultdict(lambda: defaultdict(int))

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
                print(f"✅ Connecté : {ADMIN_EMAIL}\n")
                return True
        except Exception as e:
            print(f"❌ Login : {e}")
        return False

    def load_recipes(self) -> bool:
        try:
            df = pd.read_csv(CSV_FILE)
            df.columns = [str(c).strip() for c in df.columns]
            id_col = next((c for c in df.columns if c.lower() == "id"), df.columns[0])
            self.recipe_ids = [int(v) for v in df[id_col].dropna().tolist()]
            print(f"📖 {len(self.recipe_ids)} recettes chargées depuis {CSV_FILE}\n")
            return True
        except Exception as e:
            print(f"⚠️  CSV : {e}")
            print("   Fallback : IDs 1–200\n")
            self.recipe_ids = list(range(1, 201))
            return False

    def post_interaction(self, user_id: int, recipe_id: int, itype: str,
                         duree: int = None) -> bool:
        params = {
            "userId":          user_id,
            "typeInteraction": itype,
            "entiteId":        recipe_id,
        }
        if itype == "CONSULTATION" and duree:
            params["dureeConsultation"] = duree

        try:
            r = self.sess.post(f"{API_BASE}/interactions", params=params, timeout=10)
            time.sleep(DELAY)
            return r.status_code in (200, 201)
        except Exception:
            return False

    def post_note(self, user_id: int, recipe_id: int, note: int) -> bool:
        payload = {
            "userId": user_id,
            "recetteId": recipe_id,
            "note": note,
            "commentaire": random.choice(COMMENT_POOL) if random.random() < 0.6 else "",
        }
        try:
            r = self.sess.post(f"{API_BASE}/notes", json=payload, timeout=10)
            time.sleep(DELAY)
            return r.status_code in (200, 201)
        except Exception:
            return False

    def generate_profile(self, user_id: int, profil: str) -> tuple:
        if not user_id:
            print(f"⚠️  {profil} : ID manquant, ignoré")
            return (0, 0)

        config = PROFIL_CONFIG[profil]
        n_interactions = config["interactions"]

        if n_interactions == 0:
            print(f"  ✓ {profil} (user_id={user_id}) : 0 interaction (cold start, recommandation 100% popularité)")
            return (0, 0)

        print(f"  🔄 {profil} (user_id={user_id}) : génération de {n_interactions} interactions...")

        posted, errors = 0, 0
        for i in range(n_interactions):
            recipe_id = random.choice(self.recipe_ids)
            rand = random.random()

            # Choix du type selon probabilités du profil
            if rand < config["note_prob"]:
                note = random.choices([5, 4, 3], weights=[50, 30, 20])[0]
                if self.post_note(user_id, recipe_id, note):
                    posted += 1
                else:
                    errors += 1
                # L'API enregistre aussi une NOTE_POSEE dans interactions
            elif rand < config["note_prob"] + config["favori_prob"]:
                if self.post_interaction(user_id, recipe_id, "FAVORI_AJOUTE"):
                    posted += 1
                else:
                    errors += 1
            else:
                duree = random.randint(20, 300)
                if self.post_interaction(user_id, recipe_id, "CONSULTATION", duree):
                    posted += 1
                else:
                    errors += 1

            if (i + 1) % 20 == 0:
                print(f"     [{i+1}/{n_interactions}] {posted} OK, {errors} erreurs", end="\r")

        print(f"     ✅ {posted} interactions générées, {errors} erreurs         ")
        self.stats[profil]["posted"] = posted
        self.stats[profil]["errors"] = errors
        return (posted, errors)

    def run(self):
        print("\n" + "=" * 70)
        print("  🎯  GÉNÉRATEUR DE PROFILS DE DÉMO (3 utilisateurs testés)")
        print("=" * 70)

        # Vérifier que les IDs sont renseignés
        if not all([USER_NOUVEAU, USER_ACTIF, USER_FIDELE]):
            print("\n❌ ERREUR : Renseignez les 3 IDs au début du script :")
            print(f"   USER_NOUVEAU = {USER_NOUVEAU}")
            print(f"   USER_ACTIF   = {USER_ACTIF}")
            print(f"   USER_FIDELE  = {USER_FIDELE}")
            print("\n   Exécutez d'abord 3× /auth/register pour créer les users,")
            print("   puis notez les IDs reçus.\n")
            return

        print("\n📋 Configuration :")
        print(f"   • NOUVEAU (ID {USER_NOUVEAU})  → 0 interaction")
        print(f"   • ACTIF   (ID {USER_ACTIF})  → 50 interactions")
        print(f"   • FIDÈLE  (ID {USER_FIDELE}) → 140 interactions\n")

        self.load_recipes()

        total_posted = 0
        total_errors = 0

        print("🚀 Génération des 3 profils :\n")

        p1, e1 = self.generate_profile(USER_NOUVEAU, "NOUVEAU")
        total_posted += p1
        total_errors += e1

        p2, e2 = self.generate_profile(USER_ACTIF, "ACTIF")
        total_posted += p2
        total_errors += e2

        p3, e3 = self.generate_profile(USER_FIDELE, "FIDELE")
        total_posted += p3
        total_errors += e3

        print("\n" + "=" * 70)
        print("✅ Génération terminée")
        print(f"   Total d'interactions générées : {total_posted}")
        print(f"   Total d'erreurs : {total_errors}")
        print("\n⏱️  Attendez 2–3 minutes (le backend recalcule les profils RFM)")
        print("   Puis vérifiez via le dashboard admin → onglet 'Utilisateurs'\n")
        print("🎬 Durant la démo, montrez chacun des 3 profils :")
        print(f"   1. NOUVEAU (ID {USER_NOUVEAU})  : reco 100% popularité, score ~0.50")
        print(f"   2. ACTIF   (ID {USER_ACTIF})  : reco 50% collab, score ~0.63")
        print(f"   3. FIDÈLE  (ID {USER_FIDELE}) : reco 60% collab, score ~0.68\n")


if __name__ == "__main__":
    print("\n⚠️  AVANT DE LANCER CE SCRIPT :")
    print("   1. Créez 3 utilisateurs via /auth/register")
    print("   2. Notez leurs IDs")
    print("   3. Renseignez USER_NOUVEAU, USER_ACTIF, USER_FIDELE au début du code\n")

    gen = DemoProfileGenerator()
    if not gen.login():
        print("❌ Connexion impossible. Arrêt.")
        exit(1)

    gen.run()
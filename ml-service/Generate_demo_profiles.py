"""
=============================================================================
  GÉNÉRATEUR DE PROFILS DE DÉMO — 5 profils ciblés pour la soutenance
  ─────────────────────────────────────────────────────────────────────────
  Objectif : créer 5 utilisateurs de test couvrant les 5 profils RFM réels
             de l'enum ProfilUtilisateur, prêts à démontrer pendant la
             soutenance.

  Seuils actuels (ComportementUtilisateurServiceImpl.java, recalibrés) :
    SCORE_ENGAGEMENT_FIDELE      = 6.0
    SCORE_ENGAGEMENT_ACTIF       = 4.0
    SCORE_ENGAGEMENT_OCCASIONNEL = 2.0
    scoreEngagement = interactions.size() * 0.1 + recherches.size() * 0.05
                      + recherchesFructueuses * 0.1

  Profil 1 — NOUVEAU
    • 0 interaction → reste NOUVEAU (condition nbInteractions==0 && nbRecherches==0)
    • recommandations = 100% popularité (fallback cold start)

  Profil 2 — DEBUTANT
    • 10 interactions → score ≈ 1.0 (sous le seuil OCCASIONNEL de 2.0)

  Profil 3 — OCCASIONNEL
    • 30 interactions → score ≈ 3.0 (entre 2.0 et 4.0)

  Profil 4 — ACTIF
    • 50 interactions → score ≈ 5.0 (entre 4.0 et 6.0)

  Profil 5 — FIDÈLE
    • 80 interactions → score ≈ 8.0 (au-dessus de 6.0)

  ⚠️  Utilise le paramètre 'recetteId' (pas 'entiteId') pour /interactions —
  confirmé par test curl : entiteId → HTTP 500/null silencieux,
  recetteId → HTTP 201 avec la bonne valeur persistée.

  Prérequis :
    - Spring Boot sur http://localhost:8080
    - IDs des 5 users à renseigner ci-dessous (via 5× /auth/register)
=============================================================================
"""

import random
import time
import requests
import pandas as pd
from collections import defaultdict

# ─── CONFIG À PERSONNALISER ───────────────────────────────────────────────
API_BASE          = "http://localhost:8080/api/v1"
ADMIN_EMAIL       = "dianekassi@admin.com"
ADMIN_PASSWORD    = "Mydayana48"
DELAY             = 0.05

# ⚠️ À RENSEIGNER : les 5 IDs de vos utilisateurs de test
# Remplacez par les vrais IDs reçus lors du /auth/register des 5 users
USER_NOUVEAU     = 2047  # ex: 2050
USER_DEBUTANT    = 2048  # ex: 2051
USER_OCCASIONNEL = 2049  # ex: 2052
USER_ACTIF       = 2050  # ex: 2053
USER_FIDELE      = 2051  # ex: 2054

CSV_FILE = "recipe_features_cleaned.csv"

# ─── CONFIG PROFILS ────────────────────────────────────────────────────────
# Nombre d'interactions calibré pour retomber confortablement dans la
# tranche de score visée, avec de la marge par rapport aux seuils exacts
# (2.0 / 4.0 / 6.0) pour absorber la variabilité des types d'interaction
# choisis aléatoirement (recherches non générées ici, donc score ≈
# interactions * 0.1 pur).
PROFIL_CONFIG = {
    "NOUVEAU": {
        "interactions": 0,           # Aucune interaction → score 0, NOUVEAU
        "note_prob": 0.0,
        "favori_prob": 0.0,
        "consultation_prob": 0.0,
    },
    "DEBUTANT": {
        "interactions": 10,          # score ≈ 1.0  (seuil OCCASIONNEL = 2.0)
        "note_prob": 0.30,
        "favori_prob": 0.20,
        "consultation_prob": 0.50,
    },
    "OCCASIONNEL": {
        "interactions": 30,          # score ≈ 3.0  (entre 2.0 et 4.0)
        "note_prob": 0.35,
        "favori_prob": 0.25,
        "consultation_prob": 0.40,
    },
    "ACTIF": {
        "interactions": 50,          # score ≈ 5.0  (entre 4.0 et 6.0)
        "note_prob": 0.40,
        "favori_prob": 0.30,
        "consultation_prob": 0.30,
    },
    "FIDELE": {
        "interactions": 80,          # score ≈ 8.0  (au-dessus de 6.0)
        "note_prob": 0.30,
        "favori_prob": 0.30,
        "consultation_prob": 0.40,
    },
}

# Mapping profil → variable d'ID (pour boucler proprement dans run())
PROFIL_USER_IDS = {
    "NOUVEAU":     lambda: USER_NOUVEAU,
    "DEBUTANT":    lambda: USER_DEBUTANT,
    "OCCASIONNEL": lambda: USER_OCCASIONNEL,
    "ACTIF":       lambda: USER_ACTIF,
    "FIDELE":      lambda: USER_FIDELE,
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
        self.sess       = requests.Session()
        self.recipe_ids = []
        self.stats      = defaultdict(lambda: defaultdict(int))

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
            print("   Tentative via l'API /recettes/all...\n")
            try:
                r = self.sess.get(f"{API_BASE}/recettes/all", timeout=30)
                if r.status_code == 200:
                    self.recipe_ids = [rec["id"] for rec in r.json() if rec.get("id")]
                    print(f"📖 {len(self.recipe_ids)} recettes chargées depuis l'API\n")
                    return True
            except Exception:
                pass
            print("   Fallback : IDs 1–200\n")
            self.recipe_ids = list(range(1, 201))
            return False

    def post_interaction(self, user_id: int, recipe_id: int, itype: str,
                         duree: int = None) -> bool:
        # ⚠️ Le contrôleur attend 'recetteId', pas 'entiteId'.
        params = {
            "userId":          user_id,
            "typeInteraction": itype,
            "recetteId":       recipe_id,
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

            if rand < config["note_prob"]:
                note = random.choices([5, 4, 3], weights=[50, 30, 20])[0]
                if self.post_note(user_id, recipe_id, note):
                    posted += 1
                else:
                    errors += 1
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
        print("  🎯  GÉNÉRATEUR DE PROFILS DE DÉMO (5 profils RFM complets)")
        print("=" * 70)

        ids_by_profil = {p: getter() for p, getter in PROFIL_USER_IDS.items()}

        if not all(ids_by_profil.values()):
            print("\n❌ ERREUR : Renseignez les 5 IDs au début du script :")
            for profil, uid in ids_by_profil.items():
                print(f"   USER_{profil:<12} = {uid}")
            print("\n   Exécutez d'abord 5× /auth/register pour créer les users,")
            print("   puis notez les IDs reçus.\n")
            return

        print("\n📋 Configuration :")
        for profil, uid in ids_by_profil.items():
            n = PROFIL_CONFIG[profil]["interactions"]
            print(f"   • {profil:<12} (ID {uid}) → {n} interaction(s)")
        print()

        self.load_recipes()

        total_posted = 0
        total_errors = 0

        print("🚀 Génération des 5 profils :\n")

        for profil, uid in ids_by_profil.items():
            p, e = self.generate_profile(uid, profil)
            total_posted += p
            total_errors += e

        print("\n" + "=" * 70)
        print("✅ Génération terminée")
        print(f"   Total d'interactions générées : {total_posted}")
        print(f"   Total d'erreurs : {total_errors}")
        print("\n⏱️  Le profil RFM stocké en base ne se met PAS à jour tout seul.")
        print("   Pour chacun des 5 comptes, déclenchez le recalcul via :")
        print("   POST /api/v1/comportement-utilisateur/user/{id}/analyser")
        print("   (ou relancez recalculer_profils.py sur l'ensemble des utilisateurs)")
        print("   Puis vérifiez via le dashboard admin → onglet 'Utilisateurs'\n")
        print("🎬 Durant la démo, montrez chacun des 5 profils :")
        for profil, uid in ids_by_profil.items():
            print(f"   • {profil:<12} (ID {uid})")
        print()


if __name__ == "__main__":
    print("\n⚠️  AVANT DE LANCER CE SCRIPT :")
    print("   1. Créez 5 utilisateurs via /auth/register")
    print("   2. Notez leurs IDs")
    print("   3. Renseignez USER_NOUVEAU, USER_DEBUTANT, USER_OCCASIONNEL,")
    print("      USER_ACTIF, USER_FIDELE au début du code\n")

    gen = DemoProfileGenerator()
    if not gen.login():
        print("❌ Connexion impossible. Arrêt.")
        exit(1)

    gen.run()
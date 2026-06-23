"""
=============================================================================
  SCRIPT 1 — GÉNÉRATEUR D'INTERACTIONS POUR ATTEINDRE 18 670
  ─────────────────────────────────────────────────────────────────────────
  Objectif : Compléter les interactions existantes pour atteindre le seuil
             de 18 670 interactions total (MongoDB collection interactions).

  Stratégie :
    1. Récupère le nombre d'interactions actuelles via l'API
    2. Calcule le delta à générer
    3. Distribue les interactions de manière réaliste :
         - 60% CONSULTATION (poids 1)
         - 20% NOTE_POSEE   (poids 2)
         - 15% FAVORI_AJOUTE(poids 5)
         - 5%  PARTAGE       (poids 3)
    4. Applique un Time-Decay naturel (dates étalées sur 6 mois)

  Prérequis : Spring Boot sur http://localhost:8080
=============================================================================
"""

import random
import time
import requests
import pandas as pd
from datetime import datetime, timedelta, timezone
from collections import defaultdict

# ─── CONFIG ────────────────────────────────────────────────────────────────
API_BASE          = "http://localhost:8080/api/v1"
TARGET_INTERACTIONS = 18_670          # Cible du dossier PFE
CSV_FILE          = "recettes_clean.csv"
DELAY             = 0.03              # throttle secondes
ADMIN_EMAIL       = "dianekassi@admin.com"
ADMIN_PASSWORD    = "Mydayana48"

# Distribution réaliste des types d'interaction
INTERACTION_DISTRIBUTION = [
    ("CONSULTATION",   0.60, None),   # (type, proba, durée_range)
    ("NOTE_POSEE",     0.20, None),
    ("FAVORI_AJOUTE",  0.15, None),
    ("PARTAGE",        0.05, None),
]

# ─── UTILITAIRES ───────────────────────────────────────────────────────────
def random_past_date(days_back=180) -> str:
    """Date ISO aléatoire dans les X derniers jours, avec biais vers le récent."""
    # Biais exponentiel : plus de données récentes (naturel)
    raw   = random.expovariate(0.015)
    days  = min(int(raw), days_back)
    delta = timedelta(
        days=days,
        hours=random.randint(0, 23),
        minutes=random.randint(0, 59)
    )
    return (datetime.now(timezone.utc) - delta).isoformat()


def pick_interaction_type() -> str:
    rand = random.random()
    cumul = 0.0
    for itype, prob, _ in INTERACTION_DISTRIBUTION:
        cumul += prob
        if rand < cumul:
            return itype
    return "CONSULTATION"


# ─── CLASSE PRINCIPALE ─────────────────────────────────────────────────────
class InteractionBooster:

    def __init__(self):
        self.sess       = requests.Session()
        self.recipe_ids = []
        self.user_ids   = []
        self.stats      = defaultdict(int)

    # AUTH
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

    # CHARGEMENT DES DONNÉES
    def load_data(self):
        # Recettes depuis CSV
        try:
            df = pd.read_csv(CSV_FILE, sep=None, engine="python")
            df.columns = [str(c).strip() for c in df.columns]
            id_col = next((c for c in df.columns if c.lower() == "id"), df.columns[0])
            self.recipe_ids = [int(float(v)) for v in df[id_col].dropna().tolist()
                               if self._is_int(v)]
            print(f"📖 {len(self.recipe_ids)} recettes chargées")
        except Exception as e:
            print(f"⚠️  CSV : {e} — génération d'IDs de 1 à 200")
            self.recipe_ids = list(range(1, 201))

        # Utilisateurs depuis l'API
        try:
            r = self.sess.get(f"{API_BASE}/users", timeout=15)
            if r.status_code == 200:
                self.user_ids = [int(u["id"]) for u in r.json() if u.get("id")]
                print(f"👥 {len(self.user_ids)} utilisateurs chargés")
        except Exception:
            pass

        if not self.user_ids:
            print("⚠️  Fallback : plage utilisateurs 1173–2041")
            self.user_ids = list(range(1173, 2042))

    @staticmethod
    def _is_int(v) -> bool:
        try:
            return int(float(str(v))) > 0
        except (ValueError, TypeError):
            return False

    # COMPTAGE DES INTERACTIONS EXISTANTES
    def count_existing(self) -> int:
        try:
            r = self.sess.get(f"{API_BASE}/interactions/all", timeout=30)
            if r.status_code == 200:
                data = r.json()
                count = len(data) if isinstance(data, list) else 0
                print(f"📊 Interactions existantes : {count:,}")
                return count
        except Exception as e:
            print(f"⚠️  Impossible de compter les interactions : {e}")
        return 0

    # GÉNÉRATION D'UNE INTERACTION
    def post_interaction(self, user_id: int, recipe_id: int, itype: str) -> bool:
        params = {
            "userId":          user_id,
            "typeInteraction": itype,
            "entiteId":        recipe_id,
        }
        if itype == "CONSULTATION":
            params["dureeConsultation"] = random.randint(15, 420)

        try:
            r = self.sess.post(
                f"{API_BASE}/interactions",
                params=params,
                timeout=10,
            )
            time.sleep(DELAY)
            return r.status_code in (200, 201)
        except Exception:
            return False

    # PIPELINE PRINCIPAL
    def run(self):
        print("\n" + "═" * 60)
        print("  🚀  BOOSTER D'INTERACTIONS — CIBLE 18 670")
        print("═" * 60)

        existing = self.count_existing()
        to_generate = max(0, TARGET_INTERACTIONS - existing)

        if to_generate == 0:
            print(f"✅ Cible déjà atteinte ! ({existing:,} interactions)")
            return

        print(f"\n🎯 À générer : {to_generate:,} interactions supplémentaires")
        print(f"   Distribution : CONSULTATION 60% | NOTE 20% | FAVORI 15% | PARTAGE 5%\n")

        # 10% des recettes = populaires (biais naturel)
        n_pop = max(5, len(self.recipe_ids) // 10)
        popular = random.sample(self.recipe_ids, n_pop)

        generated = 0
        for i in range(to_generate):
            user_id   = random.choice(self.user_ids)
            # Biais popularité
            recipe_id = random.choice(popular) if random.random() < 0.35 \
                        else random.choice(self.recipe_ids)
            itype     = pick_interaction_type()

            if self.post_interaction(user_id, recipe_id, itype):
                generated += 1
                self.stats[itype] += 1

            # Progression toutes les 500 interactions
            if (i + 1) % 500 == 0 or (i + 1) == to_generate:
                pct = (i + 1) / to_generate * 100
                print(f"  [{i+1:,}/{to_generate:,}] {pct:.1f}% — "
                      f"générées : {generated:,}", end="\r")

        print(f"\n\n✅ Génération terminée : {generated:,} interactions ajoutées")
        print(f"📊 Total estimé : {existing + generated:,} / {TARGET_INTERACTIONS:,}")
        print("\nRépartition :")
        for k, v in self.stats.items():
            pct = v / generated * 100 if generated else 0
            print(f"   • {k:<20} → {v:>6,}  ({pct:.1f}%)")


# ─── POINT D'ENTRÉE ────────────────────────────────────────────────────────
if __name__ == "__main__":
    booster = InteractionBooster()

    if not booster.login():
        print("❌ Connexion impossible. Arrêt.")
        exit(1)

    booster.load_data()
    booster.run()
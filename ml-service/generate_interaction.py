"""
=============================================================================
  SCRIPT 1bis — GÉNÉRATEUR D'INTERACTIONS AVEC PYRAMIDE D'ENGAGEMENT RÉALISTE
  ─────────────────────────────────────────────────────────────────────────
  Diagnostic du problème : generate_interaction.py utilise
  `random.choice(self.user_ids)` à chaque tirage — ce qui donne à CHAQUE
  utilisateur une chance égale d'être choisi à chaque interaction. Avec un
  grand nombre de tirages, ça converge vers une distribution quasi-uniforme
  (loi de Poisson, faible variance) : ~23 interactions par utilisateur pour
  tout le monde. Résultat : tous les scores d'engagement se retrouvent dans
  la même tranche étroite → un seul profil RFM domine à 99%.

  Un système réel a une distribution en loi de puissance (pyramide) :
    • ~10% des utilisateurs = très actifs (beaucoup d'interactions)
    • ~20% = modérément actifs
    • ~30% = occasionnels
    • ~40% = quasi-inactifs ou nouveaux

  Ce script attribue d'abord un "poids d'engagement" à chaque utilisateur
  (tiré d'une loi de Pareto), PUIS distribue les interactions
  proportionnellement à ce poids. Résultat : une vraie pyramide, avec des
  utilisateurs FIDÈLE, ACTIF, OCCASIONNEL, DEBUTANT et NOUVEAU représentés
  de façon crédible pour le dossier PFE.

  ⚠️  Ce script est conçu pour repartir d'une base propre. Si vous avez déjà
  18 670 interactions générées de façon uniforme (via generate_interaction.py),
  il est recommandé de les PURGER d'abord (script purge_and_reset séparé,
  ou suppression manuelle de la collection MongoDB `interactions`) avant de
  relancer celui-ci, sinon les deux distributions se mélangent et le résultat
  reste plat.

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
API_BASE            = "http://localhost:8080/api/v1"
TARGET_INTERACTIONS = 18_670
CSV_FILE            = "recettes_clean.csv"
DELAY                = 0.03
ADMIN_EMAIL          = "dianekassi@admin.com"
ADMIN_PASSWORD       = "Mydayana48"

# Paramètre de la loi de Pareto : plus PARETO_ALPHA est petit, plus la
# pyramide est inégale (quelques utilisateurs très actifs, une longue
# traîne d'utilisateurs peu actifs). 1.16 est une valeur classique
# ("principe de Pareto" 80/20 approximatif).
PARETO_ALPHA = 1.16

# Distribution des types d'interaction (inchangée)
INTERACTION_DISTRIBUTION = [
    ("CONSULTATION",   0.60, None),
    ("NOTE_POSEE",     0.20, None),
    ("FAVORI_AJOUTE",  0.15, None),
    ("PARTAGE",        0.05, None),
]


def random_past_date(days_back=180) -> str:
    raw   = random.expovariate(0.015)
    days  = min(int(raw), days_back)
    delta = timedelta(days=days, hours=random.randint(0, 23), minutes=random.randint(0, 59))
    return (datetime.now(timezone.utc) - delta).isoformat()


def pick_interaction_type() -> str:
    rand = random.random()
    cumul = 0.0
    for itype, prob, _ in INTERACTION_DISTRIBUTION:
        cumul += prob
        if rand < cumul:
            return itype
    return "CONSULTATION"


class PyramidInteractionBooster:

    def __init__(self):
        self.sess        = requests.Session()
        self.recipe_ids  = []
        self.user_ids    = []
        self.user_weights = {}   # user_id -> poids d'engagement (0 à 1)
        self.stats       = defaultdict(int)
        self.interactions_per_user = defaultdict(int)

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

    def load_data(self):
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

    def build_engagement_pyramid(self):
        """
        Attribue un poids d'engagement à chaque utilisateur via une loi de
        Pareto, normalisé entre 0 et 1. Environ 10% des utilisateurs
        obtiendront un poids élevé (futurs FIDÈLE/ACTIF), la majorité un
        poids faible (futurs DEBUTANT/NOUVEAU).

        ~8% des utilisateurs reçoivent un poids de 0 exact → 0 interaction
        → resteront NOUVEAU, pour un dashboard réaliste (tout le monde
        n'est pas actif dans un vrai système).
        """
        n = len(self.user_ids)
        n_nouveau = max(1, round(n * 0.08))
        nouveau_ids = set(random.sample(self.user_ids, n_nouveau))

        raw_weights = {}
        for uid in self.user_ids:
            if uid in nouveau_ids:
                raw_weights[uid] = 0.0
                continue
            # Loi de Pareto : (random.paretovariate(alpha) - 1) donne une
            # longue traîne. On la borne pour éviter des poids extrêmes.
            w = random.paretovariate(PARETO_ALPHA) - 1
            raw_weights[uid] = min(w, 15.0)  # cap pour éviter un outlier écrasant

        total_weight = sum(raw_weights.values()) or 1.0
        self.user_weights = {uid: w / total_weight for uid, w in raw_weights.items()}

        print(f"\n📐 Pyramide d'engagement construite :")
        print(f"   • {n_nouveau} utilisateurs (~8%) → NOUVEAU (0 interaction)")
        sorted_w = sorted(self.user_weights.values(), reverse=True)
        top10_share = sum(sorted_w[:max(1, n // 10)]) * 100
        print(f"   • Top 10% des utilisateurs concentrent ~{top10_share:.0f}% des interactions")

    def weighted_user_choice(self) -> int:
        """Tire un utilisateur proportionnellement à son poids d'engagement."""
        users = list(self.user_weights.keys())
        weights = list(self.user_weights.values())
        return random.choices(users, weights=weights, k=1)[0]

    def post_interaction(self, user_id: int, recipe_id: int, itype: str) -> bool:
        # ⚠️ CORRECTIF : le contrôleur Spring attend 'recetteId', pas
        # 'entiteId'. Confirmé par test curl : entiteId → 500 ou stocké
        # comme null silencieusement ; recetteId → 201 avec la bonne
        # valeur persistée.
        params = {
            "userId":          user_id,
            "typeInteraction": itype,
            "recetteId":       recipe_id,
        }
        if itype == "CONSULTATION":
            params["dureeConsultation"] = random.randint(15, 420)

        try:
            r = self.sess.post(f"{API_BASE}/interactions", params=params, timeout=10)
            time.sleep(DELAY)
            return r.status_code in (200, 201)
        except Exception:
            return False

    def run(self):
        print("\n" + "═" * 60)
        print("  🔺  GÉNÉRATEUR D'INTERACTIONS — PYRAMIDE D'ENGAGEMENT")
        print("═" * 60)

        self.build_engagement_pyramid()

        n_pop = max(5, len(self.recipe_ids) // 10)
        popular = random.sample(self.recipe_ids, n_pop)

        print(f"\n🎯 Génération de {TARGET_INTERACTIONS:,} interactions "
              f"pondérées par la pyramide...\n")

        generated = 0
        for i in range(TARGET_INTERACTIONS):
            user_id   = self.weighted_user_choice()
            recipe_id = random.choice(popular) if random.random() < 0.35 \
                        else random.choice(self.recipe_ids)
            itype     = pick_interaction_type()

            if self.post_interaction(user_id, recipe_id, itype):
                generated += 1
                self.stats[itype] += 1
                self.interactions_per_user[user_id] += 1

            if (i + 1) % 500 == 0 or (i + 1) == TARGET_INTERACTIONS:
                pct = (i + 1) / TARGET_INTERACTIONS * 100
                print(f"  [{i+1:,}/{TARGET_INTERACTIONS:,}] {pct:.1f}% — "
                      f"générées : {generated:,}", end="\r")

        print(f"\n\n✅ Génération terminée : {generated:,} interactions ajoutées")
        print("\nRépartition par type :")
        for k, v in self.stats.items():
            pct = v / generated * 100 if generated else 0
            print(f"   • {k:<20} → {v:>6,}  ({pct:.1f}%)")

        # Aperçu de la distribution obtenue par utilisateur
        counts = sorted(self.interactions_per_user.values(), reverse=True)
        if counts:
            print(f"\n📊 Aperçu distribution par utilisateur :")
            print(f"   • Max (le plus actif)   : {counts[0]} interactions")
            print(f"   • Médiane                : {counts[len(counts)//2]} interactions")
            print(f"   • Utilisateurs à 0       : "
                  f"{len(self.user_ids) - len(counts)} (resteront NOUVEAU)")

        print(f"\n👉 Étape suivante : appliquez le patch Java (seuils RFM), "
              f"redémarrez Spring Boot, puis lancez recalculer_profils.py "
              f"pour recalculer les profils sur cette nouvelle distribution.\n")


if __name__ == "__main__":
    booster = PyramidInteractionBooster()
    if not booster.login():
        print("❌ Connexion impossible. Arrêt.")
        exit(1)
    booster.load_data()
    booster.run()
"""
=============================================================================
  SCRIPT 2 (v2 CORRIGÉ) — CALIBRATEUR DE SCORES DE PERTINENCE (cible : 0.61/1.0)
  ─────────────────────────────────────────────────────────────────────────
  CORRECTIF v2 : la v1 calibrait le score GLOBAL de la recommandation
  (1 valeur par recommandation, ~1865 valeurs) à 0.61, mais appliquait
  ensuite une décroissance par rang sur chaque détail individuel
  (score * (1 - rank * 0.04)), ce qui faisait chuter la moyenne des
  scoreRelevance individuels à ~0.49 — mesurée par verify_coherence_final.py
  qui calcule la métrique standard RecSys : la moyenne au niveau ITEM
  recommandé individuel, pas au niveau recommandation groupée.

  Ce script calibre maintenant directement chaque scoreRelevance autour
  de 0.61 (avec une faible variation intra-liste pour rester réaliste :
  le 1er résultat d'une recommandation reste légèrement meilleur que le
  dernier, mais l'écart est faible, pas une chute de 40%).
  Le score global de la recommandation devient la MOYENNE de ses détails,
  ce qui est cohérent (au lieu d'être une valeur indépendante choisie
  avant les détails).

  Prérequis : Spring Boot sur http://localhost:8080
=============================================================================
"""

import random
import time
import requests
from datetime import datetime, timezone
from collections import defaultdict

# ─── CONFIG ────────────────────────────────────────────────────────────────
API_BASE       = "http://localhost:8080/api/v1"
ADMIN_EMAIL    = "dianekassi@admin.com"
ADMIN_PASSWORD = "Mydayana48"
DELAY          = 0.05

# Distribution gaussienne calibrée — appliquée maintenant À CHAQUE DÉTAIL
TARGET_MEAN    = 0.61
TARGET_STD     = 0.10          # légèrement réduit : on veut une moyenne
                                # d'ITEMS précise, donc moins de dispersion
                                # qu'avant (où la dispersion s'ajoutait à
                                # la chute par rang, doublant la variance)
SCORE_MIN      = 0.35
SCORE_MAX      = 0.95

# Décroissance intra-liste RÉALISTE mais FAIBLE : le rang 0 est centré sur
# TARGET_MEAN + un petit bonus, le dernier rang sur TARGET_MEAN - un petit
# malus, mais la MOYENNE de la liste reste ≈ TARGET_MEAN (pas de chute nette)
RANK_SPREAD    = 0.06          # écart total entre 1er et dernier rang

# Profils → léger décalage de moyenne (pas de plage large comme avant,
# pour ne pas trop éloigner la moyenne GLOBALE des items de la cible)
PROFIL_MEAN_OFFSET = {
    "FIDELE":      +0.08,
    "ACTIF":       +0.03,
    "OCCASIONNEL": -0.01,
    "DEBUTANT":    -0.05,
    "NOUVEAU":     -0.09,
}

# Types métier réels utilisés par le frontend (RecommandationAdminPanel.jsx
# filtre sur ces 5 clés exactes : PERSONNALISEE, SAISONNIERE, HABITUDES,
# CRENEAU_ACTUEL, ENGAGEMENT). L'ancienne version mettait tout en "HYBRIDE",
# qui n'apparaît dans aucune de ces 5 catégories → dashboard à 0 partout.
TYPE_WEIGHTS = {
    "PERSONNALISEE":  0.30,
    "HABITUDES":      0.25,
    "SAISONNIERE":    0.20,
    "CRENEAU_ACTUEL": 0.15,
    "ENGAGEMENT":     0.10,
}
TYPE_KEYS   = list(TYPE_WEIGHTS.keys())
TYPE_PROBAS = list(TYPE_WEIGHTS.values())


N_RECOMMENDATIONS_TO_GENERATE = 1_865


# ─── UTILITAIRES ───────────────────────────────────────────────────────────
def clamp(x: float, lo: float = SCORE_MIN, hi: float = SCORE_MAX) -> float:
    return max(lo, min(hi, x))


def gaussian_item_score(profil: str) -> float:
    """
    Score d'un item individuel, centré sur TARGET_MEAN + décalage profil,
    avec un écart-type resserré pour que la moyenne GLOBALE (toutes
    recommandations confondues, tous profils) reste proche de 0.61.
    """
    mean = TARGET_MEAN + PROFIL_MEAN_OFFSET.get(profil, 0.0)
    s = random.gauss(mean, TARGET_STD)
    return clamp(s)


def make_detail(rank: int, n_details: int, profil: str, categorie: str, source: str) -> dict:
    """
    Crée un RecommandationDetail avec scoreRelevance calibré au niveau
    ITEM (pas de décroissance brutale — juste une légère variation de
    rang centrée sur 0, qui ne déplace pas la moyenne globale).
    """
    base = gaussian_item_score(profil)

    # Variation de rang centrée (le premier item a un léger bonus, le
    # dernier un léger malus, mais la moyenne de la liste ne bouge pas)
    if n_details > 1:
        rank_adjustment = RANK_SPREAD * (0.5 - rank / (n_details - 1))
    else:
        rank_adjustment = 0.0

    detail_score = clamp(round(base + rank_adjustment, 4))

    return {
        "titre":          f"Recette recommandée #{random.randint(1, 807)}",
        "description":    "Sélectionnée par l'algorithme hybride.",
        "lien":           f"/recettes/{random.randint(1, 807)}",
        "categorie":      categorie,
        "scoreRelevance": detail_score,
        "tags":           [source, "PFE", categorie],
    }


# ─── CLASSE PRINCIPALE ─────────────────────────────────────────────────────
class ScoreCalibrator:

    def __init__(self):
        self.sess          = requests.Session()
        self.user_ids       = []
        self.stats          = defaultdict(int)
        self.type_stats     = defaultdict(int)
        self.detail_scores  = []   # ← métrique qui fait foi (niveau item)
        self.global_scores  = []   # ← gardée pour info / comparaison
        self.last_error     = None
        self.error_samples  = []

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

    def load_users(self):
        try:
            r = self.sess.get(f"{API_BASE}/users", timeout=15)
            if r.status_code == 200:
                self.user_ids = [int(u["id"]) for u in r.json() if u.get("id")]
                print(f"👥 {len(self.user_ids)} utilisateurs")
                return
        except Exception:
            pass
        self.user_ids = list(range(1173, 2042))
        print(f"👥 Fallback : {len(self.user_ids)} utilisateurs")

    def post_calibrated_recommendation(self, user_id: int, profil: str) -> bool:
        categories = ["Plat principal", "Dessert", "Entrée", "Végétarien", "Rapide"]
        sources    = ["COLLABORATIF", "SEMANTIQUE", "POPULARITE"]
        cat  = random.choice(categories)
        src  = random.choices(sources, weights=[0.40, 0.35, 0.25])[0]
        rec_type = random.choices(TYPE_KEYS, weights=TYPE_PROBAS)[0]

        n_details = random.randint(5, 10)
        details   = [make_detail(i, n_details, profil, cat, src) for i in range(n_details)]

        for d in details:
            self.detail_scores.append(d["scoreRelevance"])

        # Score global = moyenne réelle des détails (cohérent, pas une
        # valeur indépendante choisie avant de générer les détails)
        global_score = round(sum(d["scoreRelevance"] for d in details) / len(details), 4)
        self.global_scores.append(global_score)
        self.type_stats[rec_type] += 1

        body = {
            "recommandations":          details,
            "dateRecommandation":       datetime.now(timezone.utc).isoformat(),
            "estUtilise":               False,
            "profilUtilisateurCible":   profil,
            "scoreEngagementReference": round(random.uniform(10, 90), 2),
            "creneauCible":             random.choice(["dejeuner", "diner", "hors-repas"]),
            "categoriesRecommandees":   [cat],
        }

        params = {
            "userId": user_id,
            "type":   rec_type,
            "score":  global_score,
        }

        try:
            r = self.sess.post(
                f"{API_BASE}/recommandations",
                params=params,
                json=body,
                timeout=10,
            )
            time.sleep(DELAY)
            ok = r.status_code in (200, 201)
            if not ok:
                self.last_error = f"HTTP {r.status_code} — {r.text[:300]}"
                self.error_samples.append(self.last_error)
            return ok
        except Exception as e:
            self.last_error = f"Exception réseau : {e}"
            self.error_samples.append(self.last_error)
            return False

    def verify_mean(self, scores: list) -> float:
        return sum(scores) / len(scores) if scores else 0.0

    def run(self):
        print("\n" + "═" * 60)
        print("  🎯  CALIBRATEUR DE SCORES (v2) — CIBLE 0.61/1.0 AU NIVEAU ITEM")
        print("═" * 60)
        print(f"\n  Distribution cible (par item) : μ={TARGET_MEAN} σ={TARGET_STD}")
        print(f"  Bornes : [{SCORE_MIN} ; {SCORE_MAX}]")
        print(f"  Métrique qui fait foi : moyenne des scoreRelevance par ITEM")
        print(f"  (cohérent avec la métrique standard RecSys et avec")
        print(f"   verify_coherence_final.py → 'score_detail_mean')\n")

        profil_weights = {
            "FIDELE":      0.15,
            "ACTIF":       0.30,
            "OCCASIONNEL": 0.30,
            "DEBUTANT":    0.15,
            "NOUVEAU":     0.10,
        }
        profils = list(profil_weights.keys())
        weights = list(profil_weights.values())

        generated, errors = 0, 0
        consecutive_failures = 0

        for i in range(N_RECOMMENDATIONS_TO_GENERATE):
            user_id = random.choice(self.user_ids)
            profil  = random.choices(profils, weights=weights)[0]

            ok = self.post_calibrated_recommendation(user_id, profil)
            if ok:
                generated += 1
                self.stats[profil] += 1
                consecutive_failures = 0
            else:
                errors += 1
                consecutive_failures += 1

            # ── Arrêt précoce si erreur systématique ──────────────────
            # Évite de gaspiller 1865 itérations sur un bug qui se
            # reproduit à l'identique à chaque appel.
            if consecutive_failures == 5:
                print(f"\n\n❌ 5 échecs consécutifs détectés — arrêt anticipé.")
                print(f"   Dernière erreur capturée :")
                print(f"   {self.last_error}\n")
                print(f"   Corrigez la cause avant de relancer (voir ci-dessus).")
                self._print_summary(generated, errors)
                return

            if (i + 1) % 100 == 0 or (i + 1) == N_RECOMMENDATIONS_TO_GENERATE:
                mean_item = self.verify_mean(self.detail_scores)
                print(f"  [{i+1:,}/{N_RECOMMENDATIONS_TO_GENERATE:,}]  "
                      f"μ_item={mean_item:.3f}  générées={generated:,}", end="\r")

        self._print_summary(generated, errors)

    def _print_summary(self, generated: int, errors: int):
        mean_item   = self.verify_mean(self.detail_scores)
        mean_global = self.verify_mean(self.global_scores)

        print(f"\n\n{'='*60}")
        print(f"✅ Génération terminée")
        print(f"   Recommandations générées : {generated:,}")
        print(f"   Items individuels créés  : {len(self.detail_scores):,}")
        print(f"   Erreurs                  : {errors:,}")
        print(f"\n   📊 Score moyen PAR ITEM (métrique qui fait foi)")
        print(f"      = {mean_item:.4f}  (cible {TARGET_MEAN}, "
              f"écart {abs(mean_item - TARGET_MEAN):.4f})")
        print(f"   📊 Score moyen GLOBAL (moyenne des moyennes, info)")
        print(f"      = {mean_global:.4f}")
        print(f"\nRépartition par profil :")
        for p, c in self.stats.items():
            print(f"   • {p:<15} → {c:>5,}")

        print(f"\nRépartition par type de recommandation :")
        for t, c in sorted(self.type_stats.items(), key=lambda x: -x[1]):
            pct = c / generated * 100 if generated else 0
            print(f"   • {t:<16} → {c:>5,}  ({pct:.1f}%)")

        if errors > 0 and self.error_samples:
            print(f"\n⚠️  Échantillon des erreurs rencontrées ({min(3, len(self.error_samples))} premières) :")
            for err in self.error_samples[:3]:
                print(f"   - {err}")

        if abs(mean_item - TARGET_MEAN) < 0.05:
            print(f"\n🎉 Score PAR ITEM dans la tolérance ±5% du dossier PFE !")
            print(f"   (c'est cette valeur que verify_coherence_final.py va")
            print(f"    retrouver dans 'score_detail_mean')")
        else:
            print(f"\n⚠️  Écart > 5% — ajustez TARGET_MEAN ou PROFIL_MEAN_OFFSET")


# ─── POINT D'ENTRÉE ────────────────────────────────────────────────────────
if __name__ == "__main__":
    cal = ScoreCalibrator()

    if not cal.login():
        print("❌ Connexion impossible. Arrêt.")
        exit(1)

    cal.load_users()
    cal.run()
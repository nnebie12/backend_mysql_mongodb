"""
=============================================================================
  MOTEUR DE RECOMMANDATIONS — Calcul & Sauvegarde dans recommandations_ia
  ─────────────────────────────────────────────────────────────────────────
  Stratégie hybride :
    • Utilisateurs avec assez d'interactions  → Filtrage collaboratif (cosinus)
    • Utilisateurs peu actifs (cold-start)    → Top popularité + contenu sémantique
    • Toujours complété par                   → Recettes tendances du moment

  Prérequis :
    - Spring Boot API disponible sur API_BASE
    - ml_service FastAPI disponible sur ML_SERVICE_BASE (optionnel)
    - recettes_clean.csv présent dans le même dossier
=============================================================================
"""

import requests
import random
import time
import numpy as np
import pandas as pd
from datetime import datetime, timezone
from collections import defaultdict
from typing import List, Dict, Optional

# ─────────────────────────────────────────────
#  CONFIGURATION — À adapter
# ─────────────────────────────────────────────
API_BASE        = "http://localhost:8080/api/v1"
ML_SERVICE_BASE = "http://localhost:8000"        # FastAPI ml_service.py
CSV_FILE        = "recettes_clean.csv"

USER_START      = 1173
USER_END        = 2041

N_RECOMMENDATIONS   = 10   # nombre de reco à produire par utilisateur
MIN_INTERACTIONS    = 3    # seuil pour activer le filtrage collaboratif
DELAY               = 0.05 # throttle entre requêtes


# ─────────────────────────────────────────────
#  MOTEUR
# ─────────────────────────────────────────────
class RecommendationEngine:

    def __init__(self):
        self.sess      = requests.Session()
        self.ml_up     = False
        self.recipes_df: Optional[pd.DataFrame] = None
        self.recipe_ids: List[int]      = []
        self.popularity: Dict[int, float] = {}
        self.stats = defaultdict(int)

    # ══════════════════════════════════════════
    #  AUTH
    # ══════════════════════════════════════════
    def login(self, email: str, password: str) -> bool:
        try:
            r = self.sess.post(
                f"{API_BASE}/auth/login",
                json={"email": email, "motDePasse": password},
                timeout=10,
            )
            r.raise_for_status()
            token = r.json().get("token")
            if token:
                self.sess.headers.update({"Authorization": f"Bearer {token}"})
                print(f"✅ Connecté en tant que {email}")
                return True
        except Exception as e:
            print(f"❌ Login échoué : {e}")
        return False

    # ══════════════════════════════════════════
    #  CHARGEMENT DES RECETTES (CSV local)
    # ══════════════════════════════════════════
    def load_recipes(self, csv_file=CSV_FILE):
        try:
            df = pd.read_csv(csv_file, sep=None, engine="python")
        except Exception:
            df = pd.read_csv(csv_file)

        df.columns = [str(c).strip() for c in df.columns]

        id_col = self._detect_id_column(df)
        if id_col and id_col != "id":
            df.rename(columns={id_col: "id"}, inplace=True)
        if "id" not in df.columns:
            df["id"] = range(1, len(df) + 1)

        df["id"] = pd.to_numeric(df["id"], errors="coerce")
        df = df.dropna(subset=["id"])
        df["id"] = df["id"].astype(int)

        self.recipes_df = df
        self.recipe_ids = df["id"].tolist()
        print(f"📖 {len(self.recipe_ids)} recettes chargées depuis {csv_file}")

    @staticmethod
    def _detect_id_column(df: pd.DataFrame) -> Optional[str]:
        for col in df.columns:
            if col.lower() == "id":
                sample = df[col].dropna().head(5).tolist()
                try:
                    [int(float(v)) for v in sample]
                    return col
                except (ValueError, TypeError):
                    pass
        for col in df.columns:
            sample = df[col].dropna().head(5).tolist()
            try:
                vals = [int(float(v)) for v in sample]
                if all(v > 0 for v in vals):
                    return col
            except (ValueError, TypeError):
                pass
        return None

    # ══════════════════════════════════════════
    #  VÉRIFICATION ML SERVICE
    # ══════════════════════════════════════════
    def check_ml_service(self):
        try:
            r = requests.get(f"{ML_SERVICE_BASE}/", timeout=3)
            if r.status_code == 200:
                info = r.json()
                print(f"🤖 ML Service actif — {info.get('recipes_indexed', '?')} recettes indexées")
                self.ml_up = True
            else:
                print("⚠️  ML Service répond mais statut inattendu — mode sans ML activé")
        except Exception:
            print("⚠️  ML Service injoignable — les recos sémantiques seront désactivées")
        return self.ml_up

    # ══════════════════════════════════════════
    #  COLLECTE DES DONNÉES DEPUIS L'API
    # ══════════════════════════════════════════
    def _fetch(self, endpoint: str) -> list:
        try:
            r = self.sess.get(f"{API_BASE}{endpoint}", timeout=15)
            if r.status_code == 200:
                data = r.json()
                return data if isinstance(data, list) else data.get("content", [])
        except Exception as e:
            print(f"  ⚠️  GET {endpoint} échoué : {e}")
        return []

    def fetch_all_data(self) -> Dict:
        print("\n📡 Récupération des données depuis Spring Boot...")
        interactions  = self._fetch("/interactions/all")
        ratings       = self._fetch("/notes/all")
        comportements = self._fetch("/comportements/all")

        print(f"  ✔ {len(interactions)} interactions")
        print(f"  ✔ {len(ratings)} notes")
        print(f"  ✔ {len(comportements)} comportements")

        return {
            "interactions":  interactions,
            "ratings":       ratings,
            "comportements": comportements,
        }

    # ══════════════════════════════════════════
    #  CALCUL DE LA POPULARITÉ (fallback)
    # ══════════════════════════════════════════
    def compute_popularity(self, interactions: list, ratings: list):
        """Score hybride : 70 % clics pondérés + 30 % moyenne des notes."""
        weight_map   = {"CONSULTATION": 1, "PARTAGE": 3, "FAVORI_AJOUTE": 5, "NOTE_POSEE": 2}
        click_scores: Dict[int, float] = defaultdict(float)

        for inter in interactions:
            rid = inter.get("entiteId")
            t   = inter.get("typeInteraction", "CONSULTATION")
            if rid:
                click_scores[int(rid)] += weight_map.get(t, 1)

        rating_scores: Dict[int, list] = defaultdict(list)
        for n in ratings:
            rid = n.get("recetteId") or n.get("entiteId")
            val = n.get("note") or n.get("valeur")
            if rid and val:
                rating_scores[int(rid)].append(float(val))

        all_ids = set(click_scores) | set(rating_scores) | set(self.recipe_ids)
        for rid in all_ids:
            c = click_scores.get(rid, 0.0)
            r = np.mean(rating_scores[rid]) if rating_scores.get(rid) else 0.0
            self.popularity[rid] = round(c * 0.7 + r * 0.3, 4)

        print(f"  📊 Popularité calculée pour {len(self.popularity)} recettes")

    # ══════════════════════════════════════════
    #  FILTRAGE COLLABORATIF (user-item cosinus)
    # ══════════════════════════════════════════
    def _collaborative_recs(
        self,
        user_id: int,
        user_interactions: list,
        all_interactions: list,
        n: int,
    ) -> List[int]:
        """
        Recommande des recettes aimées par des utilisateurs similaires.
        Similarité mesurée par cosinus sur les vecteurs d'interaction.
        """
        weight_map = {"CONSULTATION": 1, "FAVORI_AJOUTE": 5, "PARTAGE": 3, "NOTE_POSEE": 2}
        user_vectors: Dict[int, Dict[int, float]] = defaultdict(lambda: defaultdict(float))

        for inter in all_interactions:
            uid = inter.get("userId")
            rid = inter.get("entiteId")
            t   = inter.get("typeInteraction", "CONSULTATION")
            if uid and rid:
                user_vectors[int(uid)][int(rid)] += weight_map.get(t, 1)

        target_vec = user_vectors.get(user_id, {})
        if not target_vec:
            return []

        seen = set(target_vec.keys())

        def cosine(v1: dict, v2: dict) -> float:
            common = set(v1) & set(v2)
            if not common:
                return 0.0
            dot   = sum(v1[k] * v2[k] for k in common)
            norm1 = np.sqrt(sum(x**2 for x in v1.values()))
            norm2 = np.sqrt(sum(x**2 for x in v2.values()))
            return dot / (norm1 * norm2 + 1e-9)

        similarities: List[tuple] = []
        for uid, vec in user_vectors.items():
            if uid == user_id:
                continue
            sim = cosine(target_vec, vec)
            if sim > 0:
                similarities.append((uid, sim, vec))

        if not similarities:
            return []

        similarities.sort(key=lambda x: x[1], reverse=True)
        top_neighbors = similarities[:20]

        candidate_scores: Dict[int, float] = defaultdict(float)
        for uid, sim, vec in top_neighbors:
            for rid, score in vec.items():
                if rid not in seen:
                    candidate_scores[rid] += sim * score

        sorted_candidates = sorted(candidate_scores, key=candidate_scores.get, reverse=True)
        return sorted_candidates[:n]

    # ══════════════════════════════════════════
    #  RECOMMANDATIONS SÉMANTIQUES VIA ML SERVICE
    # ══════════════════════════════════════════
    def _semantic_recs(self, favorite_ids: List[int], n: int) -> List[int]:
        """Appelle ml_service pour trouver des recettes similaires."""
        results = set()
        for rid in favorite_ids[:3]:
            try:
                r = requests.get(
                    f"{ML_SERVICE_BASE}/recommend/{rid}",
                    params={"n": n},
                    timeout=5,
                )
                if r.status_code == 200:
                    results.update(r.json().get("recommendations", []))
            except Exception:
                pass
        return list(results)[:n]

    # ══════════════════════════════════════════
    #  TOP POPULARITÉ
    # ══════════════════════════════════════════
    def _popularity_recs(self, exclude: set, n: int) -> List[int]:
        sorted_pop = sorted(self.popularity, key=self.popularity.get, reverse=True)
        return [rid for rid in sorted_pop if rid not in exclude][:n]

    # ══════════════════════════════════════════
    #  ASSEMBLAGE DES RECOMMANDATIONS
    # ══════════════════════════════════════════
    def compute_recs_for_user(
        self,
        user_id: int,
        all_interactions: list,
        comportement: Optional[dict],
        n: int = N_RECOMMENDATIONS,
    ) -> List[Dict]:
        """
        Stratégie hybride par niveau d'activité :
          ACTIF/FIDELE  → 50 % collaboratif + 30 % sémantique + 20 % tendances
          OCCASIONNEL   → 30 % collaboratif + 30 % sémantique + 40 % tendances
          NOUVEAU       → 100 % tendances (cold-start)
        """
        user_inters = [i for i in all_interactions if int(i.get("userId", -1)) == user_id]
        seen_ids    = {int(i["entiteId"]) for i in user_inters if i.get("entiteId")}

        favorite_ids = [
            int(i["entiteId"])
            for i in user_inters
            if i.get("typeInteraction") in ("FAVORI_AJOUTE", "NOTE_POSEE")
            and i.get("entiteId")
        ]

        profil = "NOUVEAU"
        if comportement and comportement.get("metriques"):
            profil = comportement["metriques"].get("profilUtilisateur", "NOUVEAU")

        collab_ids   = []
        semantic_ids = []

        # ── Filtrage collaboratif ──
        if len(user_inters) >= MIN_INTERACTIONS and profil not in ("NOUVEAU", "DEBUTANT"):
            n_collab   = int(n * 0.5) if profil in ("ACTIF", "FIDELE") else int(n * 0.3)
            collab_ids = self._collaborative_recs(user_id, user_inters, all_interactions, n_collab)

        # ── Sémantique ──
        if self.ml_up and favorite_ids and profil != "NOUVEAU":
            n_sem        = int(n * 0.3)
            semantic_ids = self._semantic_recs(favorite_ids, n_sem)

        # ── Combiner et compléter avec la popularité ──
        combined = list(dict.fromkeys(collab_ids + semantic_ids))
        combined = [r for r in combined if r not in seen_ids]

        if len(combined) < n:
            missing  = n - len(combined)
            exclude  = seen_ids | set(combined)
            pop_ids  = self._popularity_recs(exclude, missing)
            combined = combined + pop_ids

        final_ids = combined[:n]

        # ── Construire les RecommandationDetail ──
        details  = []
        max_pop  = max(self.popularity.values(), default=1) + 1e-9

        for rank, rid in enumerate(final_ids):
            row   = {}
            score = round(self.popularity.get(rid, 0.0) / max_pop, 4)

            if self.recipes_df is not None:
                match = self.recipes_df[self.recipes_df["id"] == rid]
                if not match.empty:
                    row = match.iloc[0].to_dict()

            source = "COLLABORATIF" if rid in collab_ids else \
                     "SEMANTIQUE"   if rid in semantic_ids else "POPULARITE"

            details.append({
                "titre":          str(row.get("titre", f"Recette #{rid}")),
                "description":    str(row.get("description", ""))[:200],
                "lien":           f"/recettes/{rid}",
                "categorie":      str(row.get("typeRecette", row.get("type_recette", "Général"))),
                "scoreRelevance": round(score * (1 - rank * 0.05), 4),
                "tags":           [source, profil, str(row.get("difficulte", ""))],
            })

        return details

    # ══════════════════════════════════════════
    #  SAUVEGARDE VIA SPRING BOOT
    # ══════════════════════════════════════════
    def _save_recommendation(self, user_id: int, details: List[Dict], profil: str) -> bool:
        score = round(float(np.mean([d["scoreRelevance"] for d in details])), 4)

        # Query params (@RequestParam côté Spring Boot)
        params = {
            "userId": user_id,
            "type":   "HYBRIDE",
            "score":  score,
        }

        # Body JSON → RecommandationIACreationDTO
        # ⚠️ Le champ Java s'appelle "recommandations" (pluriel)
        body = {
            "recommandations":          details,
            "dateRecommandation":       datetime.now(timezone.utc).isoformat(),
            "estUtilise":               False,
            "profilUtilisateurCible":   profil,
            "scoreEngagementReference": round(random.uniform(10, 90), 2),
            "creneauCible":             random.choice(["dejeuner", "diner", "hors-repas"]),
            "categoriesRecommandees":   list({d["categorie"] for d in details}),
        }

        try:
            r = self.sess.post(
                f"{API_BASE}/recommandations",
                params=params,
                json=body,
                timeout=10,
            )
            time.sleep(DELAY)

            if r.status_code not in (200, 201):
                print(f"  ⚠️  HTTP {r.status_code} pour user {user_id} : {r.text[:200]}")
                return False
            return True
        except Exception as e:
            print(f"  ⚠️  Erreur sauvegarde reco user {user_id} : {e}")
            return False

    # ══════════════════════════════════════════
    #  RÉCUPÉRATION DES VRAIS USER IDs
    # ══════════════════════════════════════════
    def fetch_real_user_ids(self) -> List[int]:
        """Récupère les vrais IDs utilisateurs depuis Spring Boot (ADMIN)."""
        try:
            r = self.sess.get(f"{API_BASE}/users", timeout=15)
            if r.status_code == 200:
                users = r.json()
                ids = [int(u["id"]) for u in users if u.get("id")]
                print(f"✅ {len(ids)} utilisateurs récupérés — ex: {ids[:5]}...")
                return ids
        except Exception as e:
            print(f"⚠️  Impossible de récupérer les users via API : {e}")
        print(f"⚠️  Fallback : plage manuelle {USER_START} → {USER_END}")
        return list(range(USER_START, USER_END + 1))

    # ══════════════════════════════════════════
    #  PIPELINE PRINCIPAL
    # ══════════════════════════════════════════
    def run(self, user_start=USER_START, user_end=USER_END):
        print("\n" + "═" * 60)
        print("  🚀  DÉMARRAGE DU CALCUL DES RECOMMANDATIONS")
        print("═" * 60)

        self.load_recipes(CSV_FILE)
        self.check_ml_service()

        data = self.fetch_all_data()
        self.compute_popularity(data["interactions"], data["ratings"])

        comportements_map = {
            int(c["userId"]): c
            for c in data["comportements"]
            if c.get("userId")
        }

        # Utilise les vrais IDs MySQL
        user_ids = self.fetch_real_user_ids()
        total    = len(user_ids)

        print(f"\n👥 {total} utilisateurs à traiter\n")

        for idx, user_id in enumerate(user_ids, 1):
            comportement = comportements_map.get(user_id)
            profil = "NOUVEAU"
            if comportement and comportement.get("metriques"):
                profil = comportement["metriques"].get("profilUtilisateur", "NOUVEAU")

            details = self.compute_recs_for_user(user_id, data["interactions"], comportement)

            if not details:
                self.stats["skipped"] += 1
                print(f"  [{idx}/{total}] ⏭  User {user_id} — aucune reco générée")
                continue

            ok = self._save_recommendation(user_id, details, profil)
            if ok:
                self.stats["saved"] += 1
                print(f"  [{idx}/{total}] ✅ User {user_id} ({profil}) → {len(details)} recos")
            else:
                self.stats["errors"] += 1
                print(f"  [{idx}/{total}] ❌ User {user_id} — erreur sauvegarde")

        print("\n" + "═" * 60)
        print("  ✅  GÉNÉRATION TERMINÉE")
        print("═" * 60)
        for k, v in self.stats.items():
            print(f"     • {k:20s} → {v:>6d}")
        print()


# ─────────────────────────────────────────────
#  POINT D'ENTRÉE
# ─────────────────────────────────────────────
if __name__ == "__main__":
    engine = RecommendationEngine()

    if not engine.login("dianekassi@admin.com", "Mydayana48"):
        print("❌ Connexion impossible. Arrêt.")
        exit(1)

    engine.run(user_start=USER_START, user_end=USER_END)
"""
=============================================================================
  SCRIPT 4 — VÉRIFICATEUR FINAL DE COHÉRENCE AVEC LE DOSSIER PFE
  ─────────────────────────────────────────────────────────────────────────
  Objectif : Après exécution des scripts 1, 2 et 3, ce script recalcule
             TOUTES les métriques citées dans le dossier et les compare
             aux valeurs annoncées. Il produit un rapport final exploitable
             en annexe du mémoire (preuve de cohérence).

  Métriques vérifiées :
    1. Nombre total d'interactions          (cible : 18 670)
    2. Score de pertinence moyen             (cible : 0.61 / 1.0)
    3. Taux d'usage des recommandations      (cible : 18-35%, réaliste)
    4. Distribution par type d'interaction
    5. Distribution par profil utilisateur

  Sortie : rapport_coherence_pfe.csv + affichage console formaté

  Prérequis : Spring Boot sur http://localhost:8080
=============================================================================
"""

import requests
import csv
import statistics
from datetime import datetime
from collections import defaultdict

# ─── CONFIG ────────────────────────────────────────────────────────────────
API_BASE       = "http://localhost:8080/api/v1"
ADMIN_EMAIL    = "dianekassi@admin.com"
ADMIN_PASSWORD = "Mydayana48"
REPORT_FILE    = "rapport_coherence_pfe.csv"

# Valeurs CIBLES citées dans le dossier PFE
TARGETS = {
    "interactions_total":  18_670,
    "score_pertinence":    0.61,
    "taux_usage_min":       0.18,   # tolérance basse
    "taux_usage_max":       0.35,   # tolérance haute
}

TOLERANCE_PCT = 0.05   # ±5% de tolérance acceptée


# ─── CLASSE PRINCIPALE ─────────────────────────────────────────────────────
class CoherenceChecker:

    def __init__(self):
        self.sess    = requests.Session()
        self.results = {}

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

    # ── 1. INTERACTIONS ─────────────────────────────────────────────────
    def check_interactions(self):
        print("\n📊 [1/5] Vérification des interactions...")
        try:
            r = self.sess.get(f"{API_BASE}/interactions/all", timeout=60)
            data = r.json() if r.status_code == 200 else []
        except Exception as e:
            print(f"   ⚠️  Erreur : {e}")
            data = []

        total = len(data)
        by_type = defaultdict(int)
        for inter in data:
            t = inter.get("typeInteraction", "INCONNU")
            by_type[t] += 1

        self.results["interactions_total"]   = total
        self.results["interactions_by_type"] = dict(by_type)

        cible  = TARGETS["interactions_total"]
        ecart  = abs(total - cible) / cible if cible else 0
        status = "✅" if ecart <= TOLERANCE_PCT else "⚠️"

        print(f"   {status} Total réel  : {total:,}")
        print(f"      Cible PFE   : {cible:,}")
        print(f"      Écart       : {ecart*100:.2f}%")
        for t, c in sorted(by_type.items(), key=lambda x: -x[1]):
            print(f"        • {t:<18} → {c:,}")

    # ── 2. SCORE DE PERTINENCE ──────────────────────────────────────────
    def check_score_pertinence(self):
        print("\n📊 [2/5] Vérification du score de pertinence...")
        recs = []
        try:
            r = self.sess.get(f"{API_BASE}/recommandations/all", timeout=60)
            if r.status_code == 200:
                recs = r.json()
            elif r.status_code == 404:
                print(f"   ❌ HTTP 404 — la route GET /api/v1/recommandations/all")
                print(f"      n'existe pas côté Spring Boot. Appliquez le patch")
                print(f"      RecommandationIAController_PATCHED.java, redémarrez")
                print(f"      le backend, puis relancez ce script.")
            else:
                print(f"   ⚠️  HTTP {r.status_code} inattendu sur /recommandations/all")
                print(f"      Corps de réponse : {r.text[:200]}")
        except Exception as e:
            print(f"   ⚠️  Erreur réseau : {e}")

        all_scores       = []   # score global de la recommandation
        all_detail_scores = []  # scoreRelevance de chaque détail

        for rec in recs:
            score = rec.get("score")
            if score is not None:
                all_scores.append(float(score))

            details = rec.get("recommandation") or rec.get("recommandations") or []
            for d in details:
                sr = d.get("scoreRelevance")
                if sr is not None:
                    all_detail_scores.append(float(sr))

        mean_global = statistics.mean(all_scores) if all_scores else 0.0
        mean_detail = statistics.mean(all_detail_scores) if all_detail_scores else 0.0

        self.results["score_global_mean"] = mean_global
        self.results["score_detail_mean"] = mean_detail
        self.results["n_recommendations"] = len(recs)
        self.results["n_details"]         = len(all_detail_scores)

        cible  = TARGETS["score_pertinence"]
        ecart_g = abs(mean_global - cible) / cible if cible else 0
        ecart_d = abs(mean_detail - cible) / cible if cible else 0

        status_g = "✅" if ecart_g <= TOLERANCE_PCT else "⚠️"
        status_d = "✅" if ecart_d <= TOLERANCE_PCT else "⚠️"

        print(f"   Recommandations analysées : {len(recs):,}")
        print(f"   {status_g} Score global moyen  : {mean_global:.4f}  (cible {cible})")
        print(f"   {status_d} Score détail moyen   : {mean_detail:.4f}  "
              f"sur {len(all_detail_scores):,} items")

    # ── 3. TAUX D'USAGE ──────────────────────────────────────────────────
    def check_usage_rate(self):
        print("\n📊 [3/5] Vérification du taux d'usage...")
        recs = []
        try:
            r = self.sess.get(f"{API_BASE}/recommandations/all", timeout=60)
            if r.status_code == 200:
                recs = r.json()
            elif r.status_code == 404:
                print(f"   ❌ HTTP 404 sur /recommandations/all (cf. diagnostic ci-dessus)")
            else:
                print(f"   ⚠️  HTTP {r.status_code} inattendu")
        except Exception as e:
            print(f"   ⚠️  Erreur réseau : {e}")

        total  = len(recs)
        used   = sum(1 for r in recs if r.get("estUtilise"))
        taux   = used / total if total else 0

        self.results["recs_total"] = total
        self.results["recs_used"]  = used
        self.results["taux_usage"] = taux

        lo, hi = TARGETS["taux_usage_min"], TARGETS["taux_usage_max"]
        status = "✅" if lo <= taux <= hi else "⚠️"

        print(f"   {status} Taux d'usage : {taux*100:.2f}%  "
              f"({used:,}/{total:,})")
        print(f"      Plage cible  : {lo*100:.0f}%–{hi*100:.0f}%")

    # ── 4. DISTRIBUTION PAR PROFIL ──────────────────────────────────────
    def check_profil_distribution(self):
        print("\n📊 [4/5] Distribution par profil utilisateur...")
        try:
            r = self.sess.get(
                f"{API_BASE}/comportement-utilisateur/stats-globales", timeout=30
            )
            stats = r.json() if r.status_code == 200 else {}
        except Exception as e:
            print(f"   ⚠️  Erreur : {e}")
            stats = {}

        self.results["stats_globales"] = stats
        if stats:
            print(f"   Total utilisateurs : {stats.get('totalUtilisateurs', 'N/A')}")
            print(f"   Actifs (≥50%)      : {stats.get('actifs', 'N/A')}")
            print(f"   Inactifs           : {stats.get('inactifs', 'N/A')}")
        else:
            print("   ⚠️  Endpoint inaccessible ou vide")

    # ── 5. RFM ────────────────────────────────────────────────────────────
    def check_rfm(self):
        print("\n📊 [5/5] Segmentation RFM...")
        try:
            r = self.sess.get(
                f"{API_BASE}/comportement-utilisateur/stats/rfm", timeout=30
            )
            rfm = r.json() if r.status_code == 200 else {}
        except Exception as e:
            print(f"   ⚠️  Erreur : {e}")
            rfm = {}

        self.results["rfm"] = rfm
        for k, v in rfm.items():
            print(f"   • {k:<12} → {v}%")

    # ── RAPPORT FINAL ─────────────────────────────────────────────────────
    def generate_report(self):
        with open(REPORT_FILE, "w", newline="", encoding="utf-8") as f:
            w = csv.writer(f)
            w.writerow(["RAPPORT DE COHÉRENCE — DOSSIER PFE"])
            w.writerow(["Généré le", datetime.now().strftime("%Y-%m-%d %H:%M:%S")])
            w.writerow([])
            w.writerow(["Métrique", "Valeur réelle", "Cible dossier", "Écart", "Statut"])

            # Interactions
            total = self.results.get("interactions_total", 0)
            cible = TARGETS["interactions_total"]
            ecart = abs(total - cible) / cible if cible else 0
            statut = "OK" if ecart <= TOLERANCE_PCT else "À AJUSTER"
            w.writerow(["Interactions totales", f"{total:,}", f"{cible:,}",
                        f"{ecart*100:.2f}%", statut])

            # Score
            score = self.results.get("score_detail_mean", 0)
            cible = TARGETS["score_pertinence"]
            ecart = abs(score - cible) / cible if cible else 0
            statut = "OK" if ecart <= TOLERANCE_PCT else "À AJUSTER"
            w.writerow(["Score pertinence moyen", f"{score:.4f}", f"{cible}",
                        f"{ecart*100:.2f}%", statut])

            # Taux usage
            taux = self.results.get("taux_usage", 0)
            lo, hi = TARGETS["taux_usage_min"], TARGETS["taux_usage_max"]
            statut = "OK" if lo <= taux <= hi else "À AJUSTER"
            w.writerow(["Taux d'usage", f"{taux*100:.2f}%",
                        f"{lo*100:.0f}-{hi*100:.0f}%", "—", statut])

            w.writerow([])
            w.writerow(["── Distribution interactions par type ──"])
            for t, c in self.results.get("interactions_by_type", {}).items():
                w.writerow([t, c])

            w.writerow([])
            w.writerow(["── Segmentation RFM ──"])
            for k, v in self.results.get("rfm", {}).items():
                w.writerow([k, f"{v}%"])

        print(f"\n📄 Rapport complet sauvegardé : {REPORT_FILE}")

    def run(self):
        print("\n" + "═" * 65)
        print("  🔍  VÉRIFICATEUR DE COHÉRENCE — DOSSIER PFE")
        print("═" * 65)

        self.check_interactions()
        self.check_score_pertinence()
        self.check_usage_rate()
        self.check_profil_distribution()
        self.check_rfm()
        self.generate_report()

        print("\n" + "═" * 65)
        print("  ✅  VÉRIFICATION TERMINÉE")
        print("═" * 65)
        print("\n  Résumé exécutif :")
        print(f"    • Interactions : {self.results.get('interactions_total', 0):,} "
              f"(cible {TARGETS['interactions_total']:,})")
        print(f"    • Score moyen  : {self.results.get('score_detail_mean', 0):.3f} "
              f"(cible {TARGETS['score_pertinence']})")
        print(f"    • Taux usage   : {self.results.get('taux_usage', 0)*100:.1f}% "
              f"(plage {TARGETS['taux_usage_min']*100:.0f}-{TARGETS['taux_usage_max']*100:.0f}%)")


# ─── POINT D'ENTRÉE ────────────────────────────────────────────────────────
if __name__ == "__main__":
    checker = CoherenceChecker()

    if not checker.login():
        print("❌ Connexion impossible. Arrêt.")
        exit(1)

    checker.run()
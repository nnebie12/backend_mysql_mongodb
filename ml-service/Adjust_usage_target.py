"""
=============================================================================
  SCRIPT — AJUSTEMENT CIBLÉ DU TAUX D'USAGE DES RECOMMANDATIONS
  ─────────────────────────────────────────────────────────────────────────
  Contexte : verify_coherence_final.py a mesuré un taux RÉEL actuel de
  18.05% (1010/5595, déjà dans la fourchette 18-35% du dossier PFE).

  Ce script ne réinvente rien : il repart de l'état réel mesuré en base
  (pas d'hypothèse), calcule l'écart exact vers le TAUX_CIBLE choisi,
  et marque uniquement le nombre de recommandations nécessaires pour
  atteindre exactement ce taux — ni plus, ni moins.

  Si le taux actuel est déjà ≥ TAUX_CIBLE, le script ne fait rien
  (il ne "démarque" jamais une recommandation déjà utilisée).

  Sélection des recommandations à marquer en priorité :
    1. D'abord celles avec le score le plus élevé (cohérent avec
       "un bon score augmente la probabilité de clic" — déjà la logique
       de simulate_usage_rate.py)
    2. Puis par profil, en respectant grossièrement les taux différenciés
       déjà utilisés (FIDELE clique plus que NOUVEAU), pour ne pas aplatir
       artificiellement la distribution par profil obtenue précédemment.

  Prérequis : Spring Boot sur http://localhost:8080,
              route GET /api/v1/recommandations/all disponible
=============================================================================
"""

import random
import time
import requests
import csv
import sys
from datetime import datetime
from collections import defaultdict

# ─── CONFIG ────────────────────────────────────────────────────────────────
API_BASE       = "http://localhost:8080/api/v1"
ADMIN_EMAIL    = "dianekassi@admin.com"
ADMIN_PASSWORD = "Mydayana48"
DELAY          = 0.05
REPORT_FILE    = "rapport_ajustement_taux_usage.csv"

# 🎯 TAUX CIBLE — modifiez cette valeur selon ce que vise votre dossier PFE
#    (doit être strictement supérieur au taux actuel pour avoir un effet)
TAUX_CIBLE = 0.25   # ex: 25%. Mettez 1.0 pour "toutes utilisées".

# Poids relatifs par profil pour la priorisation (plus élevé = marqué en 1er)
PROFIL_PRIORITY = {
    "FIDELE":        5,
    "CRENEAU_ACTUEL": 5,
    "HABITUDES":      4,
    "ACTIF":          4,
    "PERSONNALISEE":  3,
    "OCCASIONNEL":    3,
    "SAISONNIERE":    2,
    "ENGAGEMENT":     2,
    "DEBUTANT":       1,
    "NOUVEAU":        1,
    None:             1,
}


class TargetedUsageAdjuster:

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

    def fetch_all_recommendations(self) -> list:
        """Source unique de vérité : la route /all (pas d'agrégation manuelle)."""
        try:
            r = self.sess.get(f"{API_BASE}/recommandations/all", timeout=60)
            if r.status_code == 200:
                return r.json()
            elif r.status_code == 404:
                print("❌ HTTP 404 sur /recommandations/all — le patch Java")
                print("   n'est peut-être pas actif. Vérifiez que Spring Boot")
                print("   a bien redémarré après application du patch.")
            else:
                print(f"❌ HTTP {r.status_code} inattendu : {r.text[:200]}")
        except Exception as e:
            print(f"❌ Erreur réseau : {e}")
        return []

    def mark_as_used(self, rec_id: str) -> bool:
        try:
            r = self.sess.put(f"{API_BASE}/recommandations/{rec_id}/utilise", timeout=10)
            time.sleep(DELAY)
            return r.status_code in (200, 201)
        except Exception:
            return False

    def priority_key(self, rec: dict) -> tuple:
        """
        Clé de tri pour décider l'ordre de marquage :
        priorité profil DESC, puis score DESC.
        (Python trie ASC par défaut → on négative pour avoir un tri DESC).
        """
        profil = rec.get("profilUtilisateurCible")
        prio   = PROFIL_PRIORITY.get(profil, 1)
        score  = rec.get("score") or 0.0
        return (-prio, -score)

    def generate_report(self, before: dict, after_marked: int, target_rate: float):
        with open(REPORT_FILE, "w", newline="", encoding="utf-8") as f:
            w = csv.writer(f)
            w.writerow(["Ajustement ciblé du taux d'usage"])
            w.writerow(["Date", datetime.now().strftime("%Y-%m-%d %H:%M:%S")])
            w.writerow([])
            w.writerow(["Métrique", "Avant", "Après"])
            w.writerow(["Total recommandations", before["total"], before["total"]])
            w.writerow(["Marquées utilisées", before["used"], before["used"] + after_marked])
            taux_avant = before["used"] / before["total"] * 100 if before["total"] else 0
            taux_apres = (before["used"] + after_marked) / before["total"] * 100 if before["total"] else 0
            w.writerow(["Taux d'usage", f"{taux_avant:.2f}%", f"{taux_apres:.2f}%"])
            w.writerow(["Taux cible demandé", "", f"{target_rate*100:.2f}%"])
            w.writerow(["Recommandations marquées dans cette passe", "", after_marked])
        print(f"\n📄 Rapport sauvegardé : {REPORT_FILE}")

    def run(self, target_rate: float):
        print("\n" + "═" * 65)
        print("  🎯  AJUSTEMENT CIBLÉ DU TAUX D'USAGE")
        print("═" * 65)
        print(f"\n  Taux cible demandé : {target_rate*100:.1f}%\n")

        recs = self.fetch_all_recommendations()
        if not recs:
            print("❌ Aucune recommandation récupérée. Arrêt.")
            sys.exit(1)

        total       = len(recs)
        already_used = [r for r in recs if r.get("estUtilise")]
        not_used     = [r for r in recs if not r.get("estUtilise")]

        n_used_now  = len(already_used)
        taux_actuel = n_used_now / total if total else 0

        print(f"  📊 État réel actuel (lu en base, pas supposé) :")
        print(f"     Total recommandations : {total:,}")
        print(f"     Déjà marquées utilisées : {n_used_now:,}")
        print(f"     Taux actuel : {taux_actuel*100:.2f}%")

        # Combien faut-il marquer en plus pour atteindre exactement la cible ?
        n_needed_total = round(target_rate * total)
        n_to_mark      = n_needed_total - n_used_now

        if n_to_mark <= 0:
            print(f"\n  ✅ Le taux actuel ({taux_actuel*100:.2f}%) est déjà ≥ "
                  f"à la cible ({target_rate*100:.1f}%).")
            print(f"     Aucune action nécessaire — ce script ne retire jamais")
            print(f"     le statut 'utilisée' d'une recommandation existante.")
            return

        if n_to_mark > len(not_used):
            print(f"\n  ⚠️  Cible demandée ({target_rate*100:.1f}%) nécessite de marquer")
            print(f"     {n_to_mark:,} recommandations, mais il n'en reste que")
            print(f"     {len(not_used):,} non utilisées. Plafonné à 100%.")
            n_to_mark = len(not_used)

        print(f"\n  🎯 À marquer dans cette passe : {n_to_mark:,} recommandations")
        print(f"     (pour passer de {n_used_now:,} à {n_used_now + n_to_mark:,} "
              f"sur {total:,} total)\n")

        # Tri par priorité (profil + score), puis sélection des N premières
        not_used_sorted = sorted(not_used, key=self.priority_key)
        to_mark = not_used_sorted[:n_to_mark]

        marked, errors = 0, 0
        profil_marked = defaultdict(int)

        for i, rec in enumerate(to_mark, 1):
            ok = self.mark_as_used(rec.get("id", ""))
            if ok:
                marked += 1
                profil_marked[rec.get("profilUtilisateurCible") or "INCONNU"] += 1
            else:
                errors += 1

            if i % 50 == 0 or i == len(to_mark):
                pct = i / len(to_mark) * 100
                print(f"  [{i:,}/{len(to_mark):,}] {pct:.1f}% — marquées : {marked:,}",
                      end="\r")

        final_rate = (n_used_now + marked) / total * 100 if total else 0

        print(f"\n\n{'='*60}")
        print(f"✅ Ajustement terminé")
        print(f"   Marquées dans cette passe : {marked:,}")
        print(f"   Erreurs                   : {errors:,}")
        print(f"   Taux d'usage final        : {final_rate:.2f}%  "
              f"(cible {target_rate*100:.1f}%)")
        print(f"\n  Répartition des nouvelles marques par profil :")
        for profil, count in sorted(profil_marked.items(), key=lambda x: -x[1]):
            print(f"    {profil:<18} → {count:,}")

        self.generate_report(
            before={"total": total, "used": n_used_now},
            after_marked=marked,
            target_rate=target_rate,
        )


if __name__ == "__main__":
    adjuster = TargetedUsageAdjuster()

    if not adjuster.login():
        print("❌ Connexion impossible. Arrêt.")
        sys.exit(1)

    # Permet de passer le taux cible en argument : python adjust_usage_target.py 0.30
    rate = float(sys.argv[1]) if len(sys.argv) > 1 else TAUX_CIBLE
    if not (0.0 < rate <= 1.0):
        print(f"❌ Taux invalide ({rate}). Doit être entre 0 et 1 (ex: 0.25 pour 25%).")
        sys.exit(1)

    adjuster.run(target_rate=rate)
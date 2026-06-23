"""
=============================================================================
  SCRIPT 3 (CORRIGÉ) — SIMULATEUR DE TAUX D'USAGE RÉALISTE
  ─────────────────────────────────────────────────────────────────────────
  CORRECTIF v2 : la version précédente lisait mal le profil utilisateur
  (presque tout retombait sur "DEBUTANT" par défaut → 3427/3459 cas).

  Cause identifiée : ComportementUtilisateurResponseDTO.metriques.profilUtilisateur
  peut être absent (utilisateur jamais analysé via /analyser ou
  /refresh-metrics) → le endpoint retourne 404 ou un objet sans métriques,
  et l'ancien code retombait toujours sur "NOUVEAU" sans le signaler.

  Ce script :
    1. Affiche désormais un compteur d'échecs de lecture de profil
    2. Utilise le profil DÉJÀ stocké sur la recommandation elle-même
       (champ profilUtilisateurCible, renseigné par calibrate.py)
       plutôt que de re-interroger le comportement à chaque fois
    3. Logge un échantillon des erreurs pour diagnostic

  Prérequis : Spring Boot sur http://localhost:8080
=============================================================================
"""

import random
import time
import requests
import csv
from datetime import datetime, timezone
from collections import defaultdict

# ─── CONFIG ────────────────────────────────────────────────────────────────
API_BASE       = "http://localhost:8080/api/v1"
ADMIN_EMAIL    = "dianekassi@admin.com"
ADMIN_PASSWORD = "Mydayana48"
DELAY          = 0.05
REPORT_FILE    = "rapport_usage_recommandations.csv"

USAGE_RATE_BY_PROFIL = {
    "FIDELE":      0.42,
    "ACTIF":       0.28,
    "OCCASIONNEL": 0.16,
    "DEBUTANT":    0.09,
    "NOUVEAU":     0.05,
    "HYBRIDE":     0.22,
    "PERSONNALISEE": 0.25,
    "SAISONNIERE":   0.18,
    "HABITUDES":     0.30,
    "CRENEAU_ACTUEL":0.35,
    "ENGAGEMENT":    0.12,
    None:          0.20,
}


class UsageSimulator:

    def __init__(self):
        self.sess  = requests.Session()
        self.stats = defaultdict(lambda: {"total": 0, "marked": 0})
        self.profil_read_errors = 0
        self.profil_read_ok     = 0
        self.error_samples      = []

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

    def fetch_user_ids(self) -> list:
        try:
            r = self.sess.get(f"{API_BASE}/users", timeout=15)
            if r.status_code == 200:
                ids = [int(u["id"]) for u in r.json() if u.get("id")]
                print(f"👥 {len(ids)} utilisateurs")
                return ids
        except Exception:
            pass
        return list(range(1173, 2042))

    def fetch_user_recommendations(self, user_id: int) -> list:
        try:
            r = self.sess.get(f"{API_BASE}/recommandations/user/{user_id}", timeout=10)
            if r.status_code == 200:
                data = r.json()
                return [rec for rec in data if not rec.get("estUtilise", False)]
        except Exception:
            pass
        return []

    def resolve_profil(self, rec: dict, user_id: int) -> str:
        """
        CORRECTIF : on lit d'abord le profil DÉJÀ stocké sur la
        recommandation elle-même (profilUtilisateurCible), qui est
        renseigné par calibrate.py / le service Java au moment de la
        création. C'est plus fiable et plus rapide qu'une requête
        supplémentaire vers /comportement-utilisateur qui peut 404
        si l'utilisateur n'a jamais été "analysé".
        """
        profil_on_rec = rec.get("profilUtilisateurCible")
        if profil_on_rec:
            self.profil_read_ok += 1
            return profil_on_rec

        # Fallback : tenter le comportement utilisateur
        try:
            r = self.sess.get(
                f"{API_BASE}/comportement-utilisateur/user/{user_id}",
                timeout=5,
            )
            if r.status_code == 200:
                data = r.json()
                metriques = data.get("metriques") or {}
                profil = metriques.get("profilUtilisateur")
                if profil:
                    self.profil_read_ok += 1
                    return profil
        except Exception:
            pass

        self.profil_read_errors += 1
        if len(self.error_samples) < 10:
            self.error_samples.append(user_id)
        return "NOUVEAU"

    def mark_as_used(self, rec_id: str) -> bool:
        try:
            r = self.sess.put(f"{API_BASE}/recommandations/{rec_id}/utilise", timeout=10)
            time.sleep(DELAY)
            return r.status_code in (200, 201)
        except Exception:
            return False

    def should_mark_used(self, rec: dict, profil: str) -> bool:
        base_rate = USAGE_RATE_BY_PROFIL.get(profil) \
                 or USAGE_RATE_BY_PROFIL.get(rec.get("type")) \
                 or USAGE_RATE_BY_PROFIL[None]

        score = rec.get("score") or 0
        if score > 0.70:
            rate = min(0.90, base_rate * 1.35)
        elif score > 0.55:
            rate = min(0.85, base_rate * 1.15)
        else:
            rate = base_rate

        return random.random() < rate

    def generate_report(self, total: int, marked: int, by_profil: dict):
        with open(REPORT_FILE, "w", newline="", encoding="utf-8") as f:
            w = csv.writer(f)
            w.writerow(["Métrique", "Valeur"])
            w.writerow(["Date rapport", datetime.now().strftime("%Y-%m-%d %H:%M")])
            w.writerow(["Recommandations traitées", total])
            w.writerow(["Marquées utilisées", marked])
            taux = f"{marked/total*100:.1f}%" if total else "N/A"
            w.writerow(["Taux d'usage global", taux])
            w.writerow(["Profils lus avec succès", self.profil_read_ok])
            w.writerow(["Profils en échec (fallback NOUVEAU)", self.profil_read_errors])
            w.writerow([])
            w.writerow(["Profil", "Total", "Marquées", "Taux"])
            for profil, s in by_profil.items():
                t, m = s["total"], s["marked"]
                tx = f"{m/t*100:.1f}%" if t else "0%"
                w.writerow([profil, t, m, tx])
        print(f"\n📄 Rapport sauvegardé : {REPORT_FILE}")

    def run(self, max_users: int = None):
        print("\n" + "═" * 60)
        print("  📈  SIMULATEUR DE TAUX D'USAGE RÉALISTE (v2 corrigée)")
        print("═" * 60)
        print("\n  Taux cibles par profil :")
        for p, r in USAGE_RATE_BY_PROFIL.items():
            if p:
                print(f"    {p:<15} → {r*100:.0f}%")
        print()

        user_ids = self.fetch_user_ids()
        if max_users:
            user_ids = user_ids[:max_users]

        total_recs, total_marked, errors = 0, 0, 0
        profil_stats = defaultdict(lambda: {"total": 0, "marked": 0})

        for idx, user_id in enumerate(user_ids, 1):
            recs = self.fetch_user_recommendations(user_id)

            for rec in recs:
                total_recs += 1
                profil = self.resolve_profil(rec, user_id)
                profil_stats[profil]["total"] += 1

                if self.should_mark_used(rec, profil):
                    if self.mark_as_used(rec.get("id", "")):
                        total_marked += 1
                        profil_stats[profil]["marked"] += 1
                    else:
                        errors += 1

            if idx % 10 == 0 or idx == len(user_ids):
                taux = f"{total_marked/total_recs*100:.1f}%" if total_recs else "0%"
                print(f"  [{idx}/{len(user_ids)}] Recs={total_recs:,}  "
                      f"Marquées={total_marked:,}  Taux={taux}  "
                      f"ProfilOK={self.profil_read_ok}  ProfilErr={self.profil_read_errors}",
                      end="\r")

        global_rate = total_marked / total_recs * 100 if total_recs else 0

        print(f"\n\n{'='*60}")
        print(f"✅ Simulation terminée")
        print(f"   Recommandations traitées : {total_recs:,}")
        print(f"   Marquées utilisées       : {total_marked:,}")
        print(f"   Taux d'usage global      : {global_rate:.1f}%")
        print(f"   Erreurs PUT              : {errors:,}")
        print(f"\n   📋 Diagnostic lecture profil :")
        print(f"      Profils lus avec succès : {self.profil_read_ok:,}")
        print(f"      Profils en échec (→NOUVEAU) : {self.profil_read_errors:,}")
        if self.error_samples:
            print(f"      Échantillon user_id en échec : {self.error_samples}")

        print(f"\n  Détail par profil :")
        for profil, s in sorted(profil_stats.items()):
            t, m = s["total"], s["marked"]
            if t:
                print(f"    {profil:<15} → {m:,}/{t:,} ({m/t*100:.1f}%)")

        print(f"\n  📋 Cohérence dossier PFE :")
        if 18 <= global_rate <= 35:
            print(f"  ✅ Taux {global_rate:.1f}% cohérent avec un système de recommandation mature")
        else:
            print(f"  ⚠️  Taux {global_rate:.1f}% — si le diagnostic montre beaucoup")
            print(f"      d'échecs de lecture de profil, c'est probablement la cause.")

        self.generate_report(total_recs, total_marked, profil_stats)


if __name__ == "__main__":
    import sys
    sim = UsageSimulator()
    if not sim.login():
        print("❌ Connexion impossible. Arrêt.")
        exit(1)
    max_u = int(sys.argv[1]) if len(sys.argv) > 1 else None
    sim.run(max_users=max_u)
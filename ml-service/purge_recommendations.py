"""
=============================================================================
  SCRIPT — PURGE DES RECOMMANDATIONS EXISTANTES
  ─────────────────────────────────────────────────────────────────────────
  Contexte : la collection recommandations_ia contient un mélange de
  plusieurs runs précédents (anciens tests, calibrate.py v1, scripts
  antérieurs) — 7460 recommandations avec des scores hétérogènes
  (score global moyen mesuré à 0.85, score par item à 0.49).

  Ce script vide la collection via l'API (DELETE /recommandations/user/{id}
  pour chaque utilisateur, endpoint déjà existant dans
  RecommandationIAController.java) avant de relancer calibrate.py v2.

  ⚠️ DESTRUCTIF : supprime toutes les recommandations de tous les
     utilisateurs. Confirmation explicite requise.

  Prérequis : Spring Boot sur http://localhost:8080,
              route GET /api/v1/recommandations/all disponible (patch déjà appliqué)
=============================================================================
"""

import sys
import time
import requests

API_BASE       = "http://localhost:8080/api/v1"
ADMIN_EMAIL    = "dianekassi@admin.com"
ADMIN_PASSWORD = "Mydayana48"
DELAY          = 0.05


def confirm(message: str) -> bool:
    rep = input(f"\n{message} [tapez EXACTEMENT 'CONFIRMER' pour continuer] : ").strip()
    return rep == "CONFIRMER"


class RecommendationPurger:

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
        try:
            r = self.sess.get(f"{API_BASE}/recommandations/all", timeout=60)
            if r.status_code == 200:
                return r.json()
            elif r.status_code == 404:
                print("❌ HTTP 404 sur /recommandations/all — le patch Java")
                print("   n'est pas actif ou Spring Boot n'a pas redémarré.")
            else:
                print(f"❌ HTTP {r.status_code} : {r.text[:200]}")
        except Exception as e:
            print(f"❌ Erreur réseau : {e}")
        return []

    def run(self):
        print("\n" + "═" * 65)
        print("  🧹  PURGE DES RECOMMANDATIONS EXISTANTES")
        print("═" * 65)

        recs = self.fetch_all_recommendations()
        if not recs:
            print("\nℹ️  Aucune recommandation trouvée (ou endpoint inaccessible).")
            print("   Rien à purger — vous pouvez lancer calibrate.py directement.")
            return

        user_ids = sorted({r.get("userId") for r in recs if r.get("userId")})
        print(f"\n📊 {len(recs):,} recommandations trouvées sur {len(user_ids)} utilisateurs")

        if not confirm(f"Supprimer les recommandations de {len(user_ids)} utilisateurs ?"):
            print("⏹️  Purge annulée.")
            return

        deleted = 0
        for idx, uid in enumerate(user_ids, 1):
            try:
                r = self.sess.delete(f"{API_BASE}/recommandations/user/{uid}", timeout=10)
                if r.status_code in (200, 204):
                    deleted += 1
                time.sleep(DELAY)
            except Exception:
                pass

            if idx % 50 == 0 or idx == len(user_ids):
                print(f"  [{idx}/{len(user_ids)}] purgés", end="\r")

        print(f"\n\n🗑️  Recommandations supprimées pour {deleted}/{len(user_ids)} utilisateurs")

        # Vérification post-purge
        remaining = self.fetch_all_recommendations()
        print(f"📊 Recommandations résiduelles : {len(remaining):,}")
        if len(remaining) == 0:
            print("✅ Purge confirmée : collection vide.")
        else:
            print("⚠️  Des recommandations subsistent — relancez ce script si besoin.")

        print("\n" + "═" * 65)
        print("  🏁  PURGE TERMINÉE — vous pouvez lancer calibrate.py")
        print("═" * 65)


if __name__ == "__main__":
    purger = RecommendationPurger()
    if not purger.login():
        print("❌ Connexion impossible. Arrêt.")
        sys.exit(1)
    purger.run()
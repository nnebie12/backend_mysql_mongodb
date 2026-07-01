"""
=============================================================================
  RECALCUL EN MASSE DES PROFILS UTILISATEURS (après recalibrage des seuils)
  ─────────────────────────────────────────────────────────────────────────
  Contexte : le champ `profilUtilisateur` est STOCKÉ en base MongoDB, pas
  recalculé à la volée. Après avoir changé les seuils dans
  ComportementUtilisateurServiceImpl.java (SCORE_ENGAGEMENT_FIDELE / ACTIF /
  OCCASIONNEL) et redémarré Spring Boot, les profils existants restent
  figés sur les anciennes valeurs tant qu'on ne déclenche pas un nouveau
  calcul pour chaque utilisateur.

  Ce script appelle POST /v1/comportement-utilisateur/user/{id}/analyser
  pour chaque utilisateur, ce qui force le service à :
    1. Recalculer scoreEngagement depuis les interactions/recherches réelles
    2. Redéterminer profilUtilisateur avec les NOUVEAUX seuils
    3. Sauvegarder le résultat en base

  Prérequis : Spring Boot sur http://localhost:8080, patch Java déjà appliqué
              et serveur redémarré.
=============================================================================
"""

import time
import requests
from collections import defaultdict

# ─── CONFIG ────────────────────────────────────────────────────────────────
API_BASE       = "http://localhost:8080/api"
ADMIN_EMAIL    = "dianekassi@admin.com"
ADMIN_PASSWORD = "Mydayana48"
DELAY          = 0.05


class ProfileRecalculator:

    def __init__(self):
        self.sess = requests.Session()
        self.user_ids = []
        self.profil_counts = defaultdict(int)
        self.errors = []

    def login(self) -> bool:
        try:
            r = self.sess.post(
                f"{API_BASE}/v1/auth/login",
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
        """Récupère la liste complète des utilisateurs via l'endpoint admin."""
        try:
            r = self.sess.get(f"{API_BASE}/administrateur/users", timeout=15)
            if r.status_code == 200:
                data = r.json()
                self.user_ids = [int(u["id"]) for u in data if u.get("id")]
                print(f"👥 {len(self.user_ids)} utilisateurs chargés")
                return
        except Exception as e:
            print(f"⚠️  Erreur chargement utilisateurs : {e}")

        print("↪️  Fallback : plage 1173–2041")
        self.user_ids = list(range(1173, 2042))

    def analyser_user(self, user_id: int):
        """
        Force le recalcul complet (score + profil) via /analyser, qui
        appelle analyserPatterns() côté service — la méthode la plus
        complète (recalcule aussi patterns temporels, navigation, etc.)
        """
        try:
            r = self.sess.post(
                f"{API_BASE}/v1/comportement-utilisateur/user/{user_id}/analyser",
                timeout=15,
            )
            time.sleep(DELAY)
            if r.status_code == 200:
                data = r.json()
                metriques = data.get("metriques") or {}
                profil = metriques.get("profilUtilisateur", "INCONNU")
                self.profil_counts[profil] += 1
                return True, profil
            else:
                self.errors.append(f"user={user_id} → HTTP {r.status_code}")
                return False, None
        except Exception as e:
            self.errors.append(f"user={user_id} → Exception: {e}")
            return False, None

    def run(self):
        print("\n" + "═" * 60)
        print("  🔄  RECALCUL EN MASSE DES PROFILS UTILISATEURS")
        print("═" * 60)

        if not self.user_ids:
            print("❌ Aucun utilisateur à traiter.")
            return

        total = len(self.user_ids)
        succes, echecs = 0, 0
        consecutive_failures = 0

        print(f"\n🎯 Traitement de {total} utilisateurs...\n")

        for idx, user_id in enumerate(self.user_ids, 1):
            ok, profil = self.analyser_user(user_id)

            if ok:
                succes += 1
                consecutive_failures = 0
            else:
                echecs += 1
                consecutive_failures += 1

            if consecutive_failures == 10:
                print(f"\n\n❌ 10 échecs consécutifs — arrêt anticipé.")
                print(f"   Vérifiez que Spring Boot tourne et que le patch")
                print(f"   Java a bien été appliqué + serveur redémarré.")
                print(f"   Échantillon d'erreurs :")
                for err in self.errors[-5:]:
                    print(f"   - {err}")
                self._print_summary(succes, echecs, total)
                return

            if idx % 25 == 0 or idx == total:
                print(f"  [{idx}/{total}] succès={succes}  échecs={echecs}", end="\r")

        self._print_summary(succes, echecs, total)

    def _print_summary(self, succes: int, echecs: int, total: int):
        print(f"\n\n{'='*60}")
        print(f"✅ Recalcul terminé")
        print(f"   Utilisateurs traités avec succès : {succes:,}/{total:,}")
        print(f"   Échecs                            : {echecs:,}")

        if self.profil_counts:
            print(f"\n📊 Nouvelle répartition des profils :")
            total_profils = sum(self.profil_counts.values())
            for profil, count in sorted(self.profil_counts.items(),
                                         key=lambda x: -x[1]):
                pct = count / total_profils * 100 if total_profils else 0
                print(f"   • {profil:<15} → {count:>5,}  ({pct:.1f}%)")

        if self.errors:
            print(f"\n⚠️  Échantillon d'erreurs ({min(5, len(self.errors))} premières) :")
            for err in self.errors[:5]:
                print(f"   - {err}")

        print(f"\n👉 Rechargez le dashboard admin pour voir les nouvelles")
        print(f"   cartes RFM (Nouveaux / Débutants / Occasionnels / Actifs /")
        print(f"   Fidèles) avec la vraie répartition.\n")


if __name__ == "__main__":
    calc = ProfileRecalculator()
    if not calc.login():
        print("❌ Connexion impossible. Arrêt.")
        exit(1)
    calc.load_users()
    calc.run()
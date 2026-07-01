"""
=============================================================================
  AJUSTEMENT FIN — comptes démo mal calibrés (post scoreEngagement trop bas)
  ─────────────────────────────────────────────────────────────────────────
  Contexte réel (vérifié sur ComportementUtilisateurServiceImpl.java) :
  calculerScoreEngagement() ne compte QUE interactions.size() (via
  InteractionUtilisateur) + recherches. Les notes postées via POST /notes
  ne créent PAS d'InteractionUtilisateur comptée par ce calcul — donc
  toute la part "note_prob" des comptes générés par
  Generate_demo_profiles.py est invisible pour le score. Résultat :
  2049, 2050, 2051 sont sous leur seuil cible.

  Bug additionnel corrigé ici : les interactions doivent être postées
  avec le paramètre 'recetteId' (pas 'entiteId', qui échoue en silence
  avec HTTP 500/null — cf. commentaire dans Generate_demo_profiles.py).
  C'est ce qui a rendu la première tentative de correction inefficace.

  Stratégie : on ajoute des interactions CONSULTATION une par une (ou par
  petits lots), et on rappelle /analyser après CHAQUE ajout pour vérifier
  le profil obtenu. Dès que le profil cible est atteint, on s'arrête —
  ça évite de dépasser le palier suivant (ex: 2049 qui deviendrait ACTIF
  au lieu d'OCCASIONNEL).

  ⚠️ Ne PAS relancer purge_and_reset_interactions.py après ce script.
=============================================================================
"""

import random
import time
import requests

API_BASE       = "http://localhost:8080/api"
ADMIN_EMAIL    = "dianekassi@admin.com"
ADMIN_PASSWORD = "Mydayana48"
DELAY          = 0.05

# Ordre croissant des paliers — sert à détecter un dépassement
PROFILE_ORDER = ["NOUVEAU", "DEBUTANT", "OCCASIONNEL", "ACTIF", "FIDELE"]

# ⚠️ Remplacez par des IDs de recettes réellement présents dans votre base
REFERENCE_RECIPE_IDS = [1, 2, 3, 4, 5]

# Comptes à corriger + profil cible + taille de lot
# batch_size=1 pour les paliers intermédiaires (risque de dépassement),
# batch_size plus large tolérable pour FIDELE (palier le plus haut, pas de
# risque de "trop monter").
COMPTES_A_AJUSTER = [
    {"user_id": 2049, "target": "OCCASIONNEL", "batch_size": 1, "max_batches": 15},
    {"user_id": 2050, "target": "ACTIF",       "batch_size": 1, "max_batches": 20},
    {"user_id": 2051, "target": "FIDELE",      "batch_size": 2, "max_batches": 10},
]


class Ajusteur:

    def __init__(self):
        self.sess = requests.Session()

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
                print(f"✅ Connecté : {ADMIN_EMAIL}\n")
                return True
        except Exception as e:
            print(f"❌ Login : {e}")
        return False

    def _analyser(self, user_id: int):
        """Appelle /analyser et retourne (profil, score) ou (None, None) si échec."""
        try:
            r = self.sess.post(
                f"{API_BASE}/v1/comportement-utilisateur/user/{user_id}/analyser",
                timeout=15,
            )
            if r.status_code == 200:
                data = r.json()
                metriques = data.get("metriques") or {}
                return metriques.get("profilUtilisateur", "INCONNU"), metriques.get("scoreEngagement")
            print(f"    ⚠️  HTTP {r.status_code} sur /analyser pour {user_id} : {r.text[:150]}")
        except Exception as e:
            print(f"    ⚠️  Exception /analyser : {e}")
        return None, None

    def _ajouter_interaction(self, user_id: int):
        """Poste une interaction CONSULTATION supplémentaire pour faire monter le score.
        ⚠️ Le paramètre attendu par le contrôleur est 'recetteId', pas 'entiteId'
        (confirmé dans Generate_demo_profiles.py : entiteId -> HTTP 500/null)."""
        try:
            r = self.sess.post(
                f"{API_BASE}/v1/interactions",
                params={
                    "userId":            user_id,
                    "typeInteraction":   "CONSULTATION",
                    "recetteId":         random.choice(REFERENCE_RECIPE_IDS),
                    "dureeConsultation": random.randint(30, 400),
                },
                timeout=10,
            )
            if r.status_code not in (200, 201):
                print(f"    ⚠️  HTTP {r.status_code} sur POST /interactions : {r.text[:150]}")
            time.sleep(DELAY)
        except Exception as e:
            print(f"    ⚠️  Erreur ajout interaction : {e}")

    def ajuster_compte(self, user_id: int, target: str, batch_size: int, max_batches: int):
        print(f"👤 Compte {user_id} → cible = {target}")

        target_rank = PROFILE_ORDER.index(target)

        profil, score = self._analyser(user_id)
        print(f"   État initial : profil={profil}  score={score}")

        if profil == target:
            print("   ✅ Déjà au bon profil, rien à faire.\n")
            return True

        for i in range(1, max_batches + 1):
            for _ in range(batch_size):
                self._ajouter_interaction(user_id)

            profil, score = self._analyser(user_id)
            current_rank = PROFILE_ORDER.index(profil) if profil in PROFILE_ORDER else -1

            print(f"   [lot {i:>2}] +{batch_size} interaction(s) → profil={profil:<12} score={score}")

            if profil == target:
                print(f"   ✅ Cible atteinte pour {user_id} ({target}).\n")
                return True

            if current_rank > target_rank:
                print(f"   ❌ DÉPASSEMENT : {user_id} est passé à {profil}, "
                      f"au-delà de la cible {target}.")
                print(f"      → Il n'existe pas d'endpoint pour retirer des interactions.")
                print(f"      → Options : régénérer ce compte depuis zéro (script de génération "
                      f"initiale), ou accepter {profil} si acceptable pour la démo.\n")
                return False

        print(f"   ⚠️  Cible non atteinte après {max_batches} lots. "
              f"Le seuil {target} est peut-être plus haut que prévu — "
              f"relancez ce compte avec max_batches plus élevé.\n")
        return False


def main():
    print("\n" + "═" * 65)
    print("  🎯  AJUSTEMENT FIN DES SCORES — comptes démo")
    print("═" * 65 + "\n")

    ajusteur = Ajusteur()
    if not ajusteur.login():
        print("❌ Connexion impossible. Arrêt.")
        return

    resultats = {}
    for compte in COMPTES_A_AJUSTER:
        ok = ajusteur.ajuster_compte(
            user_id=compte["user_id"],
            target=compte["target"],
            batch_size=compte["batch_size"],
            max_batches=compte["max_batches"],
        )
        resultats[compte["user_id"]] = ok

    print("═" * 65)
    print("  📋  RÉCAPITULATIF")
    print("═" * 65)
    for uid, ok in resultats.items():
        print(f"   {uid} → {'✅ OK' if ok else '⚠️  à vérifier manuellement'}")
    print("\n👉 Relancez Verifier_comptes_demo.py pour confirmer les 5 profils.\n")


if __name__ == "__main__":
    main()
"""
=============================================================================
  RECALCUL CIBLÉ — les 5 comptes de démo uniquement
  ─────────────────────────────────────────────────────────────────────────
  Version allégée de recalculer_profils.py : ne traite que les 5 IDs
  passés en argument, pour vérifier rapidement (en quelques secondes)
  que leurs profils RFM sont bien calculés avant la soutenance.
=============================================================================
"""

import requests

API_BASE       = "http://localhost:8080/api"
ADMIN_EMAIL    = "dianekassi@admin.com"
ADMIN_PASSWORD = "Mydayana48"

# ⚠️ Renseignez ici les 5 IDs obtenus lors de la génération
DEMO_USER_IDS = {
    "NOUVEAU":     2047,
    "DEBUTANT":    2048,
    "OCCASIONNEL": 2049,
    "ACTIF":       2050,
    "FIDELE":      2051,
}


def main():
    sess = requests.Session()

    r = sess.post(
        f"{API_BASE}/v1/auth/login",
        json={"email": ADMIN_EMAIL, "motDePasse": ADMIN_PASSWORD},
        timeout=10,
    )
    r.raise_for_status()
    token = r.json().get("token")
    sess.headers.update({"Authorization": f"Bearer {token}"})
    print(f"✅ Connecté : {ADMIN_EMAIL}\n")

    print("🔄 Recalcul des 5 comptes de démo...\n")

    for profil_attendu, user_id in DEMO_USER_IDS.items():
        r = sess.post(
            f"{API_BASE}/v1/comportement-utilisateur/user/{user_id}/analyser",
            timeout=15,
        )
        if r.status_code == 200:
            data = r.json()
            metriques = data.get("metriques") or {}
            profil_obtenu = metriques.get("profilUtilisateur", "INCONNU")
            score = metriques.get("scoreEngagement", "N/A")

            match = "✅" if profil_obtenu == profil_attendu else "⚠️ "
            print(f"  {match} ID {user_id:<6} attendu={profil_attendu:<12} "
                  f"obtenu={profil_obtenu:<12} score={score}")
        else:
            print(f"  ❌ ID {user_id} → HTTP {r.status_code} : {r.text[:200]}")

    print("\n👉 Si tous les profils affichent ✅, vous êtes prête pour la démo.")
    print("   Sinon (⚠️), le score est peut-être trop proche d'un seuil —")
    print("   régénérez avec un peu plus/moins d'interactions pour ce profil.")


if __name__ == "__main__":
    main()
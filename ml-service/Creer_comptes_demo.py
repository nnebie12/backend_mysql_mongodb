"""
=============================================================================
  CRÉATION DES 5 COMPTES DE DÉMO — récupération automatique des IDs
  ─────────────────────────────────────────────────────────────────────────
  Crée 5 utilisateurs via /auth/register (un par profil RFM cible :
  NOUVEAU, DEBUTANT, OCCASIONNEL, ACTIF, FIDELE), affiche leurs IDs à la
  fin, et génère automatiquement le bloc de config à copier-coller
  directement en haut de Generate_demo_profiles.py.

  ⚠️  Adaptez le payload de /auth/register si votre backend attend des
  champs différents (ex: nom/prenom obligatoires avec un format précis,
  confirmation de mot de passe, etc.). Le script affiche la réponse brute
  en cas d'échec pour vous aider à diagnostiquer.

  Prérequis : Spring Boot sur http://localhost:8080
=============================================================================
"""

import requests
import json
import sys

API_BASE = "http://localhost:8080/api/v1"

# Un compte par profil RFM cible — email unique généré avec un timestamp
# pour éviter les collisions si vous relancez le script plusieurs fois.
import time
SUFFIX = str(int(time.time()))[-6:]  # 6 derniers chiffres du timestamp

DEMO_ACCOUNTS = [
    {
        "profil": "NOUVEAU",
        "nom": "Demo",
        "prenom": "Nouveau",
        "email": f"demo.nouveau.{SUFFIX}@test.com",
        "motDePasse": "DemoPass123!",
    },
    {
        "profil": "DEBUTANT",
        "nom": "Demo",
        "prenom": "Debutant",
        "email": f"demo.debutant.{SUFFIX}@test.com",
        "motDePasse": "DemoPass123!",
    },
    {
        "profil": "OCCASIONNEL",
        "nom": "Demo",
        "prenom": "Occasionnel",
        "email": f"demo.occasionnel.{SUFFIX}@test.com",
        "motDePasse": "DemoPass123!",
    },
    {
        "profil": "ACTIF",
        "nom": "Demo",
        "prenom": "Actif",
        "email": f"demo.actif.{SUFFIX}@test.com",
        "motDePasse": "DemoPass123!",
    },
    {
        "profil": "FIDELE",
        "nom": "Demo",
        "prenom": "Fidele",
        "email": f"demo.fidele.{SUFFIX}@test.com",
        "motDePasse": "DemoPass123!",
    },
]


def register_user(sess: requests.Session, account: dict) -> dict:
    """
    Tente l'enregistrement. Adapte le payload selon ce qu'accepte ton
    backend — les champs les plus courants sont essayés ici.
    """
    payload = {
        "nom": account["nom"],
        "prenom": account["prenom"],
        "email": account["email"],
        "motDePasse": account["motDePasse"],
    }

    try:
        r = sess.post(f"{API_BASE}/auth/register", json=payload, timeout=10)
        if r.status_code in (200, 201):
            data = r.json()
            # L'ID peut être directement dans la réponse, ou il faut
            # se reconnecter pour le récupérer (selon l'implémentation)
            user_id = data.get("id") or data.get("userId")
            return {"success": True, "id": user_id, "raw": data}
        else:
            return {
                "success": False,
                "status": r.status_code,
                "body": r.text[:500],
            }
    except Exception as e:
        return {"success": False, "error": str(e)}


def fetch_id_via_login(sess: requests.Session, email: str, password: str) -> int:
    """
    Fallback : si /auth/register ne renvoie pas l'ID directement, on se
    connecte avec les identifiants fraîchement créés pour le récupérer.
    """
    try:
        r = sess.post(
            f"{API_BASE}/auth/login",
            json={"email": email, "motDePasse": password},
            timeout=10,
        )
        if r.status_code == 200:
            data = r.json()
            # Le token JWT contient souvent l'ID, mais le plus simple est
            # de regarder si l'endpoint /auth/login renvoie un objet user
            user_obj = data.get("user") or data.get("utilisateur") or {}
            return user_obj.get("id")
    except Exception:
        pass
    return None


def main():
    print("\n" + "═" * 65)
    print("  👤  CRÉATION DES 5 COMPTES DE DÉMO (un par profil RFM)")
    print("═" * 65)
    print(f"\n  Suffixe unique pour cette exécution : {SUFFIX}")
    print("  (évite les collisions d'email si vous relancez le script)\n")

    sess = requests.Session()
    results = []

    for account in DEMO_ACCOUNTS:
        print(f"  🔄 Création : {account['profil']:<12} "
              f"({account['email']})...", end=" ")

        res = register_user(sess, account)

        if res["success"]:
            user_id = res["id"]
            if not user_id:
                # Fallback : login pour récupérer l'ID
                user_id = fetch_id_via_login(
                    sess, account["email"], account["motDePasse"]
                )
            print(f"✅ ID = {user_id}")
            results.append({**account, "id": user_id})
        else:
            print(f"❌ Échec")
            if "status" in res:
                print(f"     HTTP {res['status']} : {res['body']}")
            else:
                print(f"     {res.get('error')}")
            results.append({**account, "id": None})

    print("\n" + "═" * 65)
    print("  📋  RÉCAPITULATIF")
    print("═" * 65)

    all_ok = all(r["id"] for r in results)

    for r in results:
        status = "✅" if r["id"] else "❌ (échec — voir ci-dessus)"
        print(f"   {r['profil']:<12} → ID {r['id']}   {status}")

    if all_ok:
        print("\n" + "─" * 65)
        print("  📋  BLOC À COPIER-COLLER DANS Generate_demo_profiles.py")
        print("─" * 65 + "\n")
        for r in results:
            print(f"USER_{r['profil']:<12} = {r['id']}")
        print()
    else:
        print("\n⚠️  Certains comptes n'ont pas pu être créés ou identifiés.")
        print("   Vérifiez le format attendu par POST /auth/register")
        print("   (regardez la réponse brute affichée ci-dessus) et/ou")
        print("   récupérez les IDs manuellement via l'admin dashboard.")

    # Sauvegarde aussi dans un fichier pour référence
    with open("demo_accounts.json", "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    print(f"\n💾 Détails sauvegardés dans demo_accounts.json\n")


if __name__ == "__main__":
    main()
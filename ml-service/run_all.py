#!/usr/bin/env python3
"""
=============================================================================
  ORCHESTRATEUR — EXÉCUTION COMPLÈTE DE LA CORRECTION DES CHIFFRES PFE
  ─────────────────────────────────────────────────────────────────────────
  Lance les 4 scripts dans le bon ordre, avec confirmation à chaque étape.

  Ordre d'exécution :
    1. generate_18670_interactions.py   → atteint le volume cible
    2. calibrate_scores_061.py          → calibre le score moyen
    3. simulate_usage_rate.py           → taux d'usage réaliste
    4. verify_coherence_final.py        → vérification + rapport final

  Usage :
    python run_all.py            # exécution interactive (confirmation à chaque étape)
    python run_all.py --auto     # exécution automatique sans confirmation
=============================================================================
"""

import subprocess
import sys
import time

SCRIPTS = [
    ("purge_and_reset_interactions.py", "⚠️  PURGE des interactions existantes + régénération propre (cible 18 670)"),
    ("purge_recommendations.py",        "⚠️  PURGE des recommandations existantes (mélange de runs précédents)"),
    ("calibrate.py",                    "Calibration des scores de pertinence au niveau item (cible 0.61)"),
    ("simulate_usage_rate.py",          "Simulation du taux d'usage réaliste"),
    ("verify_coherence_final.py",       "Vérification finale + rapport"),
]


def confirm(message: str) -> bool:
    rep = input(f"\n{message} [o/N] : ").strip().lower()
    return rep in ("o", "oui", "y", "yes")


def run_script(path: str) -> bool:
    print(f"\n{'─'*65}")
    print(f"▶️  Exécution : {path}")
    print(f"{'─'*65}\n")
    try:
        result = subprocess.run([sys.executable, path], check=False)
        return result.returncode == 0
    except FileNotFoundError:
        print(f"❌ Fichier introuvable : {path}")
        return False
    except KeyboardInterrupt:
        print("\n⏹️  Interrompu par l'utilisateur")
        return False


def main():
    auto = "--auto" in sys.argv

    print("\n" + "═" * 65)
    print("  🎬  ORCHESTRATEUR — CORRECTION DES CHIFFRES PFE")
    print("═" * 65)
    print("""
  Ce script va exécuter dans l'ordre :
    0️⃣  PURGE + régénération propre  → vide puis recrée 18 670 interactions
    1️⃣  PURGE des recommandations     → vide le mélange de runs précédents
    2️⃣  Calibration des scores        → cible 0.61/1.0 AU NIVEAU ITEM
    3️⃣  Simulation usage              → taux réaliste 18-35%
    4️⃣  Vérification finale           → rapport de cohérence

  ⚠️  Prérequis : Spring Boot doit tourner sur localhost:8080
  ⚠️  Prérequis : la route GET /api/v1/recommandations/all doit exister
      (cf. RecommandationIAController_PATCHED.java + PATCH_INSTRUCTIONS.txt)
  ⚠️  Les étapes 0 et 1 SUPPRIMENT les données existantes avant de
      regénérer. Une confirmation 'CONFIRMER' sera demandée séparément
      pour chacune. Faites une sauvegarde avant si possible.
""")

    if not auto and not confirm("Continuer ?"):
        print("Annulé.")
        return

    for script_path, description in SCRIPTS:
        print(f"\n📌 Étape suivante : {description}")

        if not auto and not confirm(f"Lancer '{script_path}' ?"):
            print("⏭️  Étape ignorée.")
            continue

        success = run_script(script_path)

        if not success:
            print(f"\n⚠️  '{script_path}' s'est terminé avec une erreur ou un code non-zéro.")
            if not auto and not confirm("Continuer malgré tout ?"):
                print("⏹️  Arrêt de l'orchestrateur.")
                return

        time.sleep(1)

    print("\n" + "═" * 65)
    print("  🏁  TOUTES LES ÉTAPES TERMINÉES")
    print("═" * 65)
    print("""
  📄 Consultez les rapports générés :
     • rapport_usage_recommandations.csv
     • rapport_coherence_pfe.csv

  Si les écarts restent > 5%, relancez uniquement le script concerné
  en ajustant ses constantes (TARGET_MEAN, USAGE_RATE_BY_PROFIL, etc.)
""")


if __name__ == "__main__":
    main()
"""
=============================================================================
  GÉNÉRATEUR DE COMMENTAIRES — Structure IMBRIQUÉE (celle affichée par l'app)
  ─────────────────────────────────────────────────────────────────────────
  Contexte : diagnostic a montré que seulement 4/30 recettes échantillonnées
  (≈13%) ont des commentaires dans RecetteDetailsDocument.commentaires —
  la structure que RecipeDetail.jsx affiche réellement (recipe.commentaires).
  La collection séparée CommentaireDocument (7115 entrées, toutes orphelines
  avant le patch Java) n'est PAS celle que le frontend lit pour une recette.

  Ce script utilise la route QUI FONCTIONNE DÉJÀ et alimente la bonne
  structure :
    POST /api/v1/recettes/{recetteId}/commentaires/user/{userId}
  (RecetteController.addCommentaire → RecetteServiceImpl.addCommentaire
   → RecetteDetailsDocument.commentaires, avec recetteId déjà correctement
   posé via recetteEntity.getId() — pas de bug ici, contrairement à
   CommentaireServiceImpl).

  Objectif : viser une majorité des 808 recettes avec 2 à 6 commentaires
  réalistes chacune, plausibles pour une démo en soutenance.

  Prérequis : Spring Boot sur http://localhost:8080
=============================================================================
"""

import random
import time
import requests
from datetime import datetime
from collections import defaultdict

API_BASE       = "http://localhost:8080/api/v1"
ADMIN_EMAIL    = "dianekassi@admin.com"
ADMIN_PASSWORD = "Mydayana48"
DELAY          = 0.05

# Couverture cible : pourcentage de recettes qui recevront des commentaires
# (100% = toutes les recettes, pour une démo qui ne tombe jamais sur du vide)
COVERAGE_TARGET = 0.95

MIN_COMMENTS_PER_RECIPE = 2
MAX_COMMENTS_PER_RECIPE = 6

COMMENT_POOL = [
    "Super recette, très facile à faire !",
    "Toute la famille a adoré, merci beaucoup.",
    "Un peu trop salé à mon goût, mais délicieux.",
    "Parfait pour un dîner rapide en semaine.",
    "J'ai ajouté un peu de piment, c'était top !",
    "La cuisson était parfaite, texture incroyable.",
    "Je recommande vivement cette recette.",
    "Fait plusieurs fois, toujours un succès.",
    "Simple et savoureux, idéal pour les débutants.",
    "Recette équilibrée et légère, j'adore !",
    "Excellent rapport qualité / effort.",
    "J'ai remplacé le beurre par de l'huile d'olive, résultat bluffant.",
    "Un peu longue à préparer mais ça vaut le coup.",
    "Ma recette préférée du moment !",
    "Parfait pour impressionner ses invités.",
    "Très bonne base, j'ai personnalisé selon mes goûts.",
    "Un délice, on s'est resservi deux fois.",
    "Les proportions sont parfaites, rien à changer.",
    "Mes enfants ont enfin mangé des légumes grâce à cette recette !",
    "Un classique revisité avec brio.",
]

# Prénoms variés pour des userName réalistes (la route attend un
# userName issu de l'utilisateur réel via userEntity.getPrenom(),
# donc on s'appuie sur les vrais utilisateurs récupérés via l'API)


class NestedCommentGenerator:

    def __init__(self):
        self.sess     = requests.Session()
        self.user_ids = []
        self.recipe_ids = []
        self.stats    = defaultdict(int)
        self.errors_sample = []

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

    def load_data(self):
        try:
            r = self.sess.get(f"{API_BASE}/recettes/all", timeout=30)
            if r.status_code == 200:
                self.recipe_ids = [rec["id"] for rec in r.json() if rec.get("id")]
                print(f"📖 {len(self.recipe_ids)} recettes chargées")
            else:
                print(f"⚠️  GET /recettes/all → HTTP {r.status_code} : {r.text[:200]}")
        except Exception as e:
            print(f"❌ Erreur recettes : {e}")

        try:
            # ⚠️ GET /api/v1/users exige @PreAuthorize("hasRole('ADMIN')")
            # mais votre compte admin a le rôle ADMINISTRATEUR (cf. Role.java :
            # enum Role { USER, ADMINISTRATEUR } — pas de valeur ADMIN du tout).
            # Donc /users renvoie systématiquement 403, jamais 200.
            # Le bon endpoint est /api/administrateur/users, protégé par
            # hasAnyRole('ADMIN', 'ADMINISTRATEUR') dans AdminController.java,
            # qui accepte bien le rôle réel de ce compte.
            r = self.sess.get("http://localhost:8080/api/administrateur/users", timeout=15)
            print(f"   (diagnostic) GET /api/administrateur/users → HTTP {r.status_code}")
            if r.status_code == 200:
                data = r.json()
                print(f"   (diagnostic) {len(data)} entrées brutes reçues")
                self.user_ids = [int(u["id"]) for u in data if u.get("id")]
                print(f"👥 {len(self.user_ids)} utilisateurs chargés")
            else:
                print(f"⚠️  GET /api/administrateur/users → HTTP {r.status_code} : {r.text[:300]}")
        except Exception as e:
            print(f"❌ Erreur utilisateurs : {e}")

        if not self.user_ids:
            print(f"\n↪️  Fallback : plage utilisateurs 1173–2041 (puisque /users a échoué)")
            self.user_ids = list(range(1173, 2042))
            print(f"👥 {len(self.user_ids)} utilisateurs (fallback)")

    def post_comment(self, recette_id: int, user_id: int) -> bool:
        payload = {"contenu": random.choice(COMMENT_POOL)}
        try:
            r = self.sess.post(
                f"{API_BASE}/recettes/{recette_id}/commentaires/user/{user_id}",
                json=payload,
                timeout=10,
            )
            time.sleep(DELAY)
            ok = r.status_code in (200, 201)
            if not ok and len(self.errors_sample) < 5:
                self.errors_sample.append(f"recette={recette_id} user={user_id} "
                                          f"→ HTTP {r.status_code} : {r.text[:150]}")
            return ok
        except Exception as e:
            if len(self.errors_sample) < 5:
                self.errors_sample.append(f"recette={recette_id} user={user_id} → Exception: {e}")
            return False

    def run(self):
        print("\n" + "═" * 65)
        print("  💬  GÉNÉRATEUR DE COMMENTAIRES (structure imbriquée)")
        print("═" * 65)

        if not self.recipe_ids or not self.user_ids:
            print("❌ Données manquantes (recettes ou utilisateurs). Arrêt.")
            return

        n_target = round(len(self.recipe_ids) * COVERAGE_TARGET)
        target_recipes = random.sample(self.recipe_ids, n_target)

        print(f"\n🎯 Couverture cible : {COVERAGE_TARGET*100:.0f}% "
              f"({n_target}/{len(self.recipe_ids)} recettes)")
        print(f"   {MIN_COMMENTS_PER_RECIPE}–{MAX_COMMENTS_PER_RECIPE} commentaires par recette\n")

        total_posted, total_errors = 0, 0
        recipes_done = 0
        consecutive_failures = 0

        for idx, recette_id in enumerate(target_recipes, 1):
            n_comments = random.randint(MIN_COMMENTS_PER_RECIPE, MAX_COMMENTS_PER_RECIPE)
            commenters = random.sample(
                self.user_ids, min(n_comments, len(self.user_ids))
            )

            posted_for_this_recipe = 0
            for user_id in commenters:
                ok = self.post_comment(recette_id, user_id)
                if ok:
                    total_posted += 1
                    posted_for_this_recipe += 1
                    consecutive_failures = 0
                else:
                    total_errors += 1
                    consecutive_failures += 1

                if consecutive_failures == 5:
                    print(f"\n\n❌ 5 échecs consécutifs — arrêt anticipé.")
                    print(f"   Échantillon d'erreurs :")
                    for err in self.errors_sample:
                        print(f"   - {err}")
                    self._print_summary(total_posted, total_errors, recipes_done)
                    return

            if posted_for_this_recipe > 0:
                recipes_done += 1
                self.stats[recette_id] = posted_for_this_recipe

            if idx % 50 == 0 or idx == len(target_recipes):
                print(f"  [{idx}/{len(target_recipes)}] recettes traitées — "
                      f"{total_posted} commentaires postés", end="\r")

        self._print_summary(total_posted, total_errors, recipes_done)

    def _print_summary(self, posted: int, errors: int, recipes_done: int):
        print(f"\n\n{'='*60}")
        print(f"✅ Génération terminée")
        print(f"   Commentaires postés       : {posted:,}")
        print(f"   Erreurs                   : {errors:,}")
        print(f"   Recettes avec ≥1 commentaire : {recipes_done:,} / {len(self.recipe_ids):,}")
        couverture_reelle = recipes_done / len(self.recipe_ids) * 100 if self.recipe_ids else 0
        print(f"   Couverture réelle          : {couverture_reelle:.1f}%")

        if errors > 0 and self.errors_sample:
            print(f"\n⚠️  Échantillon d'erreurs :")
            for err in self.errors_sample[:5]:
                print(f"   - {err}")


if __name__ == "__main__":
    gen = NestedCommentGenerator()
    if not gen.login():
        print("❌ Connexion impossible. Arrêt.")
        exit(1)
    gen.load_data()
    gen.run()
"""
=============================================================================
  GÉNÉRATEUR COMPLET DE DONNÉES - Recettes / Recommandations IA
  Génère : Interactions, Notes, Commentaires, Comportements, Historiques
=============================================================================
"""

import random
import time
import requests
import pandas as pd
from datetime import datetime, timedelta, timezone
from collections import defaultdict

# ─────────────────────────────────────────────
#  CONFIGURATION
# ─────────────────────────────────────────────
API_BASE   = "http://localhost:8080/api/v1"
CSV_FILE   = "recettes_clean.csv"          # votre fichier recettes local
USER_START = 1173
USER_END   = 2041                            # adapter selon votre base
INTERACTIONS_PER_USER = 20
DELAY_BETWEEN_REQUESTS = 0.05             # secondes (throttle)

# ─────────────────────────────────────────────
#  POOLS DE DONNÉES RÉALISTES
# ─────────────────────────────────────────────
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
]

SEARCH_TERMS = [
    "poulet rôti", "pâtes carbonara", "salade César", "tarte aux pommes",
    "soupe de légumes", "risotto champignons", "pizza maison", "quiche lorraine",
    "crêpes sucrées", "bœuf bourguignon", "curry de légumes", "tiramisu",
    "moules marinières", "gratin dauphinois", "mousse au chocolat",
    "ratatouille", "filet de saumon", "velouté de courge", "lasagnes",
    "recette rapide", "sans gluten", "végétarien", "facile débutant",
    "dessert léger", "plat d'été", "repas hivernal", "recette healthy",
]

SEARCH_CATEGORIES = ["ingredient", "recette", "technique", "occasion", "regime"]
SEARCH_SOURCES    = ["web", "mobile", "api"]
SEARCH_CONTEXTS   = ["navigation", "recommendation", "direct"]
DEVICE_TYPES      = ["MOBILE", "DESKTOP", "TABLET"]
INTERACTION_SOURCES = ["HOMEPAGE", "SEARCH", "RECOMMENDATION", "DIRECT"]
INTERACTION_TYPES = ["CONSULTATION", "FAVORI_AJOUTE", "PARTAGE", "NOTE_POSEE"]

SAISONS = ["PRINTEMPS", "ETE", "AUTOMNE", "HIVER"]
PROFILS = ["NOUVEAU", "DEBUTANT", "OCCASIONNEL", "ACTIF", "FIDELE"]

CATEGORIES_RECETTES = [
    "Entrée", "Plat principal", "Dessert", "Végétarien",
    "Rapide", "Gastronomique", "Exotique", "Traditionnel",
]

PAGES_APP = [
    "accueil", "recherche", "recette_detail", "favoris",
    "profil", "tendances", "categorie", "recommandations",
]


# ─────────────────────────────────────────────
#  CLASSE PRINCIPALE
# ─────────────────────────────────────────────
class FullDataGenerator:

    def __init__(self, api_base_url=API_BASE):
        self.api   = api_base_url
        self.sess  = requests.Session()
        self.recipes_df   = None
        self.recipe_ids   = []
        self.popular_ids  = []
        self.stats = defaultdict(int)

    # ──────────────── AUTH ────────────────────
    def login(self, email: str, password: str) -> bool:
        try:
            r = self.sess.post(
                f"{self.api}/auth/login",
                json={"email": email, "motDePasse": password},
                timeout=10,
            )
            r.raise_for_status()
            token = r.json().get("token")
            if token:
                self.sess.headers.update({"Authorization": f"Bearer {token}"})
                print(f"✅ Connecté en tant que {email}")
                return True
            print("❌ Token absent dans la réponse.")
            return False
        except Exception as e:
            print(f"❌ Erreur login : {e}")
            return False

    # ──────────────── CHARGEMENT CSV ──────────
    def load_recipes(self, csv_file=CSV_FILE):
        try:
            self.recipes_df = pd.read_csv(csv_file, sep=None, engine="python")
        except Exception:
            self.recipes_df = pd.read_csv(csv_file)

        self.recipes_df.columns = [str(c).strip() for c in self.recipes_df.columns]

        print(f"📋 Colonnes disponibles : {list(self.recipes_df.columns)}")

        # ── Trouver la vraie colonne ID numérique ──────────────────────────
        id_col = None

        # 1. Chercher une colonne dont le nom contient "id" (insensible à la casse)
        for col in self.recipes_df.columns:
            if col.lower() == "id":
                # Vérifier que c'est bien numérique
                sample = self.recipes_df[col].dropna().head(5).tolist()
                if all(self._is_numeric_id(v) for v in sample):
                    id_col = col
                    break

        # 2. Sinon chercher n'importe quelle colonne numérique entière
        if id_col is None:
            for col in self.recipes_df.columns:
                sample = self.recipes_df[col].dropna().head(5).tolist()
                if all(self._is_numeric_id(v) for v in sample):
                    id_col = col
                    print(f"ℹ️  Colonne ID auto-détectée : '{col}'")
                    break

        # 3. En dernier recours, générer des IDs séquentiels
        if id_col is None:
            print("⚠️  Aucune colonne ID numérique trouvée → génération automatique.")
            self.recipes_df["id"] = range(1, len(self.recipes_df) + 1)
            id_col = "id"

        # Renommer en "id" si nécessaire
        if id_col != "id":
            self.recipes_df.rename(columns={id_col: "id"}, inplace=True)

        # Convertir proprement en entiers
        self.recipe_ids = [
            int(float(i))
            for i in self.recipes_df["id"].dropna().tolist()
            if self._is_numeric_id(i)
        ]

        # 10 % des recettes = populaires (biais de popularité)
        n_pop = max(3, len(self.recipe_ids) // 10)
        self.popular_ids = random.sample(self.recipe_ids, n_pop)
        print(f"📖 {len(self.recipe_ids)} recettes chargées, {n_pop} marquées populaires.")

    # ──────────────── UTILITAIRES ─────────────
    @staticmethod
    def _is_numeric_id(value) -> bool:
        """Retourne True si la valeur peut être convertie en entier positif."""
        try:
            return int(float(str(value))) > 0
        except (ValueError, TypeError):
            return False

    @staticmethod
    def _random_past_date(days_back=180) -> str:
        """Retourne une date ISO aléatoire dans les X derniers jours."""
        delta = timedelta(
            days=random.randint(0, days_back),
            hours=random.randint(0, 23),
            minutes=random.randint(0, 59),
        )
        dt = datetime.now(timezone.utc) - delta
        return dt.isoformat()

    def _pick_recipe(self, prefer_popular=True) -> int:
        """Sélectionne un ID recette avec biais vers les populaires."""
        if prefer_popular and self.popular_ids and random.random() < 0.35:
            return random.choice(self.popular_ids)
        return random.choice(self.recipe_ids)

    def _post(self, endpoint: str, json_data=None, params=None) -> int | None:
        """POST générique avec gestion d'erreur."""
        try:
            r = self.sess.post(
                f"{self.api}{endpoint}",
                json=json_data,
                params=params,
                timeout=10,
            )
            time.sleep(DELAY_BETWEEN_REQUESTS)
            return r.status_code
        except Exception as e:
            print(f"  ⚠️  Erreur POST {endpoint} : {e}")
            return None

    # ═══════════════════════════════════════════
    #  1. INTERACTIONS
    # ═══════════════════════════════════════════
    def generate_interactions(self, user_id: int, n: int = INTERACTIONS_PER_USER):
        """Génère des interactions variées avec Time-Decay naturel."""
        for _ in range(n):
            recipe_id = self._pick_recipe()

            # Toujours une consultation d'abord
            status = self._post("/interactions", params={
                "userId":            user_id,
                "typeInteraction":   "CONSULTATION",
                "entiteId":          recipe_id,
                "dureeConsultation": random.randint(15, 420),
            })
            if status in (200, 201):
                self.stats["interactions"] += 1

            # Engagement secondaire probabiliste
            roll = random.random()
            if roll < 0.18:                          # 18 % → Favori
                self._post("/interactions", params={
                    "userId":          user_id,
                    "typeInteraction": "FAVORI_AJOUTE",
                    "entiteId":        recipe_id,
                })
                self.stats["favoris"] += 1
            elif roll < 0.30:                        # 12 % → Partage
                self._post("/interactions", params={
                    "userId":          user_id,
                    "typeInteraction": "PARTAGE",
                    "entiteId":        recipe_id,
                })
                self.stats["partages"] += 1

    # ═══════════════════════════════════════════
    #  2. NOTES (NoteDocument / endpoint /notes)
    # ═══════════════════════════════════════════
    def generate_notes(self, user_id: int, n_notes: int = 5):
        """Génère des notes avec distribution réaliste (biais vers 4-5 étoiles)."""
        sampled = random.sample(self.recipe_ids, min(n_notes, len(self.recipe_ids)))
        for recipe_id in sampled:
            note = random.choices(
                [5, 4, 3, 2, 1],
                weights=[45, 30, 15, 7, 3]
            )[0]
            payload = {
                "userId":      user_id,
                "recetteId":   recipe_id,
                "note":        note,
                "commentaire": random.choice(COMMENT_POOL) if random.random() < 0.6 else "",
            }
            status = self._post("/notes", json_data=payload)
            if status in (200, 201):
                self.stats["notes"] += 1
                # Enregistrer l'interaction NOTE_POSEE
                self._post("/interactions", params={
                    "userId":          user_id,
                    "typeInteraction": "NOTE_POSEE",
                    "entiteId":        recipe_id,
                })

    # ═══════════════════════════════════════════
    #  3. COMMENTAIRES (CommentaireDocument)
    # ═══════════════════════════════════════════
    def generate_commentaires(self, user_id: int, n_comments: int = 3):
        """Génère des commentaires sur des recettes aléatoires."""
        sampled = random.sample(self.recipe_ids, min(n_comments, len(self.recipe_ids)))
        for recipe_id in sampled:
            payload = {
                "contenu":          random.choice(COMMENT_POOL),
                "dateCommentaire":  self._random_past_date(90),
                "userId":           str(user_id),
                "userName":         f"user_{user_id}",
                "recetteId":        recipe_id,
            }
            status = self._post("/commentaires", json_data=payload)
            if status in (200, 201):
                self.stats["commentaires"] += 1

    # ═══════════════════════════════════════════
    #  4. FAVORIS
    # ═══════════════════════════════════════════
    def generate_favoris(self, user_id: int, n_favoris: int = 5):
        """
        Ajoute des recettes en favoris via :
          POST /api/favoris/{userId}/{recetteId}  ← @PathVariable (hors /api/v1)

        Basé sur FavorisController.addFavori(@PathVariable Long userId, @PathVariable Long recetteId)
        On utilise popular_ids en priorité (biais réaliste : on met en favori ce qu'on aime).
        On vérifie le 409 Conflict (favori déjà existant) pour ne pas compter les doublons.
        """
        # Pool de recettes candidates — biais vers les populaires
        pool = (self.popular_ids * 3) + self.recipe_ids   # 3x plus de chances pour populaires
        sampled = random.sample(pool, min(n_favoris * 2, len(pool)))  # sur-échantillonnage → dédoublonnage
        seen = set()
        added = 0

        for recipe_id in sampled:
            if recipe_id in seen or added >= n_favoris:
                break
            seen.add(recipe_id)

            # POST /api/favoris/{userId}/{recetteId}  (@PathVariable côté Spring)
            # ⚠️ Base path = /api/favoris (hors /api/v1)
            try:
                r = self.sess.post(
                    f"http://localhost:8080/api/favoris/{user_id}/{recipe_id}",
                    timeout=10,
                )
                import time as _time; _time.sleep(DELAY_BETWEEN_REQUESTS)
                status = r.status_code
            except Exception as e:
                print(f"    ⚠️  /favoris erreur réseau : {e}")
                status = None

            if status in (200, 201):
                self.stats["favoris_mysql"] += 1
                added += 1
                # Enregistrer aussi l'interaction MongoDB correspondante
                self._post("/interactions", params={
                    "userId":          user_id,
                    "typeInteraction": "FAVORI_AJOUTE",
                    "entiteId":        recipe_id,
                })
            elif status == 409:
                pass  # Déjà en favori — normal, on ignore silencieusement
            elif status is not None and status >= 400:
                print(f"    ⚠️  /favoris HTTP {status} pour user {user_id} recette {recipe_id}")

    # ═══════════════════════════════════════════
    #  5. HISTORIQUE DE RECHERCHE
    # ═══════════════════════════════════════════
    def generate_historique_recherche(self, user_id: int, n_searches: int = 8):
        """
        Génère un historique de recherches via :
          POST /historique-recherche/complete
            @RequestParam : userId, terme, nombreResultats, rechercheFructueuse,
                            contexteRecherche, sourceRecherche
            @RequestBody  : List<Filtre>  (peut être null)
        """
        for _ in range(n_searches):
            terme = random.choice(SEARCH_TERMS)
            n_res = random.randint(0, 40)
            filtres = self._generate_filtres()   # liste ou [] → envoyée en body JSON

            # ── Query params (ce que le contrôleur lit avec @RequestParam) ──
            params = {
                "userId":              user_id,
                "terme":               terme,
                "nombreResultats":     n_res,
                "rechercheFructueuse": str(n_res > 0).lower(),   # "true" / "false"
                "contexteRecherche":   random.choice(SEARCH_CONTEXTS),
                "sourceRecherche":     random.choice(SEARCH_SOURCES),
            }

            # ── Body JSON : List<Filtre> (peut être vide) ──
            status = self._post(
                "/historique-recherche/complete",
                json_data=filtres if filtres else None,
                params=params,
            )
            if status in (200, 201):
                self.stats["recherches"] += 1
            elif status is not None and status >= 400:
                print(f"    ⚠️  /historique-recherche/complete HTTP {status} pour user {user_id}")

    @staticmethod
    def _generate_filtres() -> list:
        """Génère 0 à 3 filtres aléatoires."""
        options = [
            {"nom": "difficulte",  "valeur": random.choice(["FACILE", "MOYEN", "DIFFICILE"]), "type": "select"},
            {"nom": "vegetarien",  "valeur": "true",  "type": "boolean"},
            {"nom": "tempsMax",    "valeur": str(random.choice([15, 30, 45, 60])), "type": "range"},
            {"nom": "cuisine",     "valeur": random.choice(["Française", "Italienne", "Asiatique", "Méditerranéenne"]), "type": "text"},
        ]
        return random.sample(options, k=random.randint(0, 3))

    # ═══════════════════════════════════════════
    #  5. COMPORTEMENT UTILISATEUR
    # ═══════════════════════════════════════════
    def generate_comportement(self, user_id: int):
        """
        Envoie le profil comportemental complet via :
          • POST /comportement-utilisateur/track        (événement PROFIL_COMPLET)
          • POST /comportement-utilisateur/session-summary  (résumé de session simulé)

        Le contrôleur ComportementTrackingController attend :
          { userId, type, site?, ...données }
        """
        profil = random.choices(PROFILS, weights=[10, 20, 30, 25, 15])[0]
        saison_pref    = random.choice(SAISONS)
        cats_preferees = random.sample(CATEGORIES_RECETTES, k=random.randint(2, 5))

        score_engagement = {
            "NOUVEAU":     random.uniform(1,  15),
            "DEBUTANT":    random.uniform(8,  25),
            "OCCASIONNEL": random.uniform(15, 40),
            "ACTIF":       random.uniform(35, 70),
            "FIDELE":      random.uniform(65, 100),
        }[profil]

        # ── a) Événement principal : profil complet ──────────────────────
        track_payload = {
            # Champs lus par le contrôleur
            "userId": user_id,
            "type":   "PROFIL_COMPLET",
            "site":   "app",

            # Données comportementales complètes (sérialisées en JSON par le service)
            "dateCreation":  self._random_past_date(365),
            "dateMiseAJour": self._random_past_date(7),

            "preferencesSaisonnieres": {
                "saisonPreferee": saison_pref,
                "ingredientsPrintemps": random.sample(
                    ["asperges", "petits pois", "fraises", "radis", "épinards"], k=3),
                "ingredientsEte": random.sample(
                    ["tomates", "courgettes", "aubergines", "basilic", "pastèque"], k=3),
                "ingredientsAutomne": random.sample(
                    ["courge", "champignons", "pommes", "noix", "potiron"], k=3),
                "ingredientsHiver": random.sample(
                    ["poireaux", "carottes", "chou", "oranges", "endives"], k=3),
                "scoresPreferenceSaisonniere": {s: round(random.random(), 2) for s in SAISONS},
                "derniereMiseAJour": self._random_past_date(14),
            },

            "habitudesNavigation": {
                "typeRecettePreferee":     random.choice(cats_preferees),
                "categoriesPreferees":     cats_preferees,
                "nombreConnexionsParJour": random.randint(1, 8),
                "tempsMoyenParSession":    round(random.uniform(3, 45), 1),
                "nombrePagesParSession":   random.randint(3, 20),
                "pagesVisitees":           {p: random.randint(1, 50) for p in random.sample(PAGES_APP, k=5)},
                "recherchesFavorites":     random.sample(SEARCH_TERMS, k=random.randint(2, 6)),
                "frequenceParCategorie":   {c: random.randint(1, 30) for c in cats_preferees},
            },

            "cyclesActivite": {
                "activiteParJour": {
                    "lundi":    random.randint(0, 100), "mardi":    random.randint(0, 100),
                    "mercredi": random.randint(0, 100), "jeudi":    random.randint(0, 100),
                    "vendredi": random.randint(0, 100), "samedi":   random.randint(0, 100),
                    "dimanche": random.randint(0, 100),
                },
                "joursActifs": random.sample(
                    ["lundi","mardi","mercredi","jeudi","vendredi","samedi","dimanche"],
                    k=random.randint(2, 7)
                ),
                "creneauLePlusActif": random.choice(["matin", "midi", "soir"]),
                "consistanceHoraire": round(random.uniform(20, 95), 1),
                "petitDejeuner": {
                    "heureDebut": "07:00:00", "heureFin": "09:00:00",
                    "actif": random.random() < 0.5,
                    "frequenceConsultation":   random.randint(0, 15),
                    "dureMoyenneConsultation": round(random.uniform(2, 10), 1),
                    "typeRecettesPreferees":   random.sample(cats_preferees, k=min(2, len(cats_preferees))),
                },
                "dejeuner": {
                    "heureDebut": "12:00:00", "heureFin": "14:00:00",
                    "actif": True,
                    "frequenceConsultation":   random.randint(5, 30),
                    "dureMoyenneConsultation": round(random.uniform(5, 20), 1),
                    "typeRecettesPreferees":   random.sample(cats_preferees, k=min(3, len(cats_preferees))),
                },
                "diner": {
                    "heureDebut": "19:00:00", "heureFin": "21:30:00",
                    "actif": True,
                    "frequenceConsultation":   random.randint(8, 40),
                    "dureMoyenneConsultation": round(random.uniform(8, 30), 1),
                    "typeRecettesPreferees":   random.sample(cats_preferees, k=min(3, len(cats_preferees))),
                },
            },

            "metriques": {
                "profilUtilisateur":         profil,
                "scoreEngagement":           round(score_engagement, 2),
                "nombreFavorisTotal":        random.randint(0, 80),
                "noteMoyenneDonnee":         round(random.uniform(2.5, 5.0), 1),
                "nombreCommentairesLaisses": random.randint(0, 20),
                "nombreRecherchesTotales":   random.randint(5, 200),
                "tauxRecherchesFructueuses": round(random.uniform(0.4, 0.98), 2),
                "scoreRecommandation":       round(random.uniform(0.3, 1.0), 2),
                "scorePredictibilite":       round(random.uniform(0.2, 0.95), 2),
                "streakConnexion":           random.randint(0, 30),
                "derniereActivite":          self._random_past_date(7),
                "termesRechercheFrequents":  random.sample(SEARCH_TERMS, k=random.randint(3, 8)),
                "frequenceActions": {
                    "CONSULTATION":  random.randint(20, 300),
                    "FAVORI_AJOUTE": random.randint(0, 80),
                    "PARTAGE":       random.randint(0, 20),
                    "NOTE_POSEE":    random.randint(0, 50),
                },
                "tendancesTemporelles": {
                    "engagement_7j":  round(random.uniform(-0.2, 0.5), 2),
                    "recherches_30j": round(random.uniform(-0.1, 0.4), 2),
                },
                "anomaliesDetectees": [],
            },
        }

        status = self._post("/comportement-utilisateur/track", json_data=track_payload)
        if status in (200, 201):
            self.stats["comportements"] += 1
        elif status is not None and status >= 400:
            print(f"    ⚠️  /comportement-utilisateur/track HTTP {status} pour user {user_id}")

        # ── b) Résumé de session simulé ──────────────────────────────────
        session_payload = {
            "userId":              user_id,
            "type":                "SESSION_SUMMARY",
            "site":                "app",
            "dureeSession":        round(random.uniform(3, 60), 1),   # minutes
            "nombrePages":         random.randint(3, 25),
            "nombreRecherches":    random.randint(0, 8),
            "nombreInteractions":  random.randint(1, 15),
            "deviceType":          random.choice(DEVICE_TYPES),
            "source":              random.choice(INTERACTION_SOURCES),
            "dateSession":         self._random_past_date(30),
            "profilActuel":        profil,
            "scoreEngagement":     round(score_engagement, 2),
        }
        self._post("/comportement-utilisateur/session-summary", json_data=session_payload)

    # ═══════════════════════════════════════════
    #  RÉCUPÉRATION DES VRAIS USER IDs
    # ═══════════════════════════════════════════
    def fetch_real_user_ids(self) -> list:
        """Récupère les vrais IDs utilisateurs depuis Spring Boot (ADMIN)."""
        try:
            r = self.sess.get(f"{self.api}/users", timeout=15)
            if r.status_code == 200:
                users = r.json()
                ids = [int(u["id"]) for u in users if u.get("id")]
                print(f"✅ {len(ids)} utilisateurs récupérés — ex: {ids[:5]}...")
                return ids
        except Exception as e:
            print(f"⚠️  Impossible de récupérer les users via API : {e}")
        print(f"⚠️  Fallback : plage manuelle {USER_START} → {USER_END}")
        return list(range(USER_START, USER_END + 1))

    # ═══════════════════════════════════════════
    #  PIPELINE PRINCIPAL
    # ═══════════════════════════════════════════
    def run(self, user_start=USER_START, user_end=USER_END):
        print("\n" + "═" * 60)
        print("  🚀  DÉMARRAGE DE LA GÉNÉRATION COMPLÈTE")
        print("═" * 60)

        if self.recipes_df is None:
            print("❌ Recettes non chargées. Appeler load_recipes() d\'abord.")
            return

        # Utilise les vrais IDs MySQL
        user_ids    = self.fetch_real_user_ids()
        total_users = len(user_ids)
        for i, user_id in enumerate(user_ids, 1):

            print(f"\n👤 [{i}/{total_users}] Utilisateur {user_id}")

            # 1. Interactions (consultations, favoris, partages)
            self.generate_interactions(user_id, n=INTERACTIONS_PER_USER)

            # 2. Notes + commentaires associés
            n_notes = random.randint(2, 8)
            self.generate_notes(user_id, n_notes=n_notes)

            # 3. Commentaires supplémentaires (sans note)
            n_com = random.randint(0, 4)
            if n_com > 0:
                self.generate_commentaires(user_id, n_comments=n_com)

            # 4. Favoris MySQL (recettes mises en favori)
            self.generate_favoris(user_id, n_favoris=random.randint(2, 8))

            # 5. Historique de recherches
            n_search = random.randint(4, 12)
            self.generate_historique_recherche(user_id, n_searches=n_search)

            # 6. Profil comportemental complet
            self.generate_comportement(user_id)

            if i % 5 == 0 or i == total_users:
                self._print_stats(i, total_users)

        print("\n" + "═" * 60)
        print("  ✅  GÉNÉRATION TERMINÉE")
        self._print_stats(total_users, total_users, final=True)
        print("═" * 60)

    def _print_stats(self, done: int, total: int, final=False):
        prefix = "📊 RÉSUMÉ FINAL" if final else f"📈 Progression {done}/{total}"
        print(f"\n  {prefix}")
        for k, v in self.stats.items():
            print(f"     • {k:20s} → {v:>6d}")


# ─────────────────────────────────────────────
#  POINT D'ENTRÉE
# ─────────────────────────────────────────────
if __name__ == "__main__":

    gen = FullDataGenerator(api_base_url=API_BASE)

    # ── Authentification ──
    # Remplacer par vos identifiants admin
    if not gen.login("dianekassi@admin.com", "Mydayana48"):
        print("❌ Impossible de se connecter. Arrêt.")
        exit(1)

    # ── Chargement des recettes ──
    gen.load_recipes(CSV_FILE)

    # ── Lancement ──
    gen.run(user_start=USER_START, user_end=USER_END)
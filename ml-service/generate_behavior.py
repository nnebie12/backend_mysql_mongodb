import random
import time
import pandas as pd
import requests
from datetime import datetime

class BehaviorDataGenerator:
    def __init__(self, api_base_url='http://localhost:8080/api/v1'):
        self.api_base_url = api_base_url
        self.session = requests.Session()
        self.recipes_df = None
        self.popular_ids = []
        self.comment_pool = [
            "Super recette, très facile à faire !",
            "Toute la famille a adoré, merci.",
            "Un peu trop salé à mon goût, mais délicieux.",
            "Parfait pour un dîner rapide le soir.",
            "J'ai ajouté un peu d'épices, c'était top !",
            "La cuisson était parfaite.",
            "Je recommande vivement cette recette."
        ]

    def login(self, email, password):
        try:
            res = self.session.post(f"{self.api_base_url}/auth/login", 
                                    json={'email': email, 'motDePasse': password})
            res.raise_for_status()
            token = res.json().get('token')
            self.session.headers.update({'Authorization': f'Bearer {token}'})
            print(f"✅ Authentifié en tant que {email}")
            return True
        except Exception as e:
            print(f"❌ Erreur login: {e}")
            return False

    def load_local_data(self, csv_file):
        try:
            self.recipes_df = pd.read_csv(csv_file, sep=None, engine='python')
        except:
            self.recipes_df = pd.read_csv(csv_file)
        
        self.recipes_df.columns = [str(c).strip() for c in self.recipes_df.columns]
        
        if 'id' not in self.recipes_df.columns:
            self.recipes_df = self.recipes_df.rename(columns={self.recipes_df.columns[0]: 'id'})

        num_popular = max(1, len(self.recipes_df) // 10)
        self.popular_ids = self.recipes_df['id'].dropna().sample(num_popular).tolist()
        print(f"📖 {len(self.recipes_df)} recettes chargées.")

    def generate_behavior(self, start_user, end_user, interactions_per_user=15):
        if self.recipes_df is None: return print("❌ CSV non chargé.")

        for user_id in range(start_user, end_user + 1):
            for _ in range(interactions_per_user):
                # --- L'indentation commence ici ---
                target_row = self.recipes_df.sample(1).iloc[0]
                target_id = target_row['id']

                # Conversion forcée en entier pour ton entiteId (Long) côté Java
                try:
                    target_id = int(float(target_id)) 
                except:
                    continue 

                # 1. Enregistre la consultation
                self._record_interaction(user_id, target_id, 'CONSULTATION')

                # 2. Logique d'engagement aléatoire
                rand = random.random()
                if rand < 0.20: # 20% Note
                    self._add_rating(user_id, target_id)
                elif rand < 0.35: # 15% Favori
                    self._record_interaction(user_id, target_id, 'FAVORI_AJOUTE')
                elif rand < 0.45: # 10% Partage
                    self._record_interaction(user_id, target_id, 'PARTAGE')
                # --- Fin de l'indentation ---

            if user_id % 5 == 0:
                print(f"➔ Progression : Utilisateur {user_id} terminé.")

    def _record_interaction(self, u_id, r_id, action):
        params = {
            'userId': u_id,
            'typeInteraction': action,
            'entiteId': r_id,
            'dureeConsultation': random.randint(10, 300) if action == 'CONSULTATION' else None
        }
        return self._post_to_api("/interactions", params=params)

    def _add_rating(self, u_id, r_id):
        note_valeur = random.choices([5, 4, 3, 2], weights=[50, 30, 15, 5])[0]
        payload = {
            "userId": u_id,
            "recetteId": r_id,
            "note": note_valeur,
            "commentaire": random.choice(self.comment_pool) if random.random() < 0.5 else ""
        }
        status = self._post_to_api("/notes", json_data=payload)
        if status in [200, 201]:
            self._record_interaction(u_id, r_id, 'NOTE_POSEE')

    def _post_to_api(self, endpoint, json_data=None, params=None):
        try:
            res = self.session.post(
                f"{self.api_base_url}{endpoint}", 
                json=json_data, 
                params=params, 
                timeout=10
            )
            return res.status_code
        except:
            return None

if __name__ == "__main__":
    gen = BehaviorDataGenerator(api_base_url='http://localhost:8080/api/v1')
    if gen.login('dianekassi@admin.com', 'Mydayana48'):
        gen.load_local_data('recettes_clean.csv')
        print("\n🚀 Démarrage de la génération de données...")
        gen.generate_behavior(2001, 2030, interactions_per_user=15)
        print("\n✨ Simulation terminée.")
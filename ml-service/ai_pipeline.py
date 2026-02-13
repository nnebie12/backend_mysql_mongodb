import pandas as pd
import numpy as np
import os
from datetime import datetime
import requests
from sklearn.preprocessing import MinMaxScaler

class ProfessionalAIDataPipeline(object):
    def __init__(self, api_base_url='http://localhost:8080/api/v1'):
        self.api_base_url = api_base_url
        self.scaler = MinMaxScaler()
        print("🏗️ Pipeline ETL Professionnel Initialisé")

    def collect_training_data(self):
        """Extraction des données réelles depuis les APIs Spring Boot."""
        print("📡 Extraction des données depuis l'API Java...")
        try:
            # Récupération des interactions (MongoDB)
            interactions_resp = requests.get(f"{self.api_base_url}/interactions/all", timeout=10)
            interactions = interactions_resp.json() if interactions_resp.status_code == 200 else []

            # Récupération des recettes (MySQL via Spring)
            recipes_resp = requests.get(f"{self.api_base_url}/recettes/all", timeout=10)
            recipes = recipes_resp.json() if recipes_resp.status_code == 200 else []

            # Récupération des notes (MongoDB)
            ratings_resp = requests.get(f"{self.api_base_url}/notes/all", timeout=10)
            ratings = ratings_resp.json() if ratings_resp.status_code == 200 else []

            print(f"✅ Données récupérées : {len(interactions)} interactions, {len(recipes)} recettes, {len(ratings)} notes.")
            
            return {
                'interactions': interactions,
                'recipes': recipes,
                'ratings': ratings
            }
        except Exception as e:
            print(f"❌ Erreur lors de la collecte : {e}")
            return {'interactions': [], 'recipes': [], 'ratings': []}

    def process_and_clean_data(self, data):
        """Phase de transformation : Nettoyage et Ingénierie de Features."""
        print("🛠️ Début de la phase de transformation...")
        
        # 1. Traitement des interactions (Matrice User-Item)
        df_interactions = self._prepare_weighted_interactions(data['interactions'])
        
        # 2. Nettoyage des caractéristiques des recettes
        df_recipes = self._clean_recipe_data(data['recipes'])
        
        # 3. Calcul de la popularité hybride
        df_popularity = self._calculate_hybrid_popularity(df_interactions, data['ratings'])
        
        return {
            'matrix': df_interactions,
            'features': df_recipes,
            'popularity': df_popularity
        }

    def _prepare_weighted_interactions(self, interactions):
        """Crée une matrice avec Time Decay (décroissance temporelle)."""
        if not interactions: 
            return pd.DataFrame()
            
        df = pd.DataFrame(interactions)
        
        # Conversion de la date
        df['dateInteraction'] = pd.to_datetime(df['dateInteraction'])
        
        # Calcul du Time Decay (les actions récentes valent plus)
        # On gère le fuseau horaire pour éviter les erreurs de calcul
        now = datetime.now(df['dateInteraction'].iloc[0].tzinfo)
        df['days_ago'] = (now - df['dateInteraction']).dt.days
        df['time_decay'] = np.exp(-0.05 * df['days_ago']) # Moins 5% de poids par jour
        
        # Mapping des poids par type d'interaction
        weight_map = {'CONSULTATION': 1, 'PARTAGE': 3, 'FAVORI_AJOUTE': 5}
        df['base_weight'] = df['typeInteraction'].map(weight_map).fillna(1)
        
        # Score final pour la matrice
        df['final_score'] = df['base_weight'] * df['time_decay']
        
        # Pivot Table : Lignes = Utilisateurs, Colonnes = Recettes (entiteId)
        # On utilise entiteId car c'est le nom dans ton InteractionUtilisateur.java
        return df.pivot_table(index='userId', columns='entiteId', values='final_score', fill_value=0)

    def _clean_recipe_data(self, recipes):
        """Normalisation des données recettes (MySQL)."""
        if not recipes:
            return pd.DataFrame()
            
        df = pd.DataFrame(recipes)
        
        # Nettoyage temps de préparation
        df['tempsPreparation'] = pd.to_numeric(df['tempsPreparation'], errors='coerce')
        df['tempsPreparation'] = df['tempsPreparation'].fillna(df['tempsPreparation'].median())
        
        # Encodage difficulté
        df['difficulte'] = df['difficulte'].replace('', 'MOYEN').fillna('MOYEN')
        diff_map = {'FACILE': 1, 'MOYEN': 2, 'DIFFICILE': 3}
        df['diff_num'] = df['difficulte'].map(diff_map).fillna(2)
        
        # Création d'un index de complexité
        df['complexity_index'] = df['tempsPreparation'] * df['diff_num']
        
        # Normalisation entre 0 et 1 pour l'IA
        cols_to_scale = ['tempsPreparation', 'complexity_index']
        df[cols_to_scale] = self.scaler.fit_transform(df[cols_to_scale])
        
        return df

    def _calculate_hybrid_popularity(self, df_interactions, ratings):
        """Mélange Popularité (clics) et Satisfaction (notes)."""
        if df_interactions.empty:
            return pd.Series(dtype=float)
            
        # Somme des poids des interactions par recette
        pop_series = df_interactions.sum(axis=0)
        
        if ratings:
            df_ratings = pd.DataFrame(ratings)
            # On groupe par recetteId (nom usuel dans NoteResponseDTO)
            avg_ratings = df_ratings.groupby('recetteId')['note'].mean()
            
            # Formule : 70% clics + 30% moyenne des notes
            # On aligne les deux séries sur les IDs de recettes
            popularity = (pop_series * 0.7) + (avg_ratings * 0.3)
        else:
            popularity = pop_series
            
        return popularity.sort_values(ascending=False)

    def run_pipeline(self, output_path='ai_training_data'):
        """Lance le cycle complet ETL."""
        # 1. Extraction
        raw_data = self.collect_training_data()
        
        if not raw_data['recipes']:
            print("⚠️ Aucune recette trouvée. Pipeline interrompu.")
            return None
            
        # 2. Transformation
        processed = self.process_and_clean_data(raw_data)
        
        # 3. Chargement (Sauvegarde CSV)
        os.makedirs(output_path, exist_ok=True)
        processed['matrix'].to_csv(f"{output_path}/interaction_matrix.csv")
        processed['features'].to_csv(f"{output_path}/recipe_features_cleaned.csv", index=False)
        processed['popularity'].to_csv(f"{output_path}/trending_scores.csv")
        
        print(f"\n🚀 Pipeline terminé avec succès !")
        print(f"📁 Fichiers générés dans le dossier : {output_path}/")
        return processed

# --- EXÉCUTION ---
if __name__ == "__main__":
    pipeline = ProfessionalAIDataPipeline(api_base_url='http://localhost:8080/api/v1')
    results = pipeline.run_pipeline()
import numpy as np
import pandas as pd
import ast
import re
from typing import List, Dict, Optional
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
from sklearn.neighbors import NearestNeighbors
from sklearn.cluster import KMeans
from sklearn.decomposition import PCA
import uvicorn

# --- MOTEUR LOGIQUE ML ---

class RecipeMLEngine:
    def __init__(self, model_name='paraphrase-multilingual-MiniLM-L12-v2'):
        print(f"🤖 Initialisation du modèle SentenceTransformer ({model_name})...")
        self.model = SentenceTransformer(model_name)
        self.nn_model = NearestNeighbors(metric='cosine', algorithm='brute')
        self.recipe_ids = []
        self.matrix = None

    def _prepare_text(self, row: Dict) -> str:
        """Combine les champs pour créer une 'empreinte textuelle' riche."""
        ings = row.get('ingredients', [])
        if isinstance(ings, str):
            try: ings = ast.literal_eval(ings)
            except: ings = []
        
        ing_text = ", ".join([i.get('nom', i) if isinstance(i, dict) else str(i) for i in ings])
        
        parts = [
            str(row.get('titre', '')),
            str(row.get('description', '')),
            f"Cuisine {row.get('cuisine', '')}",
            f"Type {row.get('typeRecette', row.get('type_recette', ''))}",
            f"Ingrédients: {ing_text}",
            "Cette recette est adaptée aux végétariens" if row.get('vegetarien') else ""
        ]
        return ". ".join([p for p in parts if p.strip()])

    def fit(self, recipes_df: pd.DataFrame):
        """Entraîne l'index de recherche sur le dataset."""
        print(f"📊 Encodage de {len(recipes_df)} recettes (cela peut prendre un moment)...")
        texts = recipes_df.apply(self._prepare_text, axis=1).tolist()
        self.matrix = self.model.encode(texts, show_progress_bar=True, convert_to_numpy=True)
        self.recipe_ids = recipes_df['id'].tolist()
        
        self.nn_model.fit(self.matrix)
        print("✅ Index de recherche spatiale construit avec succès.")

    def recommend_similar(self, recipe_id: int, n_recs: int = 5) -> List[int]:
        if recipe_id not in self.recipe_ids:
            return []
        
        idx = self.recipe_ids.index(recipe_id)
        query_vec = self.matrix[idx].reshape(1, -1)
        distances, indices = self.nn_model.kneighbors(query_vec, n_neighbors=n_recs+1)
        
        # Exclure la recette elle-même
        similar_ids = [self.recipe_ids[i] for i in indices.flatten() if self.recipe_ids[i] != recipe_id]
        return [int(sid) for sid in similar_ids[:n_recs]]

# --- FONCTIONS DE NETTOYAGE ---

def clean_data_for_ml(df):
    df = df.copy()
    if 'id' not in df.columns:
        df['id'] = range(1, len(df) + 1)
        print("🆔 Colonne 'id' générée automatiquement.")

    def fix_row(row):
        diff_str = str(row.get('difficulte', '')).lower()
        if any(char.isdigit() for char in diff_str):
            numbers = re.findall(r'\d+', diff_str)
            if numbers:
                row['temps_preparation'] = int(numbers[0])
            row['difficulte'] = "Non précisée"
        return row
    
    df = df.apply(fix_row, axis=1)
    df['titre'] = df['titre'].fillna('')
    df['description'] = df['description'].fillna('')
    df['ingredients'] = df['ingredients'].fillna('[]')
    return df

# --- API FASTAPI ---

app = FastAPI(
    title="Recipe ML Engine API",
    description="API de recommandation sémantique pour les recettes"
)

# Initialisation globale
engine = RecipeMLEngine()

try:
    print("📂 Chargement des données...")
    raw_df = pd.read_csv('recettes_clean.csv')
    df_recipes = clean_data_for_ml(raw_df)
    engine.fit(df_recipes)
except FileNotFoundError:
    print("❌ ERREUR: 'recettes_clean.csv' est introuvable. L'API démarrera avec un moteur vide.")

class SearchQuery(BaseModel):
    query: str
    limit: Optional[int] = 10

@app.get("/")
def read_root():
    return {"status": "online", "model": "BERT multilingual", "recipes_indexed": len(engine.recipe_ids)}

@app.get("/recommend/{recipe_id}")
def get_recommendations(recipe_id: int, n: int = 5):
    recs = engine.recommend_similar(recipe_id, n_recs=n)
    if not recs:
        raise HTTPException(status_code=404, detail="Recette non trouvée ou ID invalide")
    return {"recipe_id": recipe_id, "recommendations": recs}

@app.post("/search/semantic")
def semantic_search(data: SearchQuery):
    if not engine.matrix is not None:
         raise HTTPException(status_code=503, detail="Moteur ML non initialisé")
    
    # Encodage de la requête utilisateur
    query_vec = engine.model.encode([data.query])
    distances, indices = engine.nn_model.kneighbors(query_vec, n_neighbors=data.limit)
    
    results = [int(engine.recipe_ids[i]) for i in indices.flatten()]
    return {"query": data.query, "results": results}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
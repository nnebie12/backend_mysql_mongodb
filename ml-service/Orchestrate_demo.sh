#!/bin/bash
# ═════════════════════════════════════════════════════════════════════════
#  SCRIPT ORCHESTRATEUR — Prépare 3 profils testés en 10 minutes
# ═════════════════════════════════════════════════════════════════════════

set -e  # Exit on error

API="http://localhost:8080/api/v1"
ADMIN_EMAIL="dianekassi@admin.com"
ADMIN_PASSWORD="Mydayana48"

echo "═══════════════════════════════════════════════════════════════════════"
echo "  🎯  ORCHESTRATEUR — Préparation des 3 profils de démo"
echo "═══════════════════════════════════════════════════════════════════════"

# STEP 1 : Créer les 3 utilisateurs
echo ""
echo "STEP 1️⃣  — Créer 3 utilisateurs de test"
echo "───────────────────────────────────────────"

create_user() {
    local nom=$1
    local email=$2
    echo -n "  Création de $nom... "
    response=$(curl -s -X POST "$API/auth/register" \
      -H "Content-Type: application/json" \
      -d "{
        \"prenom\":\"Test\",
        \"nom\":\"$nom\",
        \"email\":\"$email\",
        \"motDePasse\":\"Demo123456\"
      }")
    
    # Tentative d'extraire l'ID (varie selon la réponse du backend)
    user_id=$(echo "$response" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
    if [ -z "$user_id" ]; then
        user_id=$(echo "$response" | grep -o '"userId":[0-9]*' | head -1 | cut -d: -f2)
    fi
    
    if [ -n "$user_id" ]; then
        echo "✅ ID = $user_id"
        echo "$user_id"
    else
        echo "❌ Création échouée"
        echo "$response" | head -c 200
        echo ""
        return 1
    fi
}

USER_NOUVEAU_ID=$(create_user "NOUVEAU" "test.nouveau@demo.fr")
USER_ACTIF_ID=$(create_user "ACTIF" "test.actif@demo.fr")
USER_FIDELE_ID=$(create_user "FIDELE" "test.fidele@demo.fr")

echo ""
echo "📝 IDs créés (à noter pour step 2) :"
echo "   • USER_NOUVEAU = $USER_NOUVEAU_ID"
echo "   • USER_ACTIF   = $USER_ACTIF_ID"
echo "   • USER_FIDELE  = $USER_FIDELE_ID"

# STEP 2 : Modifier le script Python avec les IDs
echo ""
echo "STEP 2️⃣  — Générer les interactions (Python)"
echo "──────────────────────────────────────────────"

cat > /tmp/patch_ids.py << 'EOF'
import sys
script_path = sys.argv[1]
nouveau_id = sys.argv[2]
actif_id = sys.argv[3]
fidele_id = sys.argv[4]

with open(script_path, 'r') as f:
    content = f.read()

content = content.replace("USER_NOUVEAU = None", f"USER_NOUVEAU = {nouveau_id}")
content = content.replace("USER_ACTIF = None", f"USER_ACTIF = {actif_id}")
content = content.replace("USER_FIDELE = None", f"USER_FIDELE = {fidele_id}")

with open(script_path, 'w') as f:
    f.write(content)

print(f"✅ Script patché avec les IDs")
EOF

python3 /tmp/patch_ids.py \
    "generate_demo_profiles.py" \
    "$USER_NOUVEAU_ID" \
    "$USER_ACTIF_ID" \
    "$USER_FIDELE_ID"

echo "  Lancement de generate_demo_profiles.py..."
python3 generate_demo_profiles.py

# STEP 3 : Attendre le recalcul RFM
echo ""
echo "STEP 3️⃣  — Recalcul des profils RFM (attente)"
echo "───────────────────────────────────────────────"
echo "  ⏱️  Attendez 2–3 minutes..."
echo "     (le backend recalcule les scores RFM via ComportementUtilisateurService)"
sleep 120

# STEP 4 : Générer les recommandations calibrées
echo ""
echo "STEP 4️⃣  — Générer les recommandations calibrées"
echo "──────────────────────────────────────────────────"
echo "  Lancement de calibrate.py (~2 min)..."
python3 calibrate.py

# STEP 5 : Vérification finale
echo ""
echo "STEP 5️⃣  — Vérification du dashboard"
echo "──────────────────────────────────────"
echo "  ✅ Ouvrez : http://localhost:3000/admin/dashboard"
echo "  Vous devriez voir :"
echo "     • Chart RFM : distribution sur 5 profils (NOUVEAU, DÉBUTANT, etc.)"
echo "     • Onglet 'Utilisateurs' : vos 3 users avec profils différents"
echo "     • Onglet 'Recommandations IA' : reco par type (PERSONNALISÉE, etc.)"

echo ""
echo "═══════════════════════════════════════════════════════════════════════"
echo "  🎬  Vous êtes prêt pour la démo !"
echo "═══════════════════════════════════════════════════════════════════════"
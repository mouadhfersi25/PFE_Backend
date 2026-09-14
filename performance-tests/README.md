# Tests de charge JMeter — EduGame

Plan de test [edugame-load-test.jmx](edugame-load-test.jmx) pour mesurer les temps de réponse et
le taux d'erreur de l'API backend sous plusieurs utilisateurs simultanés.

## Installation

JMeter est déjà installé sur cette machine (Apache JMeter 5.6.3) dans :
```
C:\Users\<toi>\tools\apache-jmeter-5.6.3
```
Rien d'autre à installer — Java 17 (déjà présent pour le backend) suffit.

Le binaire JMeter (~90 Mo) n'est **pas** dans le dépôt git — seul ce plan de test (un simple
fichier XML) est versionné. Si tu changes de machine, retélécharge-le depuis
https://jmeter.apache.org/download_jmeter.cgi et extrais le zip où tu veux.

## ⚠️ Avant de lancer un test — compte de test obligatoire

Le plan de test se connecte (`POST /api/auth/login`) avec un compte réel avant de taper les
autres endpoints. **Deux règles importantes** :

1. **Utilise un compte de test dédié**, pas un vrai compte admin/éducateur/parent — ce compte va
   être appelé en boucle des dizaines de fois par seconde.
2. **Ne jamais lancer le test avec un mauvais mot de passe.** Le backend verrouille un compte
   après 5 échecs de connexion (15 minutes de blocage) — si tu te trompes de mot de passe dans
   le plan de test, tu vas bloquer le compte de test dès les premières secondes et tout le reste
   du test échouera avec "Trop de tentatives échouées". Vérifie bien tes identifiants avant de
   lancer un test avec plusieurs threads.

Crée un compte joueur de test (via l'inscription normale, ou demande à un admin d'en créer un),
puis renseigne ses identifiants au lancement (voir plus bas).

## Lancer un test

Toujours en **mode CLI (`-n`)** — jamais en mode graphique pour un vrai test de charge (le mode
graphique fausse les mesures et consomme trop de ressources).

```bash
cd ~/tools/apache-jmeter-5.6.3/bin

./jmeter -n \
  -t "/chemin/vers/backend/performance-tests/edugame-load-test.jmx" \
  -JTEST_EMAIL=ton-compte-de-test@exemple.com \
  -JTEST_PASSWORD=le-bon-mot-de-passe \
  -JTHREADS=20 \
  -JRAMP_UP=10 \
  -JLOOPS=10 \
  -l resultats.jtl \
  -e -o rapport-html
```

Après le test, ouvre `rapport-html/index.html` dans un navigateur — dashboard complet avec
graphiques (temps de réponse, débit, taux d'erreur) prêt à mettre dans un rapport.

### Paramètres réglables (`-J...`)
| Paramètre | Défaut | Rôle |
|---|---|---|
| `BASE_URL` | `http://localhost:8081` | URL du backend à tester |
| `TEST_EMAIL` / `TEST_PASSWORD` | — (à fournir) | Compte de test |
| `THREADS` | 20 | Nombre d'utilisateurs simulés en simultané |
| `RAMP_UP` | 10 | Secondes pour monter en charge jusqu'à `THREADS` |
| `LOOPS` | 10 | Nombre de fois que chaque utilisateur rejoue le scénario |

## Ce que le scénario teste
1. `POST /api/auth/login` — connexion (récupère le token JWT)
2. `GET /api/users/me` — profil utilisateur
3. `GET /api/users/games/available` — liste des jeux disponibles
4. `GET /api/users/leaderboard/solo` — classement

Une pause de 500 ms sépare chaque itération complète, pour simuler un rythme d'utilisation
réaliste plutôt qu'un flot ininterrompu de requêtes.

## Lire les résultats
- **Temps de réponse moyen / p95 / p99** — dans le rapport HTML, section "Response Times Over
  Time" et "Response Time Percentiles".
- **Taux d'erreur** — section "Errors" ; si > 0 % en dehors d'un test volontairement poussé à la
  limite, regarde `resultats.jtl` pour identifier quel endpoint échoue et pourquoi.
- **Débit (requêtes/seconde)** — section "Throughput Over Time".

Pour comparer avant/après une optimisation (ex. ajout d'un index), relance le même test avec les
mêmes paramètres et compare les deux rapports HTML.

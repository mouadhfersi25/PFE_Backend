# Contrat API Éducateur (aligné frontend)

Le frontend (`educatorApi`) consomme ces endpoints. Base URL frontend : `http://localhost:8081/api`.

## Jeux (lecture seule)

| Méthode | URL | Réponse / Body |
|--------|-----|----------------|
| GET | `/api/educator/games` | `GameDTO[]` |
| GET | `/api/educator/games/{id}` | `GameDTO` |

**GameDTO** : id, titre, description, difficulte, ageMin, ageMax, typeJeu, modeJeu, actif, dureeMinutes, icone, dateCreation (string ISO).

---

## Questions Quiz

| Méthode | URL | Body / Réponse |
|--------|-----|----------------|
| GET | `/api/educator/questions?gameId=` | `QuizQuestionDTO[]` |
| GET | `/api/educator/questions/{id}` | `QuizQuestionDTO` |
| POST | `/api/educator/questions` | CreateQuizQuestionRequest → 201 + `QuizQuestionDTO` |
| PUT | `/api/educator/questions/{id}` | UpdateQuizQuestionRequest → `QuizQuestionDTO` |
| DELETE | `/api/educator/questions/{id}` | 204 |

**QuizQuestionDTO** : id, jeuId, jeuTitre, contenu, bonneReponse, options (string[] | null), explication, difficulte.

**CreateQuizQuestionRequest** : jeuId, contenu, bonneReponse, options?, explication?, difficulte?.

**UpdateQuizQuestionRequest** : contenu?, bonneReponse?, options?, explication?, difficulte?.

---

## Cartes mémoire (paires)

| Méthode | URL | Body / Réponse |
|--------|-----|----------------|
| GET | `/api/educator/memory-cards?gameId=` | `MemoryCardDTO[]` |
| POST | `/api/educator/memory-cards` | CreateMemoryCardRequest → 201 + `MemoryCardDTO` |
| PUT | `/api/educator/memory-cards/{id}` | UpdateMemoryCardRequest → `MemoryCardDTO` |
| DELETE | `/api/educator/memory-cards/{id}` | 204 |

**MemoryCardDTO** : id, jeuId, jeuTitre, symbole, pairKey, categorie.

**CreateMemoryCardRequest** : jeuId, symbole, pairKey?, categorie?.

**UpdateMemoryCardRequest** : symbole?, pairKey?, categorie?.

---

## Dashboard

| Méthode | URL | Réponse |
|--------|-----|--------|
| GET | `/api/educator/dashboard/stats` | `EducatorDashboardStatsDTO` |

**EducatorDashboardStatsDTO** : totalQuestionsCreated, assignedGames, avgSuccessRate, studentActivity, difficultyDistribution (array of { name, value, color }).

---

## Erreurs

Réponses d’erreur : au moins `message` (string) pour que le frontend affiche `response.data.message`.

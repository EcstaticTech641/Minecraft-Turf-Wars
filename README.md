# rga-turfwars 👾

![Language](https://img.shields.io/badge/Language-Java_25-orange.svg)
![Platform](https://img.shields.io/badge/Platform-Paper_26.2-blue.svg)
![Framework](https://img.shields.io/badge/Framework-RGA_CPMK-purple.svg)

`rga-turfwars` is an official companion minigame plugin for the **Ronlab Game Assistant (RGA)** ecosystem.

## 📖 About the Minigame

Turf Wars is a fast-paced, team-based tug-of-war battle. Two teams (**Black Team** vs **Gold Team**) compete in an arena where the ground represents their team score. 

- **One-Shot Bow Combat**: Every bow hit results in an instant kill and advances your team's territory line.
- **Dynamic Turf Advancement**: Eliminating opponents shifts the territory line forward ($Z$-axis boundaries).
- **RGA Integration**: Procedural world loading, party management, JIT spectator routing, and victory teardowns are handled entirely by RGA (`com.ronlab:rga-api`).

## 🛠️ Tech Stack & Requirements

- **Java**: 25
- **Server Platform**: PaperMC 26.2 (`paper-plugin.yml`)
- **Build Tool**: Apache Maven (`pom.xml`)
- **Framework API**: `com.ronlab:rga-api:1.13.0-SNAPSHOT`

## ⚙️ Compilation & Deployment

### 1. Build via Maven:
```bash
mvn clean package
```
The compiled jar will be generated at `target/rga-turfwars-1.0.0-SNAPSHOT.jar`.

### 2. Deployment:
Deploy `rga-turfwars-1.0.0-SNAPSHOT.jar` to your Paper server's `plugins/` directory alongside `RonlabGameAssistant.jar`.

## 🛠️ Developer Test Command
- `/turfwars forcewin`: Requires `turfwars.admin` permission or OP. Triggers programmatic session conclusion for single-developer QA testing.


# Minecraft Turf Wars 👾
https://minecraft-turf-wars.netlify.app

![Java](https://img.shields.io/badge/Language-Java-orange.svg)
![Build Tool](https://img.shields.io/badge/Build-Gradle-blue.svg)

A custom Minecraft plugin designed for the KSU Minecraft Esports server to enable a "Turf Wars" team battle minigame.

## 📖 About the Project

Turf Wars is a fast paced, team based, player versus player experience that combines elements of a traditional “tug-of-war” game with Minecraft’s building and combat mechanics. Gold Team and Black Team face off in an arena where the ground they stand on represents their score in the game. The objective is simple: expand your team’s territory by eliminating enemy opponents.

Kennesaw State University Esports needed a fresh and engaging Minecraft gamemode for campus events. This game will satisfy all of these requirements helping drive more players to the KSU Minecraft server. This gamemode was purposefully designed with simplicity in mind so experienced players and new players will be able to enjoy the game together. 

## ✨ Features

* **Team-Based Gameplay:** Players are grouped into opposing teams (Gold Team and Black Team) to battle for arena dominance.
* **Territory Control:** Dynamic mapping and claiming of "turf" within the playing field. Whenever one team controls the entire arena the game ends.
* **Lobby System:** Includes a pre-game staging area (`/lobby`) for queuing players before the match begins.
* **Custom Arena World:** Comes with a `template_world` that is the default arena for the minigame.
* **Web Integration:** Includes a `website` directory containing HTML/CSS assets, used for displaying project information

## 🛠️ Tech Stack

* **Plugin Development:** Java (Spigot/Paper API)
* **Build Tool:** Gradle
* **Web Assets:** HTML (27%), CSS (4%)

## 📂 Project Structure

```text
Minecraft-Turf-Wars/
├── src/main/          # Java source code and resources for the Minecraft plugin
├── lobby/             # The pre-game lobby world file
├── template_world/    # The arena used for the Turf Wars minigame world file
├── website/           # Web files (HTML/CSS) for the projects's accompanying page
├── build.gradle       # Gradle build configuration and dependencies
└── gradlew            # Gradle wrapper executable
```
## 🚀 Getting Started
### Prerequisites
1. Java Development Kit (JDK) (Match the version to your target Minecraft server version, usually Java 17 or 21)
2. A Minecraft Server running Spigot or Paper
3. Git
## ⚙️ Installation & Build Instructions
### 1. Clone the repository:
```
git clone [https://github.com/minecraft-turf-wars/Minecraft-Turf-Wars.git](https://github.com/minecraft-turf-wars/Minecraft-Turf-Wars.git)
cd Minecraft-Turf-Wars
```
### 2. Build the plugin using the Gradle Wrapper:
```
./gradlew build
```
### 3. Deploy to your Minecraft server:
* After a successful build, locate the compiled .jar file in the build/libs/ directory.
* Copy the .jar file into your server's plugins/ folder.
* Restart or reload your server.

### 4. World Setup:
Move the template_world and lobby folders into your server's root directory.
⠀⠀
### 5. Have Fun!⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀

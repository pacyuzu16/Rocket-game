# Rocket Game

**Rocket Game** is an exciting 2D arcade-style game built in Java using Swing. Pilot a spaceship to destroy enemy rockets while dodging their attacks. With smooth graphics, sound effects, particle animations, and a high score system, this game offers a fun experience for players and a solid foundation for developers exploring Java game development.

---

## Table of Contents

- [Features](#features)
- [Screenshots](#screenshots)
- [Gameplay](#gameplay)
- [Installation](#installation)
- [Controls](#controls)
- [Project Structure](#project-structure)
- [How It Works](#how-it-works)
- [Known Limitations](#known-limitations)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgments](#acknowledgments)

---

## Features

- **Intuitive Controls**: Rotate, move, and shoot using keyboard inputs.
- **Dynamic Gameplay**: Destroy enemy rockets with unlimited small bullets or limited large bullets.
- **Visual Effects**: Explosions with particle animations for immersive destruction.
- **Sound Effects**: Background music and sounds for shooting, hits, and explosions.
- **High Score System**: Saves top score and player name to a file.
- **Game States**: Includes startup, name entry, gameplay, game over, and quit confirmation.

---

## Screenshots

<!-- Placeholder for screenshots; replace with actual image paths -->
![Startup Screen](/images/3.png)  
*Startup screen showing instructions and high score.*

![User's name](/images/1.png)  
*user should enter their names here for personalization*

![Gameplay](/images/2.png)  
*In-game action with player, rockets, and bullets.*

![Game Over](/images/4.png)  
*Game over screen displaying final score.*

![Quit](/images/4.png)  
*to quit or pause your game, press Q on your key board.*



## Gameplay

In **Rocket Game**, you control a spaceship to shoot down enemy rockets while avoiding collisions and enemy bullets. The goal is to achieve the highest score by destroying rockets. The game progresses through several states:

- **Startup**: Displays instructions and the current high score.
- **Name Entry**: Input your player name before starting.
- **Playing**: Core gameplay where you navigate and shoot.
- **Game Over**: Shows your final score and options to restart or return to home.
- **Confirm Quit**: Prompts confirmation to exit or return to the home screen.

---

## Installation



### Steps

1. **Clone the Repository**:
   git clone https://github.com/pacyuzu16/Rocket-game.git
   cd Rocket-game
2. **Verify Java Installation**:

                                 java -version

                                 Ensure JDK 8+ is installed.

### Compile the Code:
use your terminal or cmd and navigate to Rocket-game/dist then use the following coomand                        
                                 java -jar "ROCKET-GAME"

                                 Or use an IDE to build the project.

***Check Assets***: Confirm game/image/plane.png, game/image/background.png, and sound files exist in the correct directories.

### Controls
A / Left Arrow: Rotate spaceship left.

D / Right Arrow: Rotate spaceship right.

**Space**: Speed up the spaceship.

J: Fire small bullets (unlimited).

K: Fire large bullets (10 max, reloads every 10 seconds).

Enter: Start game or confirm name entry.

Q: Return to home or quit (confirm with Y/N).


### Project Structure


```plaintext
Rocket-game/
├── src/
│   └── game/
│       ├── component/
│       │   ├── PanelGame.java  # Main game panel and core logic
│       │   ├── Key.java        # Handles keyboard input
│       │   ├── Player.java     # Manages player spaceship behavior
│       │   ├── Bullet.java     # Defines bullet mechanics
│       │   ├── Rocket.java     # Controls enemy rocket behavior
│       │   ├── Effect.java     # Renders explosion and particle effects
│       │   ├── Sound.java      # Manages audio playback
│       └── image/
│           ├── plane.png       # Sprite for player spaceship
│           ├── background.png  # Game background image
│           ├── screenshot_startup.png   # Placeholder for startup screen
│           ├── screenshot_gameplay.png  # Placeholder for gameplay screen
│           ├── screenshot_gameover.png  # Placeholder for game over screen
│       └── sound/              # Audio files (e.g., shoot, hit, destroy)
├── highscore.txt               # Stores high score (format: playerName:score)
└── README.md                   # Project documentation
```




## How It Works

**Rocket Game** is a 2D arcade shooter powered by Java Swing and AWT, designed for smooth and engaging gameplay. Key technical aspects include:

- **Game Loop**: Maintains a consistent 60 FPS using a dedicated thread, ensuring fluid rendering and updates.
- **State Machine**: Manages five game states (Startup, Name Entry, Playing, Game Over, Confirm Quit) for a seamless user experience.
- **Collision System**: Leverages AWT’s `Area` class for precise detection of collisions between the player, bullets, and enemy rockets.
- **Multithreading**: Uses separate threads for keyboard input, bullet movement, and rocket spawning to keep gameplay responsive.
- **High Score Persistence**: Saves the top score and player name to `highscore.txt` using Java NIO, updated when a new high score is achieved.
- **Audio-Visual Effects**: Combines particle-based explosion animations with sound effects for shooting, hits, and destruction, enhancing immersion.

## Known Limitations
**Decompiled Code**: The source was decompiled with FernFlower, which may lack original comments or have minor artifacts.

**Asset Dependencies**: Missing image or sound files will cause errors or silent gameplay.

**Performance**: Heavy use of threads may lead to performance issues on low-end systems.

## Contributing

## Contributing

Want to enhance **Rocket Game**? Contributions are welcome! Whether it’s fixing bugs, adding features, or improving documentation, your input can make a difference. To contribute:

1. **Fork the Repository**:
   - Visit [github.com/pacyuzu16/Rocket-game](https://github.com/pacyuzu16/Rocket-game) and click "Fork".

2. **Create a Branch**:
   ```bash
   git checkout -b feature/your-feature-name

**Guidelines**:
   Follow the existing code style (e.g., naming conventions, indentation).

Include comments for significant changes.

Update documentation (e.g., this README) if needed.

Test your changes in the game to ensure compatibility.




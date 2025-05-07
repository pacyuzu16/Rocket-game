package game.component;

import game.obj.Bullet;
import game.obj.Effect;
import game.obj.Player;
import game.obj.Rocket;
import game.obj.sound.Sound;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class PanelGame extends JComponent {

    private Graphics2D g2;
    private BufferedImage image;
    private BufferedImage backgroundImage;
    private int width;
    private int height;
    private Thread thread;
    private volatile boolean start = true;
    private Key key;
    private int shotTime;

    // Game FPS
    private final int FPS = 60;
    private final int TARGET_TIME = 1000000000 / FPS;
    // Game Objects
    private Sound sound;
    private Player player;
    private List<Bullet> bullets;
    private List<Bullet> rocketBullets;
    private List<Rocket> rockets;
    private List<Effect> boomEffects;
    private int score = 0;
    private String playerName = "";
    private final StringBuilder nameInput = new StringBuilder();

    // Ammo tracking for large bullets
    private int largeBulletsUsed = 0;
    private final int largeAmmoLimit = 10;
    private long lastReloadTime = 0;
    private final long reloadInterval = 10000;

    // High score tracking
    private String highScorePlayer = "None";
    private int highScore = 0;
    private static final Path HIGH_SCORE_FILE = Paths.get("highscore.txt");

    // Game state
    private enum GameState { STARTUP, NAME_ENTRY, PLAYING, GAME_OVER, CONFIRM_QUIT }
    private GameState gameState = GameState.STARTUP;
    private GameState previousState;

    public void start() {
        width = getWidth();
        height = getHeight();
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        try {
            backgroundImage = ImageIO.read(getClass().getResource("/game/image/background.png"));
        } catch (IOException e) {
            System.err.println("Error loading background.png: " + e.getMessage());
            backgroundImage = null;
        }

        loadHighScore(); // Load high score at startup

        thread = new Thread(() -> {
            while (start) {
                long startTime = System.nanoTime();
                drawBackground();
                drawGame();
                render();
                long time = System.nanoTime() - startTime;
                if (time < TARGET_TIME) {
                    long sleep = (TARGET_TIME - time) / 1000000;
                    sleep(sleep);
                }
            }
        });
        initObjectGame();
        initKeyboard();
        initBullets();
        thread.start();
    }

    private void initObjectGame() {
        sound = new Sound();
        InputStream playerImageStream = getClass().getResourceAsStream("/game/image/plane.png");
        if (playerImageStream == null) {
            System.err.println("Error: Could not load plane.png");
        }
        player = new Player(playerImageStream);
        player.changeLocation(width / 2, height - 75);
        player.changeAngle(270);
        rockets = new CopyOnWriteArrayList<>();
        bullets = new CopyOnWriteArrayList<>();
        rocketBullets = new CopyOnWriteArrayList<>();
        boomEffects = new CopyOnWriteArrayList<>();
        new Thread(() -> {
            while (start) {
                if (gameState == GameState.PLAYING) {
                    addRocket();
                }
                sleep(3000);
            }
        }).start();
    }

    private void resetGame() {
        score = 0;
        largeBulletsUsed = 0;
        lastReloadTime = System.currentTimeMillis();
        rockets.clear();
        bullets.clear();
        rocketBullets.clear();
        boomEffects.clear();
        InputStream playerImageStream = getClass().getResourceAsStream("/game/image/plane.png");
        if (playerImageStream == null) {
            System.err.println("Error: Could not load plane.png");
        }
        player = new Player(playerImageStream);
        player.changeLocation(width / 2, height - 75);
        player.changeAngle(270);
        gameState = GameState.PLAYING;
        sound.playBackgroundMusic();
    }

    private void loadHighScore() {
        try {
            if (Files.exists(HIGH_SCORE_FILE)) {
                List<String> lines = Files.readAllLines(HIGH_SCORE_FILE);
                if (!lines.isEmpty()) {
                    String[] parts = lines.get(0).split(":");
                    if (parts.length == 2) {
                        highScorePlayer = parts[0];
                        highScore = Integer.parseInt(parts[1]);
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading high score: " + e.getMessage());
        }
    }

    private void saveHighScore() {
        try {
            String data = highScorePlayer + ":" + highScore;
            Files.writeString(HIGH_SCORE_FILE, data);
        } catch (IOException e) {
            System.err.println("Error saving high score: " + e.getMessage());
        }
    }

    private void updateHighScore() {
        if (score > highScore) {
            highScore = score;
            highScorePlayer = playerName;
            saveHighScore();
        }
    }

    private void initKeyboard() {
        key = new Key();
        requestFocus();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (null == gameState) {
                    if (keyCode == KeyEvent.VK_Q) {
                        if (gameState == GameState.STARTUP || gameState == GameState.PLAYING) {
                            previousState = gameState;
                            gameState = GameState.CONFIRM_QUIT;
                        } else {
                            gameState = GameState.STARTUP;
                            nameInput.setLength(0);
                        }
                    }
                    switch (keyCode) {
                        case KeyEvent.VK_A: key.setKey_left(true); break;
                        case KeyEvent.VK_D: key.setKey_right(true); break;
                        case KeyEvent.VK_SPACE: key.setKey_space(true); break;
                        case KeyEvent.VK_J: key.setKey_j(true); break;
                        case KeyEvent.VK_K: key.setKey_k(true); break;
                        case KeyEvent.VK_ENTER:
                            if (gameState == GameState.STARTUP) {
                                gameState = GameState.NAME_ENTRY;
                            } else if (gameState == GameState.GAME_OVER) {
                                updateHighScore(); // Update high score before returning to startup
                                gameState = GameState.STARTUP;
                                nameInput.setLength(0);
                            }
                            break;
                    }
                } else switch (gameState) {
                    case NAME_ENTRY:
                        if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
                            if (nameInput.length() < 15) {
                                nameInput.append((char) keyCode);
                            }
                        } else if (keyCode == KeyEvent.VK_BACK_SPACE && nameInput.length() > 0) {
                            nameInput.deleteCharAt(nameInput.length() - 1);
                        } else if (keyCode == KeyEvent.VK_ENTER && nameInput.length() > 0) {
                            playerName = nameInput.toString();
                            resetGame();
                        } else if (keyCode == KeyEvent.VK_Q) {
                            gameState = GameState.STARTUP;
                            nameInput.setLength(0);
                        }   break;
                    case CONFIRM_QUIT:
                        if (keyCode == KeyEvent.VK_Y) {
                            if (previousState == GameState.STARTUP) {
                                start = false;
                                sound.stopBackgroundMusic();
                                System.exit(0);
                            } else if (previousState == GameState.PLAYING) {
                                gameState = GameState.STARTUP;
                                nameInput.setLength(0);
                                sound.stopBackgroundMusic();
                            }
                        } else if (keyCode == KeyEvent.VK_N) {
                            gameState = previousState;
                        }   break;
                    default:
                        if (keyCode == KeyEvent.VK_Q) {
                            if (gameState == GameState.STARTUP || gameState == GameState.PLAYING) {
                                previousState = gameState;
                                gameState = GameState.CONFIRM_QUIT;
                            } else {
                                gameState = GameState.STARTUP;
                                nameInput.setLength(0);
                            }
                        }   switch (keyCode) {
                            case KeyEvent.VK_A: key.setKey_left(true); break;
                            case KeyEvent.VK_D: key.setKey_right(true); break;
                            case KeyEvent.VK_SPACE: key.setKey_space(true); break;
                            case KeyEvent.VK_J: key.setKey_j(true); break;
                            case KeyEvent.VK_K: key.setKey_k(true); break;
                            case KeyEvent.VK_ENTER:
                                if (gameState == GameState.STARTUP) {
                                    gameState = GameState.NAME_ENTRY;
                                } else if (gameState == GameState.GAME_OVER) {
                                    updateHighScore(); // Update high score before returning to startup
                                    gameState = GameState.STARTUP;
                                    nameInput.setLength(0);
                                }
                                break;
                        }   break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_A: key.setKey_left(false); break;
                    case KeyEvent.VK_D: key.setKey_right(false); break;
                    case KeyEvent.VK_SPACE: key.setKey_space(false); break;
                    case KeyEvent.VK_J: key.setKey_j(false); break;
                    case KeyEvent.VK_K: key.setKey_k(false); break;
                }
            }
        });
        new Thread(() -> {
            float s = 0.5f;
            Random rand = new Random();
            while (start) {
                try {
                    if (gameState == GameState.PLAYING && player.isAlive()) {
                        float angle = player.getAngle();
                        if (key.isKey_left()) angle -= s;
                        if (key.isKey_right()) angle += s;
                        if (key.isKey_j() || key.isKey_k()) {
                            if (shotTime == 0) {
                                if (key.isKey_j()) {
                                    bullets.add(new Bullet(player.getX(), player.getY(), angle, 8, 3f, new Color(169, 169, 169)));
                                    sound.soundShoot();
                                } else if (key.isKey_k() && largeBulletsUsed < largeAmmoLimit) {
                                    bullets.add(new Bullet(player.getX(), player.getY(), angle, 25, 3f, new Color(184, 115, 51)));
                                    largeBulletsUsed++;
                                    sound.soundBigBullet();
                                }
                            }
                            shotTime++;
                            if (shotTime == 15) shotTime = 0;
                        } else {
                            shotTime = 0;
                        }
                        if (key.isKey_space()) player.speedUp();
                        else player.speedDown();
                        player.update();
                        
                        double newX = player.getX();
                        double newY = player.getY();
                        if (newX < 0) newX = 0;
                        else if (newX + Player.PLAYER_SIZE > width) newX = width - Player.PLAYER_SIZE;
                        if (newY < 0) newY = 0;
                        else if (newY + Player.PLAYER_SIZE > height) newY = height - Player.PLAYER_SIZE;
                        player.changeLocation(newX, newY);
                        
                        player.changeAngle(angle);
                        
                        long currentTime = System.currentTimeMillis();
                        if (largeBulletsUsed >= largeAmmoLimit && currentTime - lastReloadTime >= reloadInterval) {
                            largeBulletsUsed = 0;
                            lastReloadTime = currentTime;
                        }
                    }
                    if (gameState == GameState.PLAYING || gameState == GameState.GAME_OVER) {
                        for (Rocket rocket : rockets) {
                            if (rocket != null && start) {
                                rocket.update();
                                if (gameState == GameState.PLAYING && rand.nextFloat() < 0.01f) {
                                    double bulletX = rocket.getX() + Rocket.ROCKET_SIZE / 2;
                                    double bulletY = rocket.getY();
                                    bulletX += -40;
                                    bulletY += 20;
                                    rocketBullets.add(new Bullet(bulletX, bulletY, 90, 8, 2f, Color.RED));
                                    sound.soundShoot();
                                }
                                if (!rocket.check(width, height)) {
                                    rockets.remove(rocket);
                                } else if (gameState == GameState.PLAYING && player.isAlive()) {
                                    checkPlayer(rocket);
                                }
                            }
                        }
                    }
                    sleep(5);
                } catch (Exception e) {
                    System.err.println("Error in keyboard thread: " + e.getMessage());
                }
            }
        }).start();
    }

    private void addRocket() {
        Random ran = new Random();
        int locationX = ran.nextInt(width - 50) + 25;
        Rocket rocket = new Rocket();
        rocket.changeLocation(locationX, 0);
        rocket.changeAngle(90);
        rockets.add(rocket);
    }

    private void initBullets() {
        new Thread(() -> {
            while (start) {
                try {
                    if (gameState == GameState.PLAYING) {
                        for (int i = 0; i < bullets.size() && start; i++) {
                            Bullet bullet = bullets.get(i);
                            if (bullet != null) {
                                bullet.update();
                                checkBullets(bullet);
                                if (!bullet.check(width, height)) {
                                    bullets.remove(i);
                                    i--;
                                }
                            }
                        }
                        for (int i = 0; i < rocketBullets.size() && start; i++) {
                            Bullet bullet = rocketBullets.get(i);
                            if (bullet != null) {
                                bullet.update();
                                checkRocketBullets(bullet);
                                if (!bullet.check(width, height)) {
                                    rocketBullets.remove(i);
                                    i--;
                                }
                            }
                        }
                        for (int i = 0; i < boomEffects.size() && start; i++) {
                            Effect boomEffect = boomEffects.get(i);
                            if (boomEffect != null) {
                                boomEffect.update();
                                if (!boomEffect.check()) {
                                    boomEffects.remove(i);
                                    i--;
                                }
                            }
                        }
                    }
                    sleep(1);
                } catch (Exception e) {
                    System.err.println("Error in bullet thread: " + e.getMessage());
                }
            }
        }).start();
    }

    private void checkBullets(Bullet bullet) {
        for (int i = 0; i < rockets.size() && start; i++) {
            Rocket rocket = rockets.get(i);
            if (rocket != null) {
                Area area = new Area(bullet.getShape());
                area.intersect(rocket.getShape());
                if (!area.isEmpty()) {
                    boomEffects.add(new Effect(bullet.getCenterX(), bullet.getCenterY(), 3, 5, 60, 0.5f, new Color(230, 207, 105)));
                    if (!rocket.updateHP(bullet.getSize())) {
                        score++;
                        if (score % 10 == 0 && player.isAlive()) {
                            player.resetHP();
                            sound.soundHit();
                        }
                        rockets.remove(i);
                        i--;
                        sound.soundDestroy();
                        double x = rocket.getX() + Rocket.ROCKET_SIZE / 2;
                        double y = rocket.getY() + Rocket.ROCKET_SIZE / 2;
                        boomEffects.add(new Effect(x, y, 15, 20, 30, 1.0f, new Color(255, 255, 255)));
                        boomEffects.add(new Effect(x, y, 10, 15, 40, 0.8f, new Color(255, 150, 0)));
                        boomEffects.add(new Effect(x, y, 20, 5, 50, 0.3f, new Color(32, 178, 169, 150)));
                        for (int j = 0; j < 8; j++) {
                            float sparkSpeed = 2.0f + (float) Math.random() * 1.0f;
                            boomEffects.add(new Effect(x, y, 2, 3, 20 + j * 5, sparkSpeed, new Color(255, 70, 70)));
                        }
                        for (int j = 0; j < 5; j++) {
                            float debrisSpeed = 0.5f + (float) Math.random() * 0.5f;
                            boomEffects.add(new Effect(x, y, 5, 5, 60 + j * 10, debrisSpeed, new Color(100, 100, 100)));
                        }
                        boomEffects.add(new Effect(x, y, 20, 10, 100, 0.1f, new Color(50, 50, 50, 100)));
                    } else {
                        sound.soundHit();
                    }
                    bullets.remove(bullet);
                    break;
                }
            }
        }
    }

    private void checkRocketBullets(Bullet bullet) {
        if (player.isAlive()) {
            Area area = new Area(bullet.getShape());
            area.intersect(player.getShape());
            if (!area.isEmpty()) {
                if (!player.updateHP(10)) {
                    player.setAlive(false);
                    sound.soundDestroy();
                    double x = player.getX() + Player.PLAYER_SIZE / 2;
                    double y = player.getY() + Player.PLAYER_SIZE / 2;
                    boomEffects.add(new Effect(x, y, 5, 5, 75, 0.05f, new Color(32, 178, 169)));
                    boomEffects.add(new Effect(x, y, 5, 5, 75, 0.1f, new Color(32, 178, 169)));
                    boomEffects.add(new Effect(x, y, 10, 10, 100, 0.3f, new Color(230, 207, 105)));
                    boomEffects.add(new Effect(x, y, 10, 5, 100, 0.5f, new Color(255, 70, 70)));
                    boomEffects.add(new Effect(x, y, 10, 5, 150, 0.2f, new Color(255, 255, 255)));
                    gameState = GameState.GAME_OVER;
                } else {
                    sound.soundHit();
                }
                rocketBullets.remove(bullet);
            }
        }
    }

    private void checkPlayer(Rocket rocket) {
        if (rocket != null && player.isAlive()) {
            Area area = new Area(player.getShape());
            area.intersect(rocket.getShape());
            if (!area.isEmpty()) {
                double rocketHp = rocket.getHP();
                if (!rocket.updateHP(player.getHP())) {
                    rockets.remove(rocket);
                    sound.soundDestroy();
                    double x = rocket.getX() + Rocket.ROCKET_SIZE / 2;
                    double y = rocket.getY() + Rocket.ROCKET_SIZE / 2;
                    boomEffects.add(new Effect(x, y, 5, 5, 75, 0.05f, new Color(32, 178, 169)));
                    boomEffects.add(new Effect(x, y, 5, 5, 75, 0.1f, new Color(32, 178, 169)));
                    boomEffects.add(new Effect(x, y, 10, 10, 100, 0.3f, new Color(230, 207, 105)));
                    boomEffects.add(new Effect(x, y, 10, 5, 100, 0.5f, new Color(255, 70, 70)));
                    boomEffects.add(new Effect(x, y, 10, 5, 150, 0.2f, new Color(255, 255, 255)));
                }
                if (!player.updateHP(rocketHp)) {
                    player.setAlive(false);
                    sound.soundDestroy();
                    double x = player.getX() + Player.PLAYER_SIZE / 2;
                    double y = player.getY() + Player.PLAYER_SIZE / 2;
                    boomEffects.add(new Effect(x, y, 5, 5, 75, 0.05f, new Color(32, 178, 169)));
                    boomEffects.add(new Effect(x, y, 5, 5, 75, 0.1f, new Color(32, 178, 169)));
                    boomEffects.add(new Effect(x, y, 10, 10, 100, 0.3f, new Color(230, 207, 105)));
                    boomEffects.add(new Effect(x, y, 10, 5, 100, 0.5f, new Color(255, 70, 70)));
                    boomEffects.add(new Effect(x, y, 10, 5, 150, 0.2f, new Color(255, 255, 255)));
                    gameState = GameState.GAME_OVER;
                }
            }
        }
    }

    private void drawBackground() {
        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, width, height, null);
        } else {
            g2.setColor(new Color(0, 51, 102));
            g2.fillRect(0, 0, width, height);
        }
    }

    private void drawGame() {
        switch (gameState) {
            case STARTUP: drawInstructions(); break;
            case NAME_ENTRY: drawNameEntry(); break;
            case PLAYING: drawPlayingState(); break;
            case GAME_OVER: drawGameOver(); break;
            case CONFIRM_QUIT: drawConfirmQuit(); break;
        }
    }

    private void drawInstructions() {
        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(Font.BOLD, 30f));
        FontMetrics fm = g2.getFontMetrics();
        String title = "Rocket Man";
        Rectangle2D r2 = fm.getStringBounds(title, g2);
        double x = (width - r2.getWidth()) / 2;
        double y = height / 4;
        g2.drawString(title, (int) x, (int) y);

        g2.setFont(getFont().deriveFont(Font.PLAIN, 20f));
        fm = g2.getFontMetrics();
        String[] instructions = {
            "Instructions:",
            "A/D: Rotate Left/Right",
            "Space: Speed Up",
            "J: Shoot Small Bullet (Unlimited)",
            "K: Shoot Rockets (10, reloads every 10s)",
            "Enter: Start Game",
            "Q: Quit (Confirm with Y/N)",
            "Press Enter to Continue"
        };
        int lineHeight = 30;
        y = height / 2 - (instructions.length * lineHeight) / 2;
        for (String line : instructions) {
            r2 = fm.getStringBounds(line, g2);
            x = (width - r2.getWidth()) / 2;
            g2.drawString(line, (int) x, (int) y);
            y += lineHeight;
        }

        // Display high score
        g2.setFont(getFont().deriveFont(Font.PLAIN, 20f));
        fm = g2.getFontMetrics();
        String highScoreText = "High Score: " + highScorePlayer + " - " + highScore;
        r2 = fm.getStringBounds(highScoreText, g2);
        x = (width - r2.getWidth()) / 2;
        y += 20;
        g2.drawString(highScoreText, (int) x, (int) y);

        g2.setFont(getFont().deriveFont(Font.PLAIN, 15f));
        fm = g2.getFontMetrics();
        String copyright = "© 2025 Pacyuzu Inc. All rights reserved.";
        r2 = fm.getStringBounds(copyright, g2);
        x = (width - r2.getWidth()) / 2;
        y += 20;
        g2.drawString(copyright, (int) x, (int) y);
    }

    private void drawNameEntry() {
        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(Font.BOLD, 30f));
        FontMetrics fm = g2.getFontMetrics();
        String prompt = "Enter Your Name:";
        Rectangle2D r2 = fm.getStringBounds(prompt, g2);
        double x = (width - r2.getWidth()) / 2;
        double y = height / 3;
        g2.drawString(prompt, (int) x, (int) y);

        g2.setFont(getFont().deriveFont(Font.PLAIN, 25f));
        fm = g2.getFontMetrics();
        String name = nameInput.toString();
        if (name.isEmpty()) {
            name = "Type your name...";
            g2.setColor(Color.GRAY);
        } else {
            g2.setColor(Color.WHITE);
        }
        r2 = fm.getStringBounds(name, g2);
        x = (width - r2.getWidth()) / 2;
        y = height / 2;
        g2.drawString(name, (int) x, (int) y);

        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(Font.PLAIN, 20f));
        fm = g2.getFontMetrics();
        String instruction = "Press Enter to Confirm, Q to Home";
        r2 = fm.getStringBounds(instruction, g2);
        x = (width - r2.getWidth()) / 2;
        y = height * 2 / 3;
        g2.drawString(instruction, (int) x, (int) y);
    }

    private void drawPlayingState() {
        if (player.isAlive()) {
            player.draw(g2);
        }
        for (Bullet bullet : bullets) {
            if (bullet != null) bullet.draw(g2);
        }
        for (Bullet bullet : rocketBullets) {
            if (bullet != null) bullet.draw(g2);
        }
        for (Rocket rocket : rockets) {
            if (rocket != null) rocket.draw(g2);
        }
        for (Effect boomEffect : boomEffects) {
            if (boomEffect != null) boomEffect.draw(g2);
        }
        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(Font.BOLD, 15f));
        g2.drawString("Player: " + playerName, 10, 20);
        g2.drawString("Score: " + score, 10, 40);
        g2.drawString("Rockets: " + largeBulletsUsed + "/" + largeAmmoLimit, 10, 60);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String currentTime = LocalTime.now().format(timeFormatter);
        g2.drawString("Time: " + currentTime, 10, 80);

        if (largeBulletsUsed >= largeAmmoLimit) {
            long currentTimeMillis = System.currentTimeMillis();
            long timeSinceLastReload = currentTimeMillis - lastReloadTime;
            int secondsRemaining = (int) ((reloadInterval - timeSinceLastReload) / 1000);
            if (secondsRemaining > 0) {
                g2.setColor(Color.RED);
                g2.drawString("Large Bullet Reload: " + secondsRemaining + "s", 10, 100);
            }
        }
    }

    private void drawGameOver() {
        drawPlayingState();
        sound.stopBackgroundMusic();
        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(Font.BOLD, 50f));
        FontMetrics fm = g2.getFontMetrics();
        String text = "GAME OVER";
        Rectangle2D r2 = fm.getStringBounds(text, g2);
        double textWidth = r2.getWidth();
        double textHeight = r2.getHeight();
        double x = (width - textWidth) / 2;
        double y = (height - textHeight) / 2 - 50;
        g2.drawString(text, (int) x, (int) y + fm.getAscent());

        g2.setFont(getFont().deriveFont(Font.BOLD, 25f));
        fm = g2.getFontMetrics();
        String killMessage = playerName + ", you killed " + score + " rockets!";
        r2 = fm.getStringBounds(killMessage, g2);
        textWidth = r2.getWidth();
        double x2 = (width - textWidth) / 2;
        double y2 = (height - textHeight) / 2 + 20;
        g2.drawString(killMessage, (int) x2, (int) y2 + fm.getAscent());

        g2.setFont(getFont().deriveFont(Font.BOLD, 15f));
        fm = g2.getFontMetrics();
        String textKey = "Press Enter or Q to Home";
        r2 = fm.getStringBounds(textKey, g2);
        textWidth = r2.getWidth();
        double x3 = (width - textWidth) / 2;
        double y3 = (height - textHeight) / 2 + 70;
        g2.drawString(textKey, (int) x3, (int) y3 + fm.getAscent());
    }

    private void drawConfirmQuit() {
        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(Font.BOLD, 30f));
        FontMetrics fm = g2.getFontMetrics();
        String text = previousState == GameState.STARTUP ? "Quit Game? (Y/N)" : "Return to Home? (Y/N)";
        Rectangle2D r2 = fm.getStringBounds(text, g2);
        double textWidth = r2.getWidth();
        double textHeight = r2.getHeight();
        double x = (width - textWidth) / 2;
        double y = (height - textHeight) / 2;
        g2.drawString(text, (int) x, (int) y + fm.getAscent());
    }

    private void render() {
        Graphics g = getGraphics();
        if (g != null) {
            g.drawImage(image, 0, 0, null);
            g.dispose();
        }
    }

    private void sleep(long speed) {
        try {
            Thread.sleep(speed);
        } catch (InterruptedException ex) {
            System.err.println("Sleep interrupted: " + ex.getMessage());
        }
    }
}
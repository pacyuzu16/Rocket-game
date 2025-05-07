package game.obj.sound;

import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Sound {

    private final URL shoot;
    private final URL hit;
    private final URL destroy;
    private final URL bigBullet; // New sound for big bullet
    private final URL backgroundMusic;
    private Clip backgroundClip; // Store the clip for background music to control it

    public Sound() {
        this.shoot = this.getClass().getClassLoader().getResource("game/obj/sound/shoot.wav");
        this.hit = this.getClass().getClassLoader().getResource("game/obj/sound/hit.wav");
        this.destroy = this.getClass().getClassLoader().getResource("game/obj/sound/destroy.wav");
        this.bigBullet = this.getClass().getClassLoader().getResource("game/obj/sound/bad-explosion-6855.wav");
        this.backgroundMusic = this.getClass().getClassLoader().getResource("game/obj/sound/zt-byte-blast-163367.wav");
    }

    public void soundShoot() {
        play(shoot);
    }

    public void soundHit() {
        play(hit);
    }

    public void soundDestroy() {
        play(destroy);
    }

    public void soundBigBullet() {
        play(bigBullet);
    }

    private void play(URL url) {
        try {
            if (url == null) {
                System.err.println("Sound file not found!");
                return;
            }
            Clip clip;
            try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(url)) {
                clip = AudioSystem.getClip();
                clip.open(audioIn);
                clip.addLineListener((LineEvent event) -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
            }
            clip.start();
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
            System.err.println("Error playing sound: " + e.getMessage());
        }
    }

    public void playBackgroundMusic() {
        try {
            if (backgroundClip != null && backgroundClip.isRunning()) {
                backgroundClip.stop(); // Stop if already playing
            }
            try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(backgroundMusic)) {
                backgroundClip = AudioSystem.getClip();
                backgroundClip.open(audioIn);
                backgroundClip.loop(Clip.LOOP_CONTINUOUSLY); // Loop indefinitely
            }
            backgroundClip.start();
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
            System.err.println("Error playing background music: " + e.getMessage());
        }
    }

    public void stopBackgroundMusic() {
        if (backgroundClip != null && backgroundClip.isRunning()) {
            backgroundClip.stop();
            backgroundClip.close();
        }
    }
}
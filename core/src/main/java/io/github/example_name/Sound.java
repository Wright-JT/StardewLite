package io.github.example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;

import java.util.Random;

public class Sound {

    // --- LibGDX audio references (use fully-qualified com.badlogic.gdx.audio.Sound) ---
    private com.badlogic.gdx.audio.Sound waveSound;
    private com.badlogic.gdx.audio.Sound birdSound;
    private com.badlogic.gdx.audio.Sound footstepSound;
    private com.badlogic.gdx.audio.Sound hoeLandSound;
    private com.badlogic.gdx.audio.Sound pickCropSound;
    private com.badlogic.gdx.audio.Sound breakLandSound;
    private com.badlogic.gdx.audio.Sound vendorSound;
    private com.badlogic.gdx.audio.Sound burpSound;    // <--- existing
    private com.badlogic.gdx.audio.Sound moneySound;   // <--- existing
    private com.badlogic.gdx.audio.Sound deathSound;   // <--- added
    private com.badlogic.gdx.audio.Sound seedSound;    // <--- added

    // Single reference to the currently playing music track
    private Music music;

    // Music playlist
    private Music[] songs;
    private int currentSongIndex = -1;

    // --- Music mute flag ---
    private boolean musicMuted = false;

    // --- Footstep timing ---
    private float footstepTimer = 0f;
    private final float footstepInterval = 0.5f; // seconds between footsteps while moving

    // --- Ambient scheduler thread (birds every ~30s) ---
    private volatile boolean ambientRunning = true;
    private Thread ambientThread;
    private final java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();

    public Sound() {
        // Load all sounds, but be forgiving if a file is missing
        waveSound      = loadSoundSafe("wave.mp3");
        birdSound      = loadSoundSafe("bird.mp3");
        footstepSound  = loadSoundSafe("footstep.mp3");
        hoeLandSound   = loadSoundSafe("hoeland.mp3");
        pickCropSound  = loadSoundSafe("pickcrop.mp3");
        breakLandSound = loadSoundSafe("breakland.mp3");
        vendorSound    = loadSoundSafe("vendor.mp3");
        burpSound      = loadSoundSafe("burp.mp3");
        moneySound     = loadSoundSafe("money.mp3");
        deathSound     = loadSoundSafe("death.mp3");   // <--- added
        seedSound      = loadSoundSafe("seed.mp3");    // <--- added

        // --- MUSIC PLAYLIST SETUP ---
        songs = new Music[] {
            loadMusicSafe("song1.mp3"),
            loadMusicSafe("song2.mp3"),
            loadMusicSafe("song3.mp3")
        };

        // Start music playlist (random song, then random non-repeating)
        startMusicPlaylist();

        // Make the ocean (wave) sound always playing at ~15% volume
        if (waveSound != null) {
            waveSound.loop(0.15f);
        }

        // Start background ambient sound scheduler (birds only)
        startAmbientThread();
    }

    private com.badlogic.gdx.audio.Sound loadSoundSafe(String fileName) {
        try {
            FileHandle fh = Gdx.files.internal(fileName);
            if (!fh.exists()) {
                System.out.println("[Sound] Missing sound file: " + fileName);
                return null;
            }
            return Gdx.audio.newSound(fh);
        } catch (Exception e) {
            System.out.println("[Sound] Failed to load sound " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    private Music loadMusicSafe(String fileName) {
        try {
            FileHandle fh = Gdx.files.internal(fileName);
            if (!fh.exists()) {
                System.out.println("[Sound] Missing music file: " + fileName);
                return null;
            }
            return Gdx.audio.newMusic(fh);
        } catch (Exception e) {
            System.out.println("[Sound] Failed to load music " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // MUSIC PLAYLIST LOGIC
    // -------------------------------------------------------------------------

    /** Called once from constructor to start the playlist. */
    private void startMusicPlaylist() {
        if (songs == null || songs.length == 0) {
            return;
        }
        playNextSong();
    }

    /**
     * Picks a random song from the playlist and plays it.
     * Tries to avoid picking the same index that just played (if possible).
     */
    private void playNextSong() {
        if (songs == null || songs.length == 0) {
            return;
        }

        // Count how many non-null songs exist
        int validCount = 0;
        for (Music m : songs) {
            if (m != null) validCount++;
        }
        if (validCount == 0) {
            return;
        }

        int nextIndex = currentSongIndex;
        int attempts = 0;
        final int maxAttempts = 10;

        // Try to get a different valid song than the last one (if more than 1 valid)
        do {
            nextIndex = random.nextInt(songs.length);
            attempts++;
        } while (
            (songs[nextIndex] == null ||
                (validCount > 1 && nextIndex == currentSongIndex))
                && attempts < maxAttempts
        );

        // If we still don't have a valid song, bail
        if (songs[nextIndex] == null) {
            return;
        }

        // Stop current music if any (don't dispose; it's part of the playlist)
        if (music != null) {
            music.stop();
        }

        currentSongIndex = nextIndex;
        music = songs[currentSongIndex];

        music.setLooping(false);  // let it end so the next one can play
        music.setVolume(0.3f);

        // When this song ends, pick another random one
        music.setOnCompletionListener(new Music.OnCompletionListener() {
            @Override
            public void onCompletion(Music completedMusic) {
                playNextSong();
            }
        });

        // Respect current mute state when starting a new track
        applyMusicMuteState();
    }

    // -------------------------------------------------------------------------
    // MUSIC MUTE CONTROL
    // -------------------------------------------------------------------------

    /**
     * Toggle music mute state. Called from Core, e.g. using UI.isMusicMuted().
     */
    public void setMusicMuted(boolean muted) {
        if (this.musicMuted == muted) return;
        this.musicMuted = muted;
        applyMusicMuteState();
    }

    /**
     * Apply current mute flag to the active music instance.
     */
    private void applyMusicMuteState() {
        if (music == null) return;

        if (musicMuted) {
            if (music.isPlaying()) {
                music.pause();
            }
        } else {
            if (!music.isPlaying()) {
                music.play();  // resume or start current track
            }
        }
    }

    public boolean isMusicMuted() {
        return musicMuted;
    }

    // -------------------------------------------------------------------------
    // PUBLIC UPDATE – call every frame
    // -------------------------------------------------------------------------

    /**
     * Call once per frame from Core.render().
     *
     * @param delta  time since last frame
     * @param moving true if the player is currently pressing any movement key
     */
    public void update(float delta, boolean moving) {
        if (moving && footstepSound != null) {
            footstepTimer += delta;
            if (footstepTimer >= footstepInterval) {
                footstepTimer = 0f;
                playFootstep();
            }
        } else {
            // Reset if not moving; this avoids instant stomp when resuming
            footstepTimer = 0f;
        }
    }

    // -------------------------------------------------------------------------
    // EVENT SOUND HELPERS – call these from Core on specific actions
    // -------------------------------------------------------------------------

    /** Called when the player tills land. */
    public void playHoeLand() {
        if (hoeLandSound != null) {
            // Random pitch between ~0.9 and 1.1
            float pitch = 0.9f + random.nextFloat() * 0.2f;
            hoeLandSound.play(0.8f, pitch, 0f);
        }
    }

    /** Called when the player untills / breaks land. */
    public void playBreakLand() {
        if (breakLandSound != null) {
            float pitch = 0.9f + random.nextFloat() * 0.2f; // random between 0.9–1.1
            breakLandSound.play(0.8f, pitch, 0f);
        }
    }

    /** Called when the player burps. */
    public void playBurp() {
        if (burpSound != null) {
            float pitch = 0.9f + random.nextFloat() * 0.2f; // random between 0.9–1.1
            burpSound.play(0.8f, pitch, 0f);
        }
    }

    /** Called when the player gains / spends money. */
    public void playMoney() {
        if (moneySound != null) {
            moneySound.play(0.7f);
        }
    }

    /** Called when the player dies. */
    public void playDeath() {                      // <--- added
        if (deathSound != null) {
            deathSound.play(0.6f);                 // tweak volume as desired
        }
    }

    /** Called when the player plants a seed. */
    public void playSeed() {                       // <--- added
        if (seedSound != null) {
            // Random pitch between ~0.9 and 1.1 so planting sounds varied
            float pitch = 0.9f + random.nextFloat() * 0.2f;
            seedSound.play(0.7f, pitch, 0f);       // slightly quieter than default
        }
    }

    /** Called when the player picks / harvests a crop. */
    public void playPickCrop() {
        if (pickCropSound != null) {
            // reduced slightly from 0.9f -> 0.8f
            pickCropSound.play(0.8f);              // <--- volume reduced
        }
    }

    /** Called when the player interacts with the vendor. */
    public void playVendor() {
        if (vendorSound != null) {
            vendorSound.play(0.9f);
        }
    }

    /** Internal helper for footsteps with slight random pitch variation. */
    private void playFootstep() {
        if (footstepSound == null) return;
        // Random pitch between ~0.9 and 1.1 so it doesn't sound robotic
        float pitch = 0.9f + random.nextFloat() * 0.2f;
        footstepSound.play(0.6f, pitch, 0f);
    }

    // -------------------------------------------------------------------------
    // AMBIENT THREAD – birds every ~30s
    // -------------------------------------------------------------------------

    private void startAmbientThread() {
        ambientThread = new Thread(() -> {
            while (ambientRunning) {
                try {
                    // Between 25 and 40 seconds
                    long sleepMs = 25000 + random.nextInt(15000);
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ignored) {
                    // Allow thread to exit if interrupted during dispose
                }

                if (!ambientRunning) break;

                // Only play birds as occasional ambient
                if (birdSound != null) {
                    Gdx.app.postRunnable(() -> birdSound.play(0.4f));
                }
            }
        }, "Ambient-Audio-Thread");

        ambientThread.setDaemon(true);
        ambientThread.start();
    }

    // -------------------------------------------------------------------------
    // DISPOSE
    // -------------------------------------------------------------------------

    public void dispose() {
        // Stop ambient thread
        ambientRunning = false;
        if (ambientThread != null && ambientThread.isAlive()) {
            ambientThread.interrupt();
        }

        // Stop current music (don't dispose here, will dispose via playlist loop)
        if (music != null) {
            music.stop();
        }

        // Dispose all music in playlist (avoid double-dispose)
        if (songs != null) {
            for (Music m : songs) {
                if (m != null) {
                    m.dispose();
                }
            }
            songs = null;
        }
        music = null;

        // Dispose all sounds
        disposeSound(waveSound);      waveSound = null;
        disposeSound(birdSound);      birdSound = null;
        disposeSound(footstepSound);  footstepSound = null;
        disposeSound(hoeLandSound);   hoeLandSound = null;
        disposeSound(pickCropSound);  pickCropSound = null;
        disposeSound(breakLandSound); breakLandSound = null;
        disposeSound(vendorSound);    vendorSound = null;
        disposeSound(burpSound);      burpSound = null;
        disposeSound(moneySound);     moneySound = null;
        disposeSound(deathSound);     deathSound = null;
        disposeSound(seedSound);      seedSound = null;
    }

    private void disposeSound(com.badlogic.gdx.audio.Sound s) {
        if (s != null) {
            s.dispose();
        }
    }
}

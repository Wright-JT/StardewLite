package io.github.example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import com.badlogic.gdx.graphics.GL20;


public class Core extends ApplicationAdapter {
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private BitmapFont font;
    private Random random = new Random();
    private boolean deathSoundPlayed = false;
    private Texture grassTexture, sandTexture;
    private Walking walking;
    private Texture wheatTexture, carrotTexture, potatoTexture, blueberryTexture;
    private Texture grass1Texture, grass2Texture, grass3Texture;
    private Texture flower1Texture, flower2Texture, flower3Texture;
    private Texture dirtTexture;
    private Texture wheatSeedTexture, carrotSeedTexture, potatoSeedTexture, blueberrySeedTexture, pathTexture, fenceTexture;
    private Texture coinTexture;
    private Texture hoeTexture;     // NEW
    private Texture farmerNpcTexture;
    private Texture[] fenceTextures; // 16 textures for each connection
    private Chat chat;
    private TiledMap oceanMap;
    private OrthogonalTiledMapRenderer oceanRenderer;
    private UI ui;

    // Health / hunger
    private float maxHealth = 100f;
    private float health = 100f;

    private float maxHunger = 100f;
    private float hunger = 100f;

    // Rates
    private static final float HUNGER_DRAIN_PER_SECOND = 0.75f;      // hunger lost per second
    private static final float HEALTH_STARVE_DRAIN_PER_SECOND = 2.0f; // health lost per second at 0 hunger
    private static final float HEALTH_REGEN_PER_SECOND = 1.5f;      // health gained per second at full hunger

    private final int[][] ISLAND_MAP = Island.DATA;
    private final int GRID_HEIGHT = Island.HEIGHT;
    private final int GRID_WIDTH = Island.WIDTH;

    private float playerX, playerY;
    private final int TILE_SIZE = 32;
    private float playerWidth, playerHeight;
    private Sound sound;
    private TileState[][] farm;
    private Crop[][] crops;
    private float[][] regrowTimers;

    private int selectedSlot = 0;

    private final int HOTBAR_SLOTS = 9;
    private final int EXTRA_SLOTS = 2;
    private final int TOTAL_SLOTS = HOTBAR_SLOTS + EXTRA_SLOTS;
    private final int[] inventory = new int[TOTAL_SLOTS];
    private final String[] inventoryItems = new String[TOTAL_SLOTS];

    private float spawnX, spawnY;
    private boolean shopOpen = false;
    private boolean shopSellTab = false;

    private Host host;
    private Client client;
    private FenceAndPath fenceAndPath;

    enum TileState { EMPTY, TILLED }
    enum CropType { WHEAT, CARROT, POTATO, BLUEBERRY }

    static class Crop {
        float growTime;
        CropType type;
        boolean fullyGrown;

        Crop(CropType type) {
            this.type = type;
            this.growTime = 0f;
            this.fullyGrown = false;
        }

        void update(float delta) {
            growTime += delta;
            if (growTime >= 20f) fullyGrown = true;
        }

        float getGrowthPercent() { return Math.min(growTime / 20f, 1f); }
        float getSize() { return 0.1f + 0.8f * getGrowthPercent(); }
    }
    @Override
    public void create() {
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, 0);
        camera.update();
        font = new BitmapFont();
        chat = new Chat();
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        ui = new UI();

        // --- Load textures ---
        hoeTexture = new Texture(Gdx.files.internal("hoe.png"));   // NEW
        grass1Texture = new Texture(Gdx.files.internal("grass1.png"));
        grass2Texture = new Texture(Gdx.files.internal("grass2.png"));
        grass3Texture = new Texture(Gdx.files.internal("grass3.png"));
        flower1Texture = new Texture(Gdx.files.internal("flower1.png"));
        flower2Texture = new Texture(Gdx.files.internal("flower2.png"));
        flower3Texture = new Texture(Gdx.files.internal("flower3.png"));
        grassTexture = new Texture(Gdx.files.internal("grass.png"));
        sandTexture = new Texture(Gdx.files.internal("sand.png"));
        wheatTexture = new Texture(Gdx.files.internal("wheat.png"));
        carrotTexture = new Texture(Gdx.files.internal("carrot.png"));
        potatoTexture = new Texture(Gdx.files.internal("potato.png"));
        blueberryTexture = new Texture(Gdx.files.internal("blueberry.png"));
        dirtTexture = new Texture(Gdx.files.internal("drytilleddirt.png"));
        wheatSeedTexture = new Texture(Gdx.files.internal("wheatseed.png"));
        carrotSeedTexture = new Texture(Gdx.files.internal("carrotseed.png"));
        potatoSeedTexture = new Texture(Gdx.files.internal("potatoseed.png"));
        blueberrySeedTexture = new Texture(Gdx.files.internal("blueberryseed.png"));
        pathTexture = new Texture(Gdx.files.internal("Stone Path.png"));
        fenceTexture = new Texture(Gdx.files.internal("fence_0.png"));
        fenceTextures = new Texture[16];
        for (int i = 0; i < 16; i++) {
            fenceTextures[i] = new Texture(Gdx.files.internal("fence_" + i + ".png"));
        }

        fenceAndPath = new FenceAndPath(GRID_WIDTH, GRID_HEIGHT, TILE_SIZE, pathTexture, fenceTextures);

        coinTexture = new Texture(Gdx.files.internal("8bitCoinPNG.png"));
        farmerNpcTexture = new Texture(Gdx.files.internal("farmer.png"));

        oceanMap = new TmxMapLoader().load("ocean.tmx");
        oceanRenderer = new OrthogonalTiledMapRenderer(oceanMap, TILE_SIZE / 8f);
        sound = new Sound();
        walking = new Walking();

        float PLAYER_SCALE = 2.2f;
        playerWidth = TILE_SIZE * PLAYER_SCALE;
        playerHeight = TILE_SIZE * PLAYER_SCALE;

        farm = new TileState[GRID_WIDTH][GRID_HEIGHT];
        crops = new Crop[GRID_WIDTH][GRID_HEIGHT];
        regrowTimers = new float[GRID_WIDTH][GRID_HEIGHT];

        for (int x = 0; x < GRID_WIDTH; x++)
            for (int y = 0; y < GRID_HEIGHT; y++) {
                farm[x][y] = TileState.EMPTY;
                regrowTimers[x][y] = 0f;
            }

        Arrays.fill(inventory, 0);

        CurrencyManager.load();
        CurrencyManager.setCurrency(0);
        CurrencyManager.save();

        for (int y = GRID_HEIGHT / 2 - 5; y < GRID_HEIGHT / 2 + 5; y++) {
            for (int x = GRID_WIDTH / 2 - 5; x < GRID_WIDTH / 2 + 5; x++) {
                if (ISLAND_MAP[y][x] == 1) {
                    playerX = x * TILE_SIZE;
                    playerY = y * TILE_SIZE;

                    // ✅ store spawn for respawn
                    spawnX = playerX;
                    spawnY = playerY;

                    break;
                }
            }
        }



        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (amountY > 0) {
                    selectedSlot = (selectedSlot + 1) % TOTAL_SLOTS;
                } else if (amountY < 0) {
                    selectedSlot = (selectedSlot - 1 + TOTAL_SLOTS) % TOTAL_SLOTS;
                }
                return true;
            }
        });
        // Skip empty overflow slots when scrolling
        if (selectedSlot >= HOTBAR_SLOTS &&
            (inventoryItems[selectedSlot] == null || inventory[selectedSlot] <= 0))
        {
            // skip forward until we hit something usable or wrap
            for (int i = 0; i < TOTAL_SLOTS; i++) {
                selectedSlot = (selectedSlot + 1) % TOTAL_SLOTS;

                if (selectedSlot < HOTBAR_SLOTS ||
                    (inventoryItems[selectedSlot] != null && inventory[selectedSlot] > 0))
                    break;
            }
        }

    }

    public Chat getChat() {
        return chat;
    }

    public void setLocalUsername(String username) {
        if (chat != null) {
            chat.setLocalUsername(username);
        }
    }

    public void setNetwork(Host host, Client client) {
        this.host = host;
        this.client = client;

        // Let chat know about the client so it can send messages out
        if (chat != null && client != null) {
            chat.setClient(client);
        }
    }

    public void receiveNetworkMessage(String message) {
        if (chat != null) {
            chat.addMessage(message);   // just display it
        }
    }
    private Texture getTextureForItem(String item) {
        if (item == null) return null;

        switch (item) {
            case "WHEAT": return wheatTexture;
            case "CARROT": return carrotTexture;
            case "POTATO": return potatoTexture;
            case "BLUEBERRY": return blueberryTexture;
            case "WHEAT_SEED": return wheatSeedTexture;
            case "CARROT_SEED": return carrotSeedTexture;
            case "POTATO_SEED": return potatoSeedTexture;
            case "BLUEBERRY_SEED": return blueberrySeedTexture;
            case "Fence": return fenceTexture;
            case "STONE PATH":
            case "Stone Path": return pathTexture;
            case "Hoe": return hoeTexture;
            default: return null;
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // Update chat input
        chat.update();
        updateNeeds(delta);
        camera.position.set(playerX + TILE_SIZE / 2f, playerY + TILE_SIZE / 2f, 0);
        camera.update();

        shapeRenderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        float PLAYER_SPEED = 150f;
        float nextX = playerX, nextY = playerY;

// Movement flags for animation
        boolean movingLeft  = false;
        boolean movingRight = false;
        boolean movingUp    = false;
        boolean movingDown  = false;

// Only move when chat is not active AND player is alive
        if (!chat.isActive() && health > 0f) {
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                nextX -= PLAYER_SPEED * delta;
                movingLeft = true;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                nextX += PLAYER_SPEED * delta;
                movingRight = true;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
                nextY += PLAYER_SPEED * delta;
                movingUp = true;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                nextY -= PLAYER_SPEED * delta;
                movingDown = true;
            }
            Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(mouse);
            int mx = (int)(mouse.x / TILE_SIZE);
            int my = (int)(mouse.y / TILE_SIZE);

            int px = (int)(playerX / TILE_SIZE);
            int py = (int)(playerY / TILE_SIZE);

            int dx = Math.abs(px - mx);
            int dy = Math.abs(py - my);
            int radius = 3;


        }

        int tx = (int) (nextX / TILE_SIZE);
        int ty = (int) (nextY / TILE_SIZE);

        if (inBounds(tx, ty) && ISLAND_MAP[ty][tx] != 0
            && fenceAndPath.getTile(tx, ty) != FenceAndPath.Tile.FENCE) {
            playerX = nextX;
            playerY = nextY;
        }

        if (sound != null) {
            sound.setMusicMuted(ui.isMusicMuted());  // you’d add setMusicMuted(...) in Sound
        }

        if (walking != null) {
            walking.update(delta, movingLeft, movingRight, movingUp, movingDown);
        }

        boolean moving = movingLeft || movingRight || movingUp || movingDown;
        sound.update(delta, moving);



        oceanRenderer.setView(camera);
        oceanRenderer.render();

        if (health > 0f && Gdx.input.justTouched()) {
            Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(mouse);
            int mx = (int) (mouse.x / TILE_SIZE);
            int my = (int) (mouse.y / TILE_SIZE);

            if (inBounds(mx, my)) {
                float distX = Math.abs((int) (playerX / TILE_SIZE) - mx);
                float distY = Math.abs((int) (playerY / TILE_SIZE) - my);
                if (distX <= 2 && distY <= 2) {
                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT))
                        handleLeftClick(mx, my);
                    else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT))
                        handleRightClick(mx, my);
                }
            }
        }
        // --- Eating food with E ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            String item = inventoryItems[selectedSlot];

            if (item != null &&
                (item.equals("CARROT") || item.equals("POTATO") || item.equals("BLUEBERRY"))) {

                // consume one from inventory
                inventory[selectedSlot]--;
                if (inventory[selectedSlot] <= 0) {
                    inventory[selectedSlot] = 0;
                    inventoryItems[selectedSlot] = null;
                }

                float amount = 0f;
                if (item.equals("CARROT"))    amount = 15f;
                else if (item.equals("POTATO"))   amount = 25f;
                else if (item.equals("BLUEBERRY")) amount = 40f;

                hunger = Math.min(maxHunger, hunger + amount);
                if (sound != null) sound.playBurp();
            }
        }

        for (int x = 0; x < GRID_WIDTH; x++)
            for (int y = 0; y < GRID_HEIGHT; y++) {
                if (crops[x][y] != null) crops[x][y].update(delta);
                if (regrowTimers[x][y] > 0) {
                    regrowTimers[x][y] -= delta;
                    if (regrowTimers[x][y] <= 0) {
                        double chance = random.nextDouble();
                        if (chance < 0.1) {
                            Island.DECOR[y][x] = random.nextInt(3) + 1;
                            Island.FLIP[y][x] = random.nextBoolean();
                        } else if (chance < 0.15) {
                            Island.FLOWER[y][x] = random.nextInt(3) + 1;
                            Island.FLOWER_FLIP[y][x] = random.nextBoolean();
                        }
                    }
                }
            }

        batch.begin();
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                if (ISLAND_MAP[y][x] == 1) {
                    batch.draw(grassTexture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    int decor = Island.DECOR[y][x];
                    if (decor > 0) {
                        Texture decorTex = decor == 1 ? grass1Texture :
                            decor == 2 ? grass2Texture : grass3Texture;
                        boolean flip = Island.FLIP[y][x];
                        float size = TILE_SIZE * 0.9f;
                        float offset = (TILE_SIZE - size) / 2f;
                        if (flip)
                            batch.draw(decorTex, x * TILE_SIZE + offset + size, y * TILE_SIZE + offset, -size, size);
                        else
                            batch.draw(decorTex, x * TILE_SIZE + offset, y * TILE_SIZE + offset, size, size);
                    }
                    int flower = Island.FLOWER[y][x];
                    if (flower > 0) {
                        Texture flowerTex = flower == 1 ? flower1Texture :
                            flower == 2 ? flower2Texture : flower3Texture;
                        boolean flip = Island.FLOWER_FLIP[y][x];
                        float size = TILE_SIZE * 0.9f;
                        float offset = (TILE_SIZE - size) / 2f;
                        if (flip)
                            batch.draw(flowerTex, x * TILE_SIZE + offset + size, y * TILE_SIZE + offset, -size, size);
                        else
                            batch.draw(flowerTex, x * TILE_SIZE + offset, y * TILE_SIZE + offset, size, size);
                    }
                } else if (ISLAND_MAP[y][x] == 2) {
                    batch.draw(sandTexture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }

                if (farm[x][y] == TileState.TILLED) {
                    batch.draw(dirtTexture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
                Crop c = crops[x][y];
                if (c != null) {
                    float size = c.getSize() * TILE_SIZE;
                    float offset = (TILE_SIZE - size) / 2f;

                    Texture cropTex = null;
                    switch (c.type) {
                        case WHEAT:
                            cropTex = wheatTexture;
                            break;
                        case CARROT:
                            cropTex = carrotTexture;
                            break;
                        case POTATO:
                            cropTex = potatoTexture;
                            break;
                        case BLUEBERRY:
                            cropTex = blueberryTexture;
                            break;
                    }

                    if (cropTex != null) {
                        batch.draw(cropTex, x * TILE_SIZE + offset, y * TILE_SIZE + offset, size, size);
                    }
                }
            }
        }
        fenceAndPath.render(batch);
        batch.end();
        drawNPCs();

        batch.begin();
        batch.draw(
            walking.getCurrentFrame(),
            playerX - (playerWidth - TILE_SIZE) / 2f,
            playerY - (playerHeight - TILE_SIZE) / 2f,
            playerWidth,
            playerHeight
        );
        batch.end();


        handleShopInteraction();
        drawInventory();
        drawCurrencyHUD();
        if (shopOpen) drawShopWindow();
        chat.draw(shapeRenderer, batch, font);

///  Draw health/hunger + death overlay, and check for respawn click
        boolean respawnClicked = ui.draw(shapeRenderer, batch, font,
            health, maxHealth, hunger, maxHunger);
        if (respawnClicked) {
            respawnPlayer();
        }

//  Options: Close Game
        if (ui.pollCloseRequested()) {
            Gdx.app.exit();
        }
    }



    private void handleTileToggle(int x, int y) {
        if (!inBounds(x, y) || ISLAND_MAP[y][x] != 1) return;

        // If there's a crop here, harvest instead of modifying the soil
        if (crops[x][y] != null) {
            handleHarvest(x, y);
            return;
        }

        // No crop: toggle tilled state
        if (farm[x][y] == TileState.EMPTY) {
            farm[x][y] = TileState.TILLED;
            Island.DECOR[y][x] = 0;
            Island.FLOWER[y][x] = 0;
        } else {
            farm[x][y] = TileState.EMPTY;
            crops[x][y] = null;
            regrowTimers[x][y] = 60f;
        }
    }
    private void respawnPlayer() {
        // Reset stats
        health = maxHealth;
        hunger = maxHunger;
        deathSoundPlayed = false;
        playerX = spawnX;
        playerY = spawnY;
        CurrencyManager.setCurrency(0);
        CurrencyManager.save();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            inventory[i] = 0;
            inventoryItems[i] = null;
        }
    }

    private void updateNeeds(float delta) {
        // Hunger drains over time
        hunger -= HUNGER_DRAIN_PER_SECOND * delta;
        if (hunger < 0f) hunger = 0f;
        if (hunger > maxHunger) hunger = maxHunger;

        // If hunger is 0, health drains
        if (hunger <= 0f) {
            health -= HEALTH_STARVE_DRAIN_PER_SECOND * delta;
        }
        // If hunger is full, health slowly regenerates
        else if (hunger >= maxHunger / 10) {
            health += HEALTH_REGEN_PER_SECOND * delta;
        }
        if (health < 0f) health = 0f;
        if (health > maxHealth) health = maxHealth;

        if (health == 0f && !deathSoundPlayed) {
            deathSoundPlayed = true;
            if (sound != null) sound.playDeath();
        }
    }


    private void handleLeftClick(int x, int y) {
        if (!inBounds(x, y)) return;

        FenceAndPath.Tile tile = fenceAndPath.getTile(x, y);

        if (tile == FenceAndPath.Tile.FENCE) {
            fenceAndPath.removeFence(x, y);
            if (sound != null) sound.playBreakLand(); // or a dedicated break sound
            return; // stop here, don’t also till/harvest
        }

        if (tile == FenceAndPath.Tile.PATH) {
            fenceAndPath.removePath(x, y);
            if (sound != null) sound.playBreakLand();
            return;
        }

        // Harvest if a crop is grown
        Crop crop = crops[x][y];
        if (crop != null && crop.fullyGrown) {
            handleHarvest(x, y);
            return;
        }
        // If there’s no crop, untill the soil
        if (farm[x][y] == TileState.TILLED && crops[x][y] == null) {
            farm[x][y] = TileState.EMPTY;
            if (sound != null) sound.playBreakLand();
            regrowTimers[x][y] = 60f; // start regrow timer for decor/grass
        }
    }

    private void handleRightClick(int x, int y) {
        if (!inBounds(x, y)) return;

        int px = (int)(playerX / TILE_SIZE);
        int py = (int)(playerY / TILE_SIZE);

        float distX = Math.abs(px - x);
        float distY = Math.abs(py - y);
        if (distX > 2 || distY > 2) return;
        String item = inventoryItems[selectedSlot];

        // --- If there's a fully grown crop, harvest it ---
        if (crops[x][y] != null) {
            handleHarvest(x, y);
            return;
        }

        if (item != null && inventory[selectedSlot] > 0) {

            if ("Fence".equals(item)) {
                if (canPlaceStructure(x, y)) {
                    fenceAndPath.placeFence(x, y);
                    if (sound != null) sound.playFence();
                    inventory[selectedSlot]--;
                    if (inventory[selectedSlot] <= 0) inventoryItems[selectedSlot] = null;
                    return;
                }
            }

            if ("Stone Path".equals(item) || "STONE PATH".equals(item)) {
                if (canPlaceStructure(x, y)) {
                    fenceAndPath.placePath(x, y);
                    if (sound != null) sound.playPath();
                    inventory[selectedSlot]--;
                    if (inventory[selectedSlot] <= 0) inventoryItems[selectedSlot] = null;
                    return;
                }
            }
        }
        if ("Hoe".equals(item)) {
            if (farm[x][y] == TileState.EMPTY &&
                fenceAndPath.getTile(x, y) != FenceAndPath.Tile.FENCE &&
                fenceAndPath.getTile(x, y) != FenceAndPath.Tile.PATH) {

                farm[x][y] = TileState.TILLED;
                Island.DECOR[y][x] = 0;
                Island.FLOWER[y][x] = 0;
                if (sound != null) sound.playHoeLand();
                return;  // stop further processing
            }
        }
        // --- If soil is TILLED, try to plant seeds ---
        if (farm[x][y] == TileState.TILLED && crops[x][y] == null) {
            item = inventoryItems[selectedSlot]; // re-read
            if (item == null || inventory[selectedSlot] <= 0) return;
            if (fenceAndPath.getTile(x, y) == FenceAndPath.Tile.PATH) return;

            CropType type = null;
            if ("WHEAT_SEED".equals(item)) type = CropType.WHEAT;
            else if ("CARROT_SEED".equals(item)) type = CropType.CARROT;
            else if ("POTATO_SEED".equals(item)) type = CropType.POTATO;
            else if ("BLUEBERRY_SEED".equals(item)) type = CropType.BLUEBERRY;
            if (sound != null) sound.playSeed();
            if (type != null) {
                crops[x][y] = new Crop(type);
                inventory[selectedSlot]--;
                if (inventory[selectedSlot] <= 0)
                    inventoryItems[selectedSlot] = null;
            }
        }
    }



    private void handleHarvest(int x, int y) {
        if (!inBounds(x, y)) return;
        Crop crop = crops[x][y];
        if (crop != null && crop.fullyGrown) {
            int slotIndex = -1;
            for (int i = 0; i < TOTAL_SLOTS; i++)
                if (inventoryItems[i] == null || inventoryItems[i].equals(crop.type.toString())) {
                    slotIndex = i; break;
                }
            if (slotIndex != -1) {
                if (inventoryItems[slotIndex] == null)
                    inventoryItems[slotIndex] = crop.type.toString();
                inventory[slotIndex]++;
            }
            int reward = 0;
            switch (crop.type) {
                case WHEAT:
                    reward = 10;
                    break;
                case CARROT:
                    reward = 20;
                    break;
                case POTATO:
                    reward = 50;
                    break;
                case BLUEBERRY:
                    reward = 100;
                    break;
            }
            crops[x][y] = null;
            if (sound != null) sound.playPickCrop();
        }
    }
    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < GRID_WIDTH && y < GRID_HEIGHT;
    }
    private boolean isWalkable(int x, int y) {
        if (!inBounds(x, y)) return false;
        if (ISLAND_MAP[y][x] == 0) return false;
        if (fenceAndPath.getTile(x, y) == FenceAndPath.Tile.FENCE) return false;

        return true;
    }
    private void drawInventory() {
        int slotSize = 40, spacing = 10;
        int totalWidth = TOTAL_SLOTS * slotSize + (TOTAL_SLOTS - 1) * spacing;
        int startX = (Gdx.graphics.getWidth() - totalWidth) / 2;
        int y = 20;
        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(
            0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < TOTAL_SLOTS; i++) {

            int xPos = startX + i * (slotSize + spacing);
            int border = 2;
            shapeRenderer.setColor(0f, 0f, 0f, 1f);
            shapeRenderer.rect(xPos - border, y - border,
                slotSize + border * 2, slotSize + border * 2);
            float r = (i == selectedSlot) ? 0.7f : 0.5f;
            float g = 0.7f;
            float b = 0.7f;

            shapeRenderer.setColor(r, g, b, 1f);
            shapeRenderer.rect(xPos, y, slotSize, slotSize);
        }
        shapeRenderer.end();
        batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(
            0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        batch.begin();

        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (inventory[i] > 0 && inventoryItems[i] != null) {

                Texture tex = getTextureForItem(inventoryItems[i]);

                if (tex != null) {
                    float size = slotSize * 0.9f;
                    float off  = (slotSize - size) / 2f;

                    batch.draw(tex,
                        startX + i * (slotSize + spacing) + off,
                        y + off,
                        size, size);
                }

                font.draw(batch, String.valueOf(inventory[i]),
                    startX + i * (slotSize + spacing) + slotSize - 14,
                    y + slotSize - 6);
            }
        }
        batch.end();
    }

    private void drawCurrencyHUD() {
        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(
            0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(
            0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        int padding = 12;
        int iconSize = 32;
        String text = String.valueOf(CurrencyManager.getCurrency());
        int panelW = 160;
        int panelH = 54;
        int x = 14;
        int y = Gdx.graphics.getHeight() - panelH - 14;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.25f);   // 40% opaque
        shapeRenderer.rect(x, y, panelW, panelH);
        shapeRenderer.setColor(1f, 1f, 1f, 0.15f);  // highlight stripe
        shapeRenderer.rect(x, y + panelH - 4, panelW, 4);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        batch.draw(coinTexture, x + padding, y + (panelH - iconSize) / 2 - 1, iconSize, iconSize);
        font.getData().setScale(2.0f);
        font.setColor(0, 0, 0, 0.7f);
        font.draw(batch, text, x + padding + iconSize + 14, y + panelH - 16 - 2);
        font.setColor(Color.WHITE);
        font.draw(batch, text, x + padding + iconSize + 12, y + panelH - 16);
        font.getData().setScale(1.0f);
        batch.end();
    }


    private void drawNPCs() {
        List<Island.NPC> npcs = Island.NPCS;
        if (npcs == null || npcs.isEmpty()) return;
        batch.begin();
        for (Island.NPC npc : npcs) {
            if (npc.type == Island.NPCType.FARMER) {
                float nx = npc.x * TILE_SIZE;
                float ny = npc.y * TILE_SIZE;
                float scale = 2.0f;
                float w = TILE_SIZE * scale, h = TILE_SIZE * scale;
                batch.draw(farmerNpcTexture,
                    nx - (w - TILE_SIZE) / 2f,
                    ny - (h - TILE_SIZE) / 2f,
                    w, h);
                if (isNearPlayer(npc.x, npc.y, 2)) {
                    String prompt = "Press E to Trade";
                    font.getData().setScale(1.25f);
                    font.setColor(Color.YELLOW);
                    GlyphLayout layout = new GlyphLayout(font, prompt);
                    float textX = nx + (w - layout.width) / 1.5f;
                    float textY = ny + h + 16;
                    font.draw(batch, layout, textX, textY);
                    font.setColor(Color.WHITE);
                }

            }
        }
        batch.end();
    }

    private boolean isNearPlayer(int tx, int ty, int radiusTiles) {
        int px = (int)(playerX / TILE_SIZE);
        int py = (int)(playerY / TILE_SIZE);
        return Math.abs(px - tx) <= radiusTiles && Math.abs(py - ty) <= radiusTiles;
    }

    private boolean hasItem(String name) {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (name.equals(inventoryItems[i]) && inventory[i] > 0) {
                return true;
            }
        }
        return false;
    }

    private void buyItemExact(String item, int cost) {
        if (CurrencyManager.getCurrency() < cost) return;

        // find empty slot
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (inventoryItems[i] == null) {
                inventoryItems[i] = item;
                inventory[i] = 1;
                CurrencyManager.addCurrency(-cost);
                return;
            }
        }
    }

    private void handleShopInteraction() {
        if (!shopOpen) {
            for (Island.NPC npc : Island.NPCS) {
                if (npc.type == Island.NPCType.FARMER && isNearPlayer(npc.x, npc.y, 2)) {
                    if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                        shopOpen = true;
                        // Play vendor sound ONCE when shop opens
                        if (sound != null) sound.playVendor();
                    }
                }
            }
        } else {
            // Close shop
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                shopOpen = false;
                return;
            }

            // Toggle buy/sell tab
            if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
                shopSellTab = !shopSellTab;
                if (sound != null) sound.playVendor(); // optional click sound
            }

            if (shopSellTab) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                    int s = sellAllOf("WHEAT");
                    if (s > 0) {
                        CurrencyManager.addCurrency(s * 10);
                        if (sound != null) sound.playMoney();
                    }
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                    int s = sellAllOf("CARROT");
                    if (s > 0) {
                        CurrencyManager.addCurrency(s * 20);
                        if (sound != null) sound.playMoney();
                    }
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
                    int s = sellAllOf("POTATO");
                    if (s > 0) {
                        CurrencyManager.addCurrency(s * 50);
                        if (sound != null) sound.playMoney();
                    }
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
                    int s = sellAllOf("BLUEBERRY");
                    if (s > 0) {
                        CurrencyManager.addCurrency(s * 100);
                        if (sound != null) sound.playMoney();
                    }
                }

            } else {
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                    if (!hasItem("Hoe")) {
                        buyItemExact("Hoe", 0);
                    }
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                    buySeed("WHEAT_SEED", 0);
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
                    buySeed("CARROT_SEED", 10);
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
                    buySeed("POTATO_SEED", 20);
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {
                    buySeed("BLUEBERRY_SEED", 40);

                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) {
                    buyItem("Fence", 30);

                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7)) {
                    buyItem("Stone Path", 10);

                }
            }
        }
    }


    private void buySeed(String seed, int cost) {
        if (CurrencyManager.getCurrency() < cost) return;
        int slot = -1;
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (inventoryItems[i] == null || inventoryItems[i].equals(seed)) { slot = i; break; }
        }
        if (slot != -1) {
            if (inventoryItems[slot] == null) inventoryItems[slot] = seed;
            inventory[slot]++;
            CurrencyManager.addCurrency(-cost);
        }
    }

    private void buyItem(String item, int cost) {
        if (CurrencyManager.getCurrency() < cost) return;

        int slot = -1;

        // Check if the item already exists in inventory
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (inventoryItems[i] != null && inventoryItems[i].equals(item)) {
                slot = i;
                break;
            }
        }

        // If it doesn't exist, find an empty slot
        if (slot == -1) {
            for (int i = 0; i < TOTAL_SLOTS; i++) {
                if (inventoryItems[i] == null) {
                    slot = i;
                    break;
                }
            }
        }

        // If there's a slot, add to it
        if (slot != -1) {
            if (inventoryItems[slot] == null) inventoryItems[slot] = item;
            inventory[slot] += 10; // for example, give 10 items per purchase
            CurrencyManager.addCurrency(-cost);
        }
    }

    private void drawShopWindow() {
        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(
            0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(
            0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        int w = 420, h = 260;
        int x = (Gdx.graphics.getWidth() - w) / 2;
        int y = (Gdx.graphics.getHeight() - h) / 2;

        // --- Enable blending so transparency works ---
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Transparent dark overlay (background fade)
        shapeRenderer.setColor(0f, 0f, 0f, 0.30f); // 30% opaque
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Shop panel (slightly transparent)
        shapeRenderer.setColor(0.10f, 0.10f, 0.15f, 0.90f);
        shapeRenderer.rect(x, y, w, h);

        shapeRenderer.end();

        // Turn blending off (optional)
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.getData().setScale(1.2f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Farmer's Stand (" + (shopSellTab ? "Sell" : "Buy") + ")", x + 14, y + h - 16);
        font.getData().setScale(1.0f);

        if (shopSellTab) {
            font.draw(batch, "[1] Sell all WHEAT (+10)", x + 14, y + h - 42);
            font.draw(batch, "[2] Sell all CARROT (+20)", x + 14, y + h - 68);
            font.draw(batch, "[3] Sell all POTATO (+50)", x + 14, y + h - 94);
            font.draw(batch, "[4] Sell all BLUEBERRY (+100)", x + 14, y + h - 120);
            font.setColor(Color.GRAY);
            font.draw(batch, "TAB to switch to Buy, ESC to close", x + 14, y + 20);
        } else {
            font.draw(batch, "[1] Buy Hoe (0)", x + 14, y + h - 42);
            font.draw(batch, "[2] Buy WHEAT SEED (0)", x + 14, y + h - 68);
            font.draw(batch, "[3] Buy CARROT SEED (10)", x + 14, y + h - 94);
            font.draw(batch, "[4] Buy POTATO SEED (20)", x + 14, y + h - 120);
            font.draw(batch, "[5] Buy BLUEBERRY SEED (40)", x + 14, y + h - 146);
            font.draw(batch, "[6] Buy Fence (30)", x + 14, y + h - 172);
            font.draw(batch, "[7] Buy Stone Path (10)", x + 14, y + h - 198);
            font.setColor(Color.GRAY);
            font.draw(batch, "TAB to switch to Sell, ESC to close", x + 14, y + 20);

        }

        font.setColor(Color.WHITE);
        batch.end();
    }


    private int sellAllOf(String type) {
        int total = 0;
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (inventoryItems[i] != null && inventoryItems[i].equals(type)) {
                total += inventory[i];
                inventory[i] = 0;
                inventoryItems[i] = null;
            }
        }
        return total;
    }

    private boolean canPlaceStructure(int x, int y) {
        return crops[x][y] == null
            && farm[x][y] != TileState.TILLED   // <-- prevent placement on tilled land
            && fenceAndPath.getTile(x, y) == FenceAndPath.Tile.EMPTY;
    }

    @Override
    public void dispose() {
        CurrencyManager.save();

        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
        if (walking != null) walking.dispose();
        grassTexture.dispose();
        sandTexture.dispose();
        wheatTexture.dispose();
        carrotTexture.dispose();
        potatoTexture.dispose();
        blueberryTexture.dispose();
        flower1Texture.dispose();
        flower2Texture.dispose();
        flower3Texture.dispose();
        oceanMap.dispose();
        oceanRenderer.dispose();
        grass1Texture.dispose();
        grass2Texture.dispose();
        grass3Texture.dispose();
        pathTexture.dispose();
        fenceTexture.dispose();
        for (Texture t : fenceTextures) {
            if (t != null) t.dispose();
        }
        if (sound != null) sound.dispose();
        if (coinTexture != null) coinTexture.dispose();
        if (dirtTexture != null) dirtTexture.dispose();
        if (farmerNpcTexture != null) farmerNpcTexture.dispose();
        if (wheatSeedTexture != null) wheatSeedTexture.dispose();
        if (carrotSeedTexture != null) carrotSeedTexture.dispose();
        if (potatoSeedTexture != null) potatoSeedTexture.dispose();
        if (blueberrySeedTexture != null) blueberrySeedTexture.dispose();
        if (ui != null) ui.dispose();
    }
}

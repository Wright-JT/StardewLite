package io.github.example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
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

public class Core extends ApplicationAdapter {
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private BitmapFont font;
    private Random random = new Random();

    // textures
    private Texture playerTexture, grassTexture, sandTexture, wheatTexture, carrotTexture;
    private Texture grass1Texture, grass2Texture, grass3Texture;
    private Texture flower1Texture, flower2Texture, flower3Texture;
    private Texture coinTexture;
    private Texture tradingBoatTexture;

    private static final int TRADE_RADIUS_TILES = 3;

    // ocean background (tmx)
    private TiledMap oceanMap;
    private OrthogonalTiledMapRenderer oceanRenderer;

    private final int[][] ISLAND_MAP = Island.DATA;
    private final int GRID_HEIGHT = Island.HEIGHT;
    private final int GRID_WIDTH = Island.WIDTH;

    private float playerX, playerY;
    private final int TILE_SIZE = 32;
    private float playerWidth, playerHeight;

    private TileState[][] farm;
    private Crop[][] crops;
    private float[][] regrowTimers;

    private final int HOTBAR_SLOTS = 9;
    private int selectedSlot = 0;
    private final int[] inventory = new int[HOTBAR_SLOTS];
    private final String[] inventoryItems = new String[HOTBAR_SLOTS];

    private boolean shopOpen = false;

    // Day/Night + needs
    private float timeOfDay = 8f;     // 0..24
    private int dayCount = 1;
    private final float dayLengthSeconds = 420f; // ~7 min per day

    private float hunger = 100f;   // 0..100 (higher is better)
    private float tiredness = 0f;  // 0..100 (higher is worse)
    private final float hungerDrainPerSec = 0.12f;
    private final float tiredGainPerSecDay = 0.08f;
    private final float tiredGainPerSecNight = 0.14f;
    private float deprivationTimer = 0f;
    private final float DEPRIVATION_LIMIT = 15f;

    enum TileState { EMPTY, TILLED }
    enum CropType { WHEAT, CARROT }

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

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();

        // textures
        grass1Texture = new Texture(Gdx.files.internal("grass1.png"));
        grass2Texture = new Texture(Gdx.files.internal("grass2.png"));
        grass3Texture = new Texture(Gdx.files.internal("grass3.png"));
        flower1Texture = new Texture(Gdx.files.internal("flower1.png"));
        flower2Texture = new Texture(Gdx.files.internal("flower2.png"));
        flower3Texture = new Texture(Gdx.files.internal("flower3.png"));

        playerTexture = new Texture(Gdx.files.internal("farmer.png"));
        grassTexture = new Texture(Gdx.files.internal("grass.png"));
        sandTexture = new Texture(Gdx.files.internal("sand.png"));
        wheatTexture = new Texture(Gdx.files.internal("wheat.png"));
        carrotTexture = new Texture(Gdx.files.internal("carrot.png"));
        coinTexture = new Texture(Gdx.files.internal("8bitCoinPNG.png"));
        tradingBoatTexture = new Texture(Gdx.files.internal("tradingboat.png"));

        // ocean
        oceanMap = new TmxMapLoader().load("ocean.tmx");
        oceanRenderer = new OrthogonalTiledMapRenderer(oceanMap, TILE_SIZE / 8f);

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

        // load persisted currency
        CurrencyManager.load();

        // spawn on land near center
        boolean spawned = false;
        for (int y = GRID_HEIGHT / 2 - 5; y < GRID_HEIGHT / 2 + 5 && !spawned; y++) {
            for (int x = GRID_WIDTH / 2 - 5; x < GRID_WIDTH / 2 + 5; x++) {
                if (ISLAND_MAP[y][x] == 1) {
                    playerX = x * TILE_SIZE;
                    playerY = y * TILE_SIZE;
                    spawned = true;
                    break;
                }
            }
        }

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (amountY > 0) selectedSlot = (selectedSlot + 1) % HOTBAR_SLOTS;
                else if (amountY < 0) selectedSlot = (selectedSlot - 1 + HOTBAR_SLOTS) % HOTBAR_SLOTS;
                return true;
            }
        });
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        updateClockAndNeeds(delta);

        camera.position.set(playerX + TILE_SIZE / 2f, playerY + TILE_SIZE / 2f, 0);
        camera.update();

        shapeRenderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        // movement (hunger/tiredness penalties)
        float baseSpeed = 150f;
        float staminaFactor = 0.5f + 0.5f * (hunger / 100f);         // 0.5..1.0
        float fatigueFactor = 0.7f + 0.3f * (1f - tiredness / 100f); // 0.7..1.0
        float PLAYER_SPEED = baseSpeed * staminaFactor * fatigueFactor;

        float nextX = playerX, nextY = playerY;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))  nextX -= PLAYER_SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) nextX += PLAYER_SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.UP))    nextY += PLAYER_SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN))  nextY -= PLAYER_SPEED * delta;

        int tx = (int) (nextX / TILE_SIZE);
        int ty = (int) (nextY / TILE_SIZE);
        if (inBounds(tx, ty) && ISLAND_MAP[ty][tx] != 0) { // block water
            playerX = nextX;
            playerY = nextY;
        }

        // ocean underlay
        oceanRenderer.setView(camera);
        oceanRenderer.render();

        // needs actions
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) eatOne();
        if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) sleepNow();

        // mouse interactions
        if (!shopOpen && Gdx.input.justTouched()) {
            Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(mouse);
            int mx = (int) (mouse.x / TILE_SIZE);
            int my = (int) (mouse.y / TILE_SIZE);
            if (inBounds(mx, my)) {
                float distX = Math.abs((int) (playerX / TILE_SIZE) - mx);
                float distY = Math.abs((int) (playerY / TILE_SIZE) - my);
                if (distX <= 2 && distY <= 2) {
                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT))  handleTileToggle(mx, my);
                    else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) handleHarvest(mx, my);
                }
            }
        }

        // seeds (only when shop closed)
        if (!shopOpen &&
            (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUM_2))) {
            int targetX = (int) (playerX / TILE_SIZE);
            int targetY = (int) (playerY / TILE_SIZE);
            if (inBounds(targetX, targetY) && farm[targetX][targetY] == TileState.TILLED && crops[targetX][targetY] == null) {
                CropType type = Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) ? CropType.WHEAT : CropType.CARROT;
                crops[targetX][targetY] = new Crop(type);
            }
        }

        // update crops + regrowth timers
        for (int x = 0; x < GRID_WIDTH; x++) {
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
        }

        // draw ground & decor
        batch.begin();
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                if (ISLAND_MAP[y][x] == 1) {
                    batch.draw(grassTexture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);

                    int decor = Island.DECOR[y][x];
                    if (decor > 0) {
                        Texture t = decor == 1 ? grass1Texture : (decor == 2 ? grass2Texture : grass3Texture);
                        boolean flip = Island.FLIP[y][x];
                        float size = TILE_SIZE * 0.9f, off = (TILE_SIZE - size) / 2f;
                        if (flip) batch.draw(t, x * TILE_SIZE + off + size, y * TILE_SIZE + off, -size, size);
                        else      batch.draw(t, x * TILE_SIZE + off, y * TILE_SIZE + off, size, size);
                    }

                    int flower = Island.FLOWER[y][x];
                    if (flower > 0) {
                        Texture t = flower == 1 ? flower1Texture : (flower == 2 ? flower2Texture : flower3Texture);
                        boolean flip = Island.FLOWER_FLIP[y][x];
                        float size = TILE_SIZE * 0.9f, off = (TILE_SIZE - size) / 2f;
                        if (flip) batch.draw(t, x * TILE_SIZE + off + size, y * TILE_SIZE + off, -size, size);
                        else      batch.draw(t, x * TILE_SIZE + off, y * TILE_SIZE + off, size, size);
                    }

                } else if (ISLAND_MAP[y][x] == 2) {
                    batch.draw(sandTexture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
        batch.end();

        // tilled soil & crops
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                if (farm[x][y] == TileState.TILLED) {
                    shapeRenderer.setColor(0.55f, 0.27f, 0.07f, 1);
                    shapeRenderer.rect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
                Crop c = crops[x][y];
                if (c != null) {
                    float size = c.getSize() * TILE_SIZE;
                    float off = (TILE_SIZE - size) / 2f;
                    // simple color block per crop
                    if (c.type == CropType.WHEAT) shapeRenderer.setColor(1f, 1f, 0f, 1f);
                    else                          shapeRenderer.setColor(1f, 0.55f, 0.1f, 1f);
                    shapeRenderer.rect(x * TILE_SIZE + off, y * TILE_SIZE + off, size, size);
                }
            }
        }
        shapeRenderer.end();

        // NPCs (boat + vendor)
        drawNPCs();

        // day/night overlay (brighter night)
        drawDayNightOverlay();

        // player
        batch.begin();
        batch.draw(playerTexture,
            playerX - (playerWidth - TILE_SIZE) / 2f,
            playerY - (playerHeight - TILE_SIZE) / 2f,
            playerWidth, playerHeight);
        batch.end();

        // trading
        handleShopInteraction();

        // HUD
        drawInventory();
        drawCurrencyHUD();
        drawNeedsHUD();
        drawClockHUD();
        if (shopOpen) drawShopWindow();
    }

    private void handleTileToggle(int x, int y) {
        if (!inBounds(x, y) || ISLAND_MAP[y][x] != 1) return;

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

    private void handleHarvest(int x, int y) {
        if (!inBounds(x, y)) return;
        Crop crop = crops[x][y];
        if (crop != null && crop.fullyGrown) {
            int slotIndex = -1;
            for (int i = 0; i < HOTBAR_SLOTS; i++) {
                if (inventoryItems[i] == null || inventoryItems[i].equals(crop.type.toString())) {
                    slotIndex = i; break;
                }
            }
            if (slotIndex != -1) {
                if (inventoryItems[slotIndex] == null)
                    inventoryItems[slotIndex] = crop.type.toString();
                inventory[slotIndex]++;
            }
            int reward = (crop.type == CropType.WHEAT) ? 10 : 20;
            CurrencyManager.addCurrency(reward);
            crops[x][y] = null;
        }
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < GRID_WIDTH && y < GRID_HEIGHT;
    }

    private void drawInventory() {
        int slotSize = 40, spacing = 10;
        int totalWidth = HOTBAR_SLOTS * slotSize + (HOTBAR_SLOTS - 1) * spacing;
        int startX = (Gdx.graphics.getWidth() - totalWidth) / 2, y = 20;

        // screen-space
        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int xPos = startX + i * (slotSize + spacing);
            int border = 2;
            shapeRenderer.setColor(0f, 0f, 0f, 1f);
            shapeRenderer.rect(xPos - border, y - border, slotSize + border * 2, slotSize + border * 2);
            shapeRenderer.setColor(i == selectedSlot ? 0.7f : 0.5f, 0.7f, 0.7f, 1f);
            shapeRenderer.rect(xPos, y, slotSize, slotSize);
        }
        shapeRenderer.end();

        batch.begin();
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            if (inventory[i] > 0 && inventoryItems[i] != null) {
                Texture tex = inventoryItems[i].equals("WHEAT") ? wheatTexture :
                    inventoryItems[i].equals("CARROT") ? carrotTexture : null;
                if (tex != null) {
                    float size = slotSize * 0.9f, off = (slotSize - size) / 2f;
                    batch.draw(tex, startX + i * (slotSize + spacing) + off, y + off, size, size);
                }
                font.draw(batch, String.valueOf(inventory[i]),
                    startX + i * (slotSize + spacing) + slotSize - 14, y + slotSize - 6);
            }
        }
        batch.end();
    }

    private void drawCurrencyHUD() {
        // screen-space
        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        int padding = 12;
        int iconSize = 32;
        String text = String.valueOf(CurrencyManager.getCurrency());

        int panelW = 160;
        int panelH = 54;
        int x = 14;
        int y = Gdx.graphics.getHeight() - panelH - 14;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.45f);
        shapeRenderer.rect(x, y, panelW, panelH);
        shapeRenderer.setColor(1f, 1f, 1f, 0.12f);
        shapeRenderer.rect(x, y + panelH - 4, panelW, 4);
        shapeRenderer.end();

        batch.begin();
        batch.draw(coinTexture, x + padding, y + (panelH - iconSize) / 2, iconSize, iconSize);
        font.getData().setScale(2.0f);
        font.setColor(0, 0, 0, 0.7f);
        font.draw(batch, text, x + padding + iconSize + 12 + 2, y + panelH - 16 - 2);
        font.setColor(Color.WHITE);
        font.draw(batch, text, x + padding + iconSize + 12, y + panelH - 16);
        font.getData().setScale(1.0f);
        batch.end();
    }

    private void drawNeedsHUD() {
        // screen-space
        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        int x = 14, y = 14 + 54 + 12; // below currency panel
        int w = 220, h = 16, gap = 8;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f,0f,0f,0.55f);
        shapeRenderer.rect(x, y, w, h);
        shapeRenderer.rect(x, y + h + gap, w, h);

        // hunger bar
        shapeRenderer.setColor(0.95f, 0.55f, 0.15f, 1f);
        shapeRenderer.rect(x+2, y+2, (w-4) * clamp01(hunger/100f), h-4);

        // tiredness bar (how tired you are)
        shapeRenderer.setColor(0.25f, 0.55f, 1f, 1f);
        shapeRenderer.rect(x+2, y + h + gap + 2, (w-4) * clamp01(tiredness/100f), h-4);
        shapeRenderer.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Hunger", x + 4, y + h - 2);
        font.draw(batch, "Tiredness", x + 4, y + h + gap + h - 2);
        font.setColor(Color.GRAY);
        font.draw(batch, "F=Eat   Z=Sleep   E=Trade", x, y + h + gap + h + 18);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void drawClockHUD() {
        String timeStr = String.format("Day %d  %02d:%02d",
            dayCount, (int) timeOfDay, (int) ((timeOfDay - (int)timeOfDay) * 60));
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, timeStr,
            camera.position.x + Gdx.graphics.getWidth()/2f - 140,
            camera.position.y + Gdx.graphics.getHeight()/2f - 20);
        batch.end();
    }

    // boat + vendor (farmer.png), with prompt when close
    // boat + vendor (use boat texture only for trader), with prompt when close
    private void drawNPCs() {
        if (Island.NPCS == null || Island.NPCS.isEmpty()) return;

        batch.begin();

        // draw the boat prop
        if (Island.SAILBOAT != null && tradingBoatTexture != null) {
            float bx = Island.SAILBOAT.x * TILE_SIZE;
            float by = Island.SAILBOAT.y * TILE_SIZE;
            float boatSize = TILE_SIZE * 2.0f;
            float off = (boatSize - TILE_SIZE) / 2f;
            batch.draw(tradingBoatTexture, bx - off, by - off, boatSize, boatSize);

            // show prompt at boat when close enough to trade
            if (isNearPlayer(Island.SAILBOAT.x, Island.SAILBOAT.y, TRADE_RADIUS_TILES)) {
                font.setColor(Color.YELLOW);
                font.draw(batch, "Press E to Trade", bx - 16, by + boatSize + 8);
                font.setColor(Color.WHITE);
            }
        }

        // draw any non-vendor NPCs (we currently add none, but keep logic)
        for (Island.NPC npc : Island.NPCS) {
            if (npc.type == Island.NPCType.VENDOR) continue; // vendor shown as boat only
            float nx = npc.x * TILE_SIZE;
            float ny = npc.y * TILE_SIZE;
            float scale = 2.0f;
            float w = TILE_SIZE * scale, h = TILE_SIZE * scale;
            // reuse farmer texture for generic NPCs if ever added
            batch.draw(playerTexture, nx - (w - TILE_SIZE) / 2f, ny - (h - TILE_SIZE) / 2f, w, h);
        }

        batch.end();
    }


    private boolean isNearPlayer(int tx, int ty, int radiusTiles) {
        int px = (int)(playerX / TILE_SIZE);
        int py = (int)(playerY / TILE_SIZE);
        return Math.abs(px - tx) <= radiusTiles && Math.abs(py - ty) <= radiusTiles;
    }

    private void handleShopInteraction() {
        boolean nearVendor = false;
        for (Island.NPC npc : Island.NPCS) {
            if (npc.type == Island.NPCType.VENDOR && isNearPlayer(npc.x, npc.y, TRADE_RADIUS_TILES)) {
                nearVendor = true;
                break;
            }
        }

        if (!shopOpen) {
            if (nearVendor && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                shopOpen = true;
            }
        } else {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                shopOpen = false;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                int sold = sellAllOf("WHEAT");
                CurrencyManager.addCurrency(sold * 10);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                int sold = sellAllOf("CARROT");
                CurrencyManager.addCurrency(sold * 20);
            }
        }
    }

    private void drawShopWindow() {
        // screen-space
        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        int w = 420, h = 160;
        int x = (Gdx.graphics.getWidth() - w) / 2;
        int y = (Gdx.graphics.getHeight() - h) / 2;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.5f);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        shapeRenderer.setColor(0.08f, 0.08f, 0.1f, 0.95f);
        shapeRenderer.rect(x, y, w, h);
        shapeRenderer.setColor(1f, 1f, 1f, 0.12f);
        shapeRenderer.rect(x, y + h - 6, w, 6);
        shapeRenderer.end();

        batch.begin();
        font.getData().setScale(1.2f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Vendor's Boat", x + 14, y + h - 16);
        font.getData().setScale(1.0f);

        font.draw(batch, "[1] Sell all WHEAT  (+10 each)", x + 14, y + h - 42);
        font.draw(batch, "[2] Sell all CARROT (+20 each)", x + 14, y + h - 68);
        font.setColor(Color.GRAY);
        font.draw(batch, "ESC to close", x + 14, y + 20);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private int sellAllOf(String type) {
        int total = 0;
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            if (inventoryItems[i] != null && inventoryItems[i].equals(type)) {
                total += inventory[i];
                inventory[i] = 0;
                inventoryItems[i] = null;
            }
        }
        return total;
    }

    private void updateClockAndNeeds(float delta) {
        // clock
        float hoursPerSec = 24f / dayLengthSeconds;
        timeOfDay += hoursPerSec * delta;
        if (timeOfDay >= 24f) { timeOfDay -= 24f; dayCount++; }

        boolean isNight = (timeOfDay >= 20f || timeOfDay < 6f);

        // needs
        hunger = Math.max(0f, hunger - hungerDrainPerSec * delta);
        float tiredGain = isNight ? tiredGainPerSecNight : tiredGainPerSecDay;
        tiredness = Math.min(100f, tiredness + tiredGain * delta);

        // deprivation -> blackout sleep
        if (hunger <= 0f || tiredness >= 100f) {
            deprivationTimer += delta;
            if (deprivationTimer > DEPRIVATION_LIMIT) {
                sleepNow();
                deprivationTimer = 0f;
            }
        } else {
            deprivationTimer = 0f;
        }
    }

    private void sleepNow() {
        // sleep 8 hours
        advanceTime(8f);
        tiredness = 0f;
        hunger = Math.max(0f, hunger - 15f); // got a bit hungrier
    }

    private void advanceTime(float hours) {
        timeOfDay += hours;
        while (timeOfDay >= 24f) { timeOfDay -= 24f; dayCount++; }
    }

    private void eatOne() {
        int slot = findFirst("CARROT");
        if (slot == -1) slot = findFirst("WHEAT");
        if (slot != -1) {
            String t = inventoryItems[slot];
            if ("CARROT".equals(t)) {
                hunger = Math.min(100f, hunger + 35f);
                tiredness = Math.max(0f, tiredness - 10f);
            } else if ("WHEAT".equals(t)) {
                hunger = Math.min(100f, hunger + 20f);
                tiredness = Math.max(0f, tiredness - 5f);
            }
            inventory[slot]--;
            if (inventory[slot] <= 0) {
                inventory[slot] = 0;
                inventoryItems[slot] = null;
            }
        }
    }

    private int findFirst(String item) {
        for (int i = 0; i < HOTBAR_SLOTS; i++)
            if (inventory[i] > 0 && item.equals(inventoryItems[i])) return i;
        return -1;
    }

    // brighter, moonlit night overlay
    private void drawDayNightOverlay() {
        float t = (timeOfDay % 24f) / 24f; // 0..1
        float sun = (float) Math.cos((t - 0.5f) * Math.PI * 2.0);
        sun = Math.max(0f, sun);

        final float MIN_NIGHT_BRIGHTNESS = 0.58f; // brighter base night
        final float MAX_NIGHT_ALPHA      = 0.40f; // not too dark

        float brightness = MIN_NIGHT_BRIGHTNESS + (1f - MIN_NIGHT_BRIGHTNESS) * sun;
        float alpha = Math.min(1f - brightness, MAX_NIGHT_ALPHA);

        float r = 0.00f, g = 0.00f, b = 0.08f;

        float vw = Gdx.graphics.getWidth();
        float vh = Gdx.graphics.getHeight();

        shapeRenderer.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(r, g, b, alpha);
        shapeRenderer.rect(camera.position.x - vw / 2f,
            camera.position.y - vh / 2f,
            vw, vh);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    @Override
    public void dispose() {
        CurrencyManager.save();

        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
        playerTexture.dispose();
        grassTexture.dispose();
        sandTexture.dispose();
        wheatTexture.dispose();
        carrotTexture.dispose();
        flower1Texture.dispose();
        flower2Texture.dispose();
        flower3Texture.dispose();
        oceanMap.dispose();
        oceanRenderer.dispose();
        grass1Texture.dispose();
        grass2Texture.dispose();
        grass3Texture.dispose();
        coinTexture.dispose();
        if (tradingBoatTexture != null) tradingBoatTexture.dispose();
    }
}

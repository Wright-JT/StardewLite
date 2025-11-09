package io.github.example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
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

    private Texture playerTexture, grassTexture, sandTexture;
    private Texture wheatTexture, carrotTexture, potatoTexture, blueberryTexture;
    private Texture grass1Texture, grass2Texture, grass3Texture;
    private Texture flower1Texture, flower2Texture, flower3Texture;
    private Texture dirtTexture;

    private Texture wheatSeedTexture, carrotSeedTexture, potatoSeedTexture, blueberrySeedTexture;

    private Texture coinTexture;
    private Texture farmerNpcTexture;

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
    private boolean shopSellTab = true;

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

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();

        // --- Load textures ---
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
        potatoTexture = new Texture(Gdx.files.internal("potato.png"));
        blueberryTexture = new Texture(Gdx.files.internal("blueberry.png"));
        dirtTexture = new Texture(Gdx.files.internal("drytilleddirt.png"));
        wheatSeedTexture = new Texture(Gdx.files.internal("wheatseed.png"));
        carrotSeedTexture = new Texture(Gdx.files.internal("carrotseed.png"));
        potatoSeedTexture = new Texture(Gdx.files.internal("potatoseed.png"));
        blueberrySeedTexture = new Texture(Gdx.files.internal("blueberryseed.png"));

        coinTexture = new Texture(Gdx.files.internal("8bitCoinPNG.png"));
        farmerNpcTexture = new Texture(Gdx.files.internal("pngtree-farmer-pixel-art-character-icon-design-png-image_8744094.png"));

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

        CurrencyManager.load();
        CurrencyManager.setCurrency(0);
        CurrencyManager.save();

        for (int y = GRID_HEIGHT / 2 - 5; y < GRID_HEIGHT / 2 + 5; y++) {
            for (int x = GRID_WIDTH / 2 - 5; x < GRID_WIDTH / 2 + 5; x++) {
                if (ISLAND_MAP[y][x] == 1) {
                    playerX = x * TILE_SIZE;
                    playerY = y * TILE_SIZE;
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
        camera.position.set(playerX + TILE_SIZE / 2f, playerY + TILE_SIZE / 2f, 0);
        camera.update();

        shapeRenderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        float PLAYER_SPEED = 150f;
        float nextX = playerX, nextY = playerY;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) nextX -= PLAYER_SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) nextX += PLAYER_SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) nextY += PLAYER_SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) nextY -= PLAYER_SPEED * delta;

        int tx = (int) (nextX / TILE_SIZE);
        int ty = (int) (nextY / TILE_SIZE);
        if (inBounds(tx, ty) && ISLAND_MAP[ty][tx] != 0) {
            playerX = nextX;
            playerY = nextY;
        }

        oceanRenderer.setView(camera);
        oceanRenderer.render();

        if (Gdx.input.justTouched()) {
            Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(mouse);
            int mx = (int) (mouse.x / TILE_SIZE);
            int my = (int) (mouse.y / TILE_SIZE);

            if (inBounds(mx, my)) {
                float distX = Math.abs((int)(playerX / TILE_SIZE) - mx);
                float distY = Math.abs((int)(playerY / TILE_SIZE) - my);
                if (distX <= 2 && distY <= 2) {
                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT))
                        handleLeftClick(mx, my);
                    else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT))
                        handleRightClick(mx, my);
                }
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

                // ✅ Draw tilled soil (use dirt texture)
                if (farm[x][y] == TileState.TILLED) {
                    batch.draw(dirtTexture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }

                /// ✅ Draw crop
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
        batch.end();
        drawNPCs();

        batch.begin();
        batch.draw(playerTexture,
            playerX - (playerWidth - TILE_SIZE) / 2f,
            playerY - (playerHeight - TILE_SIZE) / 2f,
            playerWidth, playerHeight);
        batch.end();

        handleShopInteraction();
        drawInventory();
        drawCurrencyHUD();
        if (shopOpen) drawShopWindow();
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

    private void handleLeftClick(int x, int y) {
        if (!inBounds(x, y)) return;

        // Harvest if a crop is grown
        Crop crop = crops[x][y];
        if (crop != null && crop.fullyGrown) {
            handleHarvest(x, y);
            return;
        }

        // If there’s no crop, and the tile is tilled — untill it
        if (farm[x][y] == TileState.TILLED && crops[x][y] == null) {
            farm[x][y] = TileState.EMPTY;
            regrowTimers[x][y] = 60f; // start regrow timer for decor/grass
        }
    }


    private void handleRightClick(int x, int y) {
        if (!inBounds(x, y)) return;

        float distX = Math.abs((int)(playerX / TILE_SIZE) - x);
        float distY = Math.abs((int)(playerY / TILE_SIZE) - y);
        if (distX > 2 || distY > 2) return;

        // --- If there's a fully grown crop, harvest it ---
        if (crops[x][y] != null) {
            handleHarvest(x, y);
            return;
        }

        // --- If soil is EMPTY, till it ---
        if (ISLAND_MAP[y][x] == 1 && farm[x][y] == TileState.EMPTY) {
            farm[x][y] = TileState.TILLED;
            Island.DECOR[y][x] = 0;
            Island.FLOWER[y][x] = 0;
            return;
        }

        // --- If soil is TILLED, try to plant seeds ---
        if (farm[x][y] == TileState.TILLED && crops[x][y] == null) {
            String item = inventoryItems[selectedSlot];
            if (item == null || inventory[selectedSlot] <= 0) return;

            CropType type = null;
            if ("WHEAT_SEED".equals(item)) type = CropType.WHEAT;
            else if ("CARROT_SEED".equals(item)) type = CropType.CARROT;
            else if ("POTATO_SEED".equals(item)) type = CropType.POTATO;
            else if ("BLUEBERRY_SEED".equals(item)) type = CropType.BLUEBERRY;

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
            for (int i = 0; i < HOTBAR_SLOTS; i++)
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
        }
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < GRID_WIDTH && y < GRID_HEIGHT;
    }

    private void drawInventory() {
        int slotSize = 40, spacing = 10;
        int totalWidth = HOTBAR_SLOTS * slotSize + (HOTBAR_SLOTS - 1) * spacing;
        int startX = (Gdx.graphics.getWidth() - totalWidth) / 2, y = 20;

        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(
            0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int xPos = startX + i * (slotSize + spacing);
            int border = 2;
            shapeRenderer.setColor(0f, 0f, 0f, 1f);
            shapeRenderer.rect(xPos - border, y - border,
                slotSize + border * 2, slotSize + border * 2);
            shapeRenderer.setColor(i == selectedSlot ? 0.7f : 0.5f, 0.7f, 0.7f, 1f);
            shapeRenderer.rect(xPos, y, slotSize, slotSize);
        }
        shapeRenderer.end();

        batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(
            0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        batch.begin();
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            if (inventory[i] > 0 && inventoryItems[i] != null) {
                Texture tex = null;
                String item = inventoryItems[i];
                if ("WHEAT".equals(item)) tex = wheatTexture;
                else if ("CARROT".equals(item)) tex = carrotTexture;
                else if ("POTATO".equals(item)) tex = potatoTexture;
                else if ("BLUEBERRY".equals(item)) tex = blueberryTexture;
                else if ("WHEAT_SEED".equals(item)) tex = wheatSeedTexture;
                else if ("CARROT_SEED".equals(item)) tex = carrotSeedTexture;
                else if ("POTATO_SEED".equals(item)) tex = potatoSeedTexture;
                else if ("BLUEBERRY_SEED".equals(item)) tex = blueberrySeedTexture;

                if (tex != null) {
                    float size = slotSize * 0.9f;
                    float off = (slotSize - size) / 2f;
                    batch.draw(tex, startX + i * (slotSize + spacing) + off,
                        y + off, size, size);
                }
                font.draw(batch, String.valueOf(inventory[i]),
                    startX + i * (slotSize + spacing) + slotSize - 14,
                    y + slotSize - 6);
            }
        }
        batch.end();
    }


    private void drawCurrencyHUD() {
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
        batch.draw(coinTexture, x + padding, y + (panelH - iconSize) / 2 - 1, iconSize, iconSize);
        font.getData().setScale(2.0f);
        font.setColor(0, 0, 0, 0.7f);
        font.draw(batch, text, x + padding + iconSize + 12 + 2, y + panelH - 16 - 2);
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
                    font.setColor(Color.YELLOW);
                    font.draw(batch, "Press E to Trade", nx - 10, ny + h + 16);
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

    private void handleShopInteraction() {
        if (!shopOpen) {
            for (Island.NPC npc : Island.NPCS) {
                if (npc.type == Island.NPCType.FARMER && isNearPlayer(npc.x, npc.y, 2)) {
                    if (Gdx.input.isKeyJustPressed(Input.Keys.E)) shopOpen = true;
                }
            }
        } else {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) shopOpen = false;
            if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) shopSellTab = !shopSellTab;

            if (shopSellTab) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) { int s = sellAllOf("WHEAT"); CurrencyManager.addCurrency(s * 10); }
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) { int s = sellAllOf("CARROT"); CurrencyManager.addCurrency(s * 20); }
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) { int s = sellAllOf("POTATO"); CurrencyManager.addCurrency(s * 50); }
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) { int s = sellAllOf("BLUEBERRY"); CurrencyManager.addCurrency(s * 100); }
            } else {
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) buySeed("WHEAT_SEED", 0);
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) buySeed("CARROT_SEED", 10);
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) buySeed("POTATO_SEED", 20);
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) buySeed("BLUEBERRY_SEED", 40);
            }
        }
    }

    private void buySeed(String seed, int cost) {
        if (CurrencyManager.getCurrency() < cost) return;
        int slot = -1;
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            if (inventoryItems[i] == null || inventoryItems[i].equals(seed)) { slot = i; break; }
        }
        if (slot != -1) {
            if (inventoryItems[slot] == null) inventoryItems[slot] = seed;
            inventory[slot]++;
            CurrencyManager.addCurrency(-cost);
        }
    }

    private void drawShopWindow() {
        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        int w = 420, h = 200;
        int x = (Gdx.graphics.getWidth() - w) / 2;
        int y = (Gdx.graphics.getHeight() - h) / 2;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.5f);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.setColor(0.08f, 0.08f, 0.1f, 0.95f);
        shapeRenderer.rect(x, y, w, h);
        shapeRenderer.end();

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
            font.draw(batch, "[1] Buy WHEAT SEED (0)", x + 14, y + h - 42);
            font.draw(batch, "[2] Buy CARROT SEED (10)", x + 14, y + h - 68);
            font.draw(batch, "[3] Buy POTATO SEED (20)", x + 14, y + h - 94);
            font.draw(batch, "[4] Buy BLUEBERRY SEED (40)", x + 14, y + h - 120);
            font.setColor(Color.GRAY);
            font.draw(batch, "TAB to switch to Sell, ESC to close", x + 14, y + 20);
        }
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
        if (coinTexture != null) coinTexture.dispose();
        if (dirtTexture != null) dirtTexture.dispose();
        if (farmerNpcTexture != null) farmerNpcTexture.dispose();
        if (wheatSeedTexture != null) wheatSeedTexture.dispose();
        if (carrotSeedTexture != null) carrotSeedTexture.dispose();
        if (potatoSeedTexture != null) potatoSeedTexture.dispose();
        if (blueberrySeedTexture != null) blueberrySeedTexture.dispose();
    }
}

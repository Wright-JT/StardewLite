package io.github.example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
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
import java.util.Random;

public class Core extends ApplicationAdapter {
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private BitmapFont font;
    private Random random = new Random();

    private Texture playerTexture, grassTexture, sandTexture, wheatTexture, carrotTexture;
    private Texture grass1Texture, grass2Texture, grass3Texture;
    private Texture flower1Texture, flower2Texture, flower3Texture;
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

        // --- find spawn on land near center ---
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

        // --- handle mouse clicks ---
        if (Gdx.input.justTouched()) {
            Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(mouse);
            int mx = (int) (mouse.x / TILE_SIZE);
            int my = (int) (mouse.y / TILE_SIZE);
            if (inBounds(mx, my)) {
                float distX = Math.abs((int) (playerX / TILE_SIZE) - mx);
                float distY = Math.abs((int) (playerY / TILE_SIZE) - my);
                if (distX <= 2 && distY <= 2) {
                    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT))
                        handleTileToggle(mx, my); // till/un-till
                    else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT))
                        handleHarvest(mx, my); // pick crop
                }
            }
        }

        // --- planting seeds ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            int targetX = (int) (playerX / TILE_SIZE);
            int targetY = (int) (playerY / TILE_SIZE);
            if (inBounds(targetX, targetY) && farm[targetX][targetY] == TileState.TILLED && crops[targetX][targetY] == null) {
                CropType type = Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) ? CropType.WHEAT : CropType.CARROT;
                crops[targetX][targetY] = new Crop(type);
            }
        }

        // --- update crops and regrowth ---
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

        // --- draw terrain and decor ---
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
            }
        }
        batch.end();

        // --- draw soil and crops ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x < GRID_WIDTH; x++)
            for (int y = 0; y < GRID_HEIGHT; y++) {
                if (farm[x][y] == TileState.TILLED) {
                    shapeRenderer.setColor(0.55f, 0.27f, 0.07f, 1);
                    shapeRenderer.rect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
                Crop c = crops[x][y];
                if (c != null) {
                    float size = c.getSize() * TILE_SIZE;
                    float offset = (TILE_SIZE - size) / 2f;
                    shapeRenderer.setColor(c.type == CropType.WHEAT ? 1f : 1f, c.type == CropType.WHEAT ? 1f : 0.55f, c.type == CropType.WHEAT ? 0f : 0.1f, 1f);
                    shapeRenderer.rect(x * TILE_SIZE + offset, y * TILE_SIZE + offset, size, size);
                }
            }
        shapeRenderer.end();

        // --- draw player ---
        batch.begin();
        batch.draw(playerTexture,
            playerX - (playerWidth - TILE_SIZE) / 2f,
            playerY - (playerHeight - TILE_SIZE) / 2f,
            playerWidth, playerHeight);
        batch.end();

        drawInventory();
    }

    // --- till/un-till logic ---
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

    // --- right-click harvest only ---
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

        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0,
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

        batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
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

    @Override
    public void dispose() {
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
    }
}

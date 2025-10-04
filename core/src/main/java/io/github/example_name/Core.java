package io.github.example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.Arrays;

public class Core extends ApplicationAdapter {
    private ShapeRenderer shapeRenderer;
    private final int GRID_WIDTH = 40;
    private final int GRID_HEIGHT = 32;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private BitmapFont font;

    private int playerX = 5;
    private int playerY = 4;
    private TileState[][] farm;
    private Crop[][] crops;
    // Inventory fields
    private final int HOTBAR_SLOTS = 9;
    private int selectedSlot = 0; // currently selected slot
    private final int[] inventory = new int[HOTBAR_SLOTS]; // empty by default
    private final String[] inventoryItems = new String[HOTBAR_SLOTS]; // e.g., "Wheat", "Carrot"

    // Extend your TileState enum
    enum TileState {
        EMPTY,   // Grass
        TILLED,  // Tilled soil
        PLANTED  // Optional, could be same as TILLED with crop
    }

    static class Crop {
        float growTime;      // total time in seconds
        CropType type;       // WHEAT, CARROT
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

        float getGrowthPercent() {
            return Math.min(growTime / 20f, 1f);
        }

        float getSize() {
            // Crop visually grows from 0.1 to 0.9 of the tile
            float min = 0.1f;
            float max = 0.9f;
            return min + (max - min) * getGrowthPercent();
        }
    }



    @Override
    public void create() {
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(Gdx.graphics.getWidth()/2f, Gdx.graphics.getHeight()/2f, 0);
        camera.update();

        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(Gdx.graphics.getWidth()/2f, Gdx.graphics.getHeight()/2f, 0);
        camera.update();
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont(); // default font

        shapeRenderer = new ShapeRenderer();

        farm = new TileState[GRID_WIDTH][GRID_HEIGHT];
        crops = new Crop[GRID_WIDTH][GRID_HEIGHT];
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                farm[x][y] = TileState.EMPTY;
                crops[x][y] = null;
            }
        }
        // start empty
        Arrays.fill(inventory, 0);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        int TILE_SIZE = 32;
        camera.position.set(playerX * TILE_SIZE + TILE_SIZE /2f, playerY * TILE_SIZE + TILE_SIZE /2f, 0);
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        // --- Movement ---
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.LEFT)) playerX--;
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.RIGHT)) playerX++;
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP)) playerY++;
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN)) playerY--;

        playerX = Math.max(0, Math.min(playerX, GRID_WIDTH - 1));
        playerY = Math.max(0, Math.min(playerY, GRID_HEIGHT - 1));

        // --- Handle SPACE key ---
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE)) {
            TileState current = farm[playerX][playerY];
            Crop crop = crops[playerX][playerY];

            if (current == TileState.EMPTY) {
                // Grass → till soil
                farm[playerX][playerY] = TileState.TILLED;
            } else if (current == TileState.TILLED && crop == null) {
                // Tilled soil → back to grass
                farm[playerX][playerY] = TileState.EMPTY;
            } else if (crop != null) {
                // If crop fully grown → harvest
                if (crop.fullyGrown) {
                    int slotIndex = -1;
                    switch (crop.type) {
                        case WHEAT:
                            slotIndex = 0;
                            break;
                        case CARROT:
                            slotIndex = 1;
                            break;
                    }

                    if (slotIndex >= 0) {
                        // If slot empty, set the item type
                        if (inventoryItems[slotIndex] == null) {
                            inventoryItems[slotIndex] = crop.type.toString();
                            inventory[slotIndex] = 1;
                        } else {
                            inventory[slotIndex]++;
                        }
                    }

                    // Remove crop from farm
                    crops[playerX][playerY] = null;
                    farm[playerX][playerY] = TileState.TILLED;
                } else {
                    // Not fully grown → just remove crop and keep tilled soil
                    crops[playerX][playerY] = null;
                }
            }
        }

        // --- Plant crops ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) && farm[playerX][playerY] == TileState.TILLED) {
            crops[playerX][playerY] = new Crop(CropType.WHEAT);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) && farm[playerX][playerY] == TileState.TILLED) {
            crops[playerX][playerY] = new Crop(CropType.CARROT);
        }


        // --- Update crops ---
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                if (crops[x][y] != null) {
                    crops[x][y].update(delta);
                }
            }
        }

        // --- Hotbar scrolling ---
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.A)) {
            selectedSlot = (selectedSlot - 1 + HOTBAR_SLOTS) % HOTBAR_SLOTS;
        }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.D)) {
            selectedSlot = (selectedSlot + 1) % HOTBAR_SLOTS;
        }

        // --- Draw farm and crops ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                // Soil
                if (farm[x][y] == TileState.EMPTY) shapeRenderer.setColor(0.2f, 0.6f, 0.2f, 1);
                else if (farm[x][y] == TileState.TILLED) shapeRenderer.setColor(0.55f, 0.27f, 0.07f, 1);
                shapeRenderer.rect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);

                // Crop
                Crop crop = crops[x][y];
                if (crop != null) {
                    float size = crop.getSize() * TILE_SIZE;
                    float offset = (TILE_SIZE - size) / 2f;

                    if (crop.type == CropType.WHEAT) shapeRenderer.setColor(1f, 1f, 0f, 1f);
                    else if (crop.type == CropType.CARROT) shapeRenderer.setColor(1f, 0.55f, 0.1f, 1f);

                    shapeRenderer.rect(x * TILE_SIZE + offset, y * TILE_SIZE + offset, size, size);
                }
            }
        }

        // --- Draw player ---
        shapeRenderer.setColor(1, 0, 0, 1);
        shapeRenderer.rect(playerX * TILE_SIZE, playerY * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        shapeRenderer.end();

        // --- Draw inventory slots ---
        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        int slotCount = HOTBAR_SLOTS;
        int slotSize = 40;
        int spacing = 10;
        int totalWidth = slotCount * slotSize + (slotCount - 1) * spacing;
        int startX = (Gdx.graphics.getWidth() - totalWidth) / 2;
        int y = 20;

        for (int i = 0; i < slotCount; i++) {
            // Draw slot background
            if (i == selectedSlot) shapeRenderer.setColor(0.7f, 0.7f, 0.7f, 1);
            else shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1);
            shapeRenderer.rect(startX + i * (slotSize + spacing), y, slotSize, slotSize);

            // Draw crop inside slot if any
            if (inventory[i] > 0 && inventoryItems[i] != null) {
                float cropSize = slotSize * 0.9f;
                float offset = (slotSize - cropSize) / 2f;

                // Set color based on crop type
                if (inventoryItems[i].equals("WHEAT")) shapeRenderer.setColor(1f, 1f, 0f, 1f);         // yellow
                else if (inventoryItems[i].equals("CARROT")) shapeRenderer.setColor(1f, 0.55f, 0.1f, 1f); // orange

                shapeRenderer.rect(startX + i * (slotSize + spacing) + offset, y + offset, cropSize, cropSize);
            }
        }
        shapeRenderer.end();

// --- Draw inventory numbers ---
        batch.begin();
        for (int i = 0; i < slotCount; i++) {
            if (inventory[i] > 0 && inventoryItems[i] != null) {
                String text = String.valueOf(inventory[i]);
                font.draw(batch, text, startX + i * (slotSize + spacing) + slotSize - 10, y + slotSize - 5);
            }
        }
        batch.end();

    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
    }

    enum CropType {
        WHEAT,
        CARROT
    }
}

package io.github.example_name;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class FenceAndPath {

    public enum Tile { EMPTY, FENCE, PATH }

    private Tile[][] grid;
    private int width, height;
    private int tileSize;

    private Texture[] fenceTextures; // 16 textures for each connection combo
    private Texture pathTexture;

    public FenceAndPath(int width, int height, int tileSize, Texture pathTexture, Texture[] fenceTextures) {
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.pathTexture = pathTexture;
        this.fenceTextures = fenceTextures;

        grid = new Tile[width][height];
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                grid[x][y] = Tile.EMPTY;
    }

    public void placeFence(int x, int y) {
        if (!inBounds(x, y)) return;
        grid[x][y] = Tile.FENCE;

        updateNeighbors(x, y);
    }

    public void removeFence(int x, int y) {
        if (!inBounds(x, y)) return;
        if (grid[x][y] == Tile.FENCE) grid[x][y] = Tile.EMPTY;
    }

    public void placePath(int x, int y) {
        if (!inBounds(x, y)) return;
        grid[x][y] = Tile.PATH;
    }

    public Tile getTile(int x, int y) {
        if (!inBounds(x, y)) return Tile.EMPTY;
        return grid[x][y];
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    private boolean hasFence(int x, int y) {
        return inBounds(x, y) && grid[x][y] == Tile.FENCE;
    }

    /**
     * Compute the index for fence texture based on neighboring fences.
     * Bitmask order: up=1, down=2, left=4, right=8
     */
    private int getFenceIndex(int x, int y) {
        int index = 0;
        if (hasFence(x, y + 1)) index |= 1;    // up
        if (hasFence(x, y - 1)) index |= 2;    // down
        if (hasFence(x - 1, y)) index |= 4;    // left
        if (hasFence(x + 1, y)) index |= 8;    // right
        return index;
    }

    public void render(SpriteBatch batch) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float drawX = x * tileSize;
                float drawY = y * tileSize;

                switch (grid[x][y]) {
                    case FENCE:
                        int idx = getFenceIndex(x, y);
                        batch.draw(fenceTextures[idx], drawX, drawY, tileSize, tileSize);
                        break;
                    case PATH:
                        batch.draw(pathTexture, drawX, drawY, tileSize, tileSize);
                        break;
                }
            }
        }
    }
    private void updateNeighbors(int x, int y) {
        refreshFence(x, y);
        refreshFence(x + 1, y);
        refreshFence(x - 1, y);
        refreshFence(x, y + 1);
        refreshFence(x, y - 1);
    }

    private void refreshFence(int x, int y) {
        if (!inBounds(x, y)) return;
        if (grid[x][y] == Tile.FENCE) {
            // Just recomputing connection index happens automatically in render(),
            // so nothing is needed here unless you cache textures.
        }
    }
}

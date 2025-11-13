package io.github.example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

public class UI {

    // --- Stats Bar Layout ---
    private final int margin = 16;
    private final int barWidth = 220;
    private final int barHeight = 20;
    private final int labelOffsetY = 18;

    // --- Options Button ---
    private Texture optionsTexture = new Texture(Gdx.files.internal("options.png"));
    private final int optionsSize = 48;   // CHANGE SIZE HERE
    private boolean optionsOpen = false;

    // --- Options Window Buttons ---
    private final int windowWidth = 360;
    private final int windowHeight = 220;
    private final int buttonWidth = 220;
    private final int buttonHeight = 48;
    private final int buttonSpacing = 14;

    private boolean closeRequested = false;

    public UI() {}

    // Called after UI.draw() from Core
    public boolean pollCloseRequested() {
        boolean temp = closeRequested;
        closeRequested = false;
        return temp;
    }

    public void dispose() {
        if (optionsTexture != null) optionsTexture.dispose();
    }

    // --------------------------------------------------------------------------
    //  MAIN UI DRAW
    // --------------------------------------------------------------------------
    public boolean draw(ShapeRenderer shapeRenderer,
                        SpriteBatch batch,
                        BitmapFont font,
                        float health, float maxHealth,
                        float hunger, float maxHunger) {

        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        // projection
        Matrix4 uiMatrix = new Matrix4().setToOrtho2D(0, 0, screenW, screenH);
        shapeRenderer.setProjectionMatrix(uiMatrix);
        batch.setProjectionMatrix(uiMatrix);

        float healthRatio = Math.min(1f, Math.max(0f, health / maxHealth));
        float hungerRatio = Math.min(1f, Math.max(0f, hunger / maxHunger));

        // positions
        int healthX = margin;
        int healthY = margin;

        int hungerX = screenW - barWidth - margin;
        int hungerY = margin;

        // --- Draw health + hunger ---
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // health
        shapeRenderer.setColor(0f, 0f, 0f, 0.4f);
        shapeRenderer.rect(healthX, healthY, barWidth, barHeight);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(healthX, healthY, barWidth * healthRatio, barHeight);

        // hunger
        shapeRenderer.setColor(0f, 0f, 0f, 0.4f);
        shapeRenderer.rect(hungerX, hungerY, barWidth, barHeight);
        shapeRenderer.setColor(Color.ORANGE);
        shapeRenderer.rect(hungerX, hungerY, barWidth * hungerRatio, barHeight);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // labels
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "HEALTH", healthX, healthY + barHeight + labelOffsetY);
        font.draw(batch, "HUNGER (E to eat)", hungerX, hungerY + barHeight + labelOffsetY);
        batch.end();

        // --- Draw options icon ---
        drawOptionsIcon(batch);

        // If the options window is open — draw it
        if (optionsOpen) {
            drawOptionsWindow(shapeRenderer, batch, font, screenW, screenH);
        }

        // Death screen
        if (health <= 0) {
            return drawDeathOverlay(shapeRenderer, batch, font, screenW, screenH);
        }

        return false;
    }

    // --------------------------------------------------------------------------
    // OPTIONS ICON
    // --------------------------------------------------------------------------
    private void drawOptionsIcon(SpriteBatch batch) {
        batch.begin();

        int x = Gdx.graphics.getWidth() - optionsSize - 16;
        int y = Gdx.graphics.getHeight() - optionsSize - 16;

        batch.draw(optionsTexture, x, y, optionsSize, optionsSize);
        batch.end();

        // click detection
        if (Gdx.input.justTouched()) {
            int mx = Gdx.input.getX();
            int my = Gdx.graphics.getHeight() - Gdx.input.getY();

            if (mx >= x && mx <= x + optionsSize &&
                my >= y && my <= y + optionsSize) {

                optionsOpen = !optionsOpen;
            }
        }
    }

    // --------------------------------------------------------------------------
    // OPTIONS WINDOW
    // --------------------------------------------------------------------------
    private void drawOptionsWindow(ShapeRenderer shapeRenderer,
                                   SpriteBatch batch,
                                   BitmapFont font,
                                   int screenW, int screenH) {

        int x = (screenW - windowWidth) / 2;
        int y = (screenH - windowHeight) / 2;

        // fade background
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.35f);
        shapeRenderer.rect(0, 0, screenW, screenH);

        shapeRenderer.setColor(0.12f, 0.12f, 0.16f, 0.92f);
        shapeRenderer.rect(x, y, windowWidth, windowHeight);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.3f);
        font.draw(batch, "Options", x + 20, y + windowHeight - 18);
        font.getData().setScale(1f);
        batch.end();

        // BUTTON POSITIONS
        int closeY = y + windowHeight / 2 - 10;
        int cancelY = closeY - buttonHeight - buttonSpacing;
        int btnX = x + (windowWidth - buttonWidth) / 2;

        drawButton("Close Game", btnX, closeY, shapeRenderer, batch, font);
        drawButton("Cancel", btnX, cancelY, shapeRenderer, batch, font);

        // Handle clicking the buttons
        if (Gdx.input.justTouched()) {
            int mx = Gdx.input.getX();
            int my = screenH - Gdx.input.getY();

            // Close Game
            if (mx >= btnX && mx <= btnX + buttonWidth &&
                my >= closeY && my <= closeY + buttonHeight) {
                closeRequested = true;
            }

            // Cancel → close window
            if (mx >= btnX && mx <= btnX + buttonWidth &&
                my >= cancelY && my <= cancelY + buttonHeight) {
                optionsOpen = false;
            }
        }
    }

    private void drawButton(String text, int x, int y,
                            ShapeRenderer shapeRenderer,
                            SpriteBatch batch,
                            BitmapFont font) {

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.8f);
        shapeRenderer.rect(x, y, buttonWidth, buttonHeight);
        shapeRenderer.setColor(1f, 1f, 1f, 0.12f);
        shapeRenderer.rect(x, y + buttonHeight - 3, buttonWidth, 3);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        GlyphLayout layout = new GlyphLayout();
        layout.setText(font, text);
        float tx = x + (buttonWidth - layout.width) / 2;
        float ty = y + buttonHeight / 2 + layout.height / 2;

        font.setColor(Color.WHITE);
        font.draw(batch, text, tx, ty);
        batch.end();
    }

    // --------------------------------------------------------------------------
    // DEATH OVERLAY
    // --------------------------------------------------------------------------
    private boolean drawDeathOverlay(ShapeRenderer shapeRenderer,
                                     SpriteBatch batch,
                                     BitmapFont font,
                                     int screenW,
                                     int screenH) {

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.5f, 0f, 0f, 0.7f);
        shapeRenderer.rect(0, 0, screenW, screenH);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.getData().setScale(2f);
        font.setColor(Color.WHITE);
        GlyphLayout layout = new GlyphLayout(font, "You died!");
        font.draw(batch, layout,
            (screenW - layout.width) / 2f,
            screenH / 2f + 70f);
        font.getData().setScale(1f);
        batch.end();

        int btnW = 240;
        int btnH = 55;
        int btnX = (screenW - btnW) / 2;
        int btnY = screenH / 2 - 20;

        drawButton("Respawn", btnX, btnY, shapeRenderer, batch, font);

        if (Gdx.input.justTouched()) {
            int mx = Gdx.input.getX();
            int my = screenH - Gdx.input.getY();

            if (mx >= btnX && mx <= btnX + btnW &&
                my >= btnY && my <= btnY + btnH) {
                return true;
            }
        }

        return false;
    }
}

package io.github.example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

public class UI {

    // Layout settings
    private final int margin = 16;
    private final int barWidth = 220;
    private final int barHeight = 20;
    private final int labelOffsetY = 18;  // distance of label above bar

    public UI() {
    }

    /**
     * Draws HEALTH (bottom-left) and HUNGER (bottom-right) bars.
     * If health <= 0, also draws a red death overlay with a Respawn button.
     *
     * @return true if the Respawn button was clicked this frame.
     */
    public boolean draw(ShapeRenderer shapeRenderer,
                        SpriteBatch batch,
                        BitmapFont font,
                        float health, float maxHealth,
                        float hunger, float maxHunger) {

        // Clamp ratios between 0 and 1
        float healthRatio = maxHealth <= 0 ? 0 : Math.max(0f, Math.min(health / maxHealth, 1f));
        float hungerRatio = maxHunger <= 0 ? 0 : Math.max(0f, Math.min(hunger / maxHunger, 1f));

        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        // Screen-space projection (HUD)
        Matrix4 uiMatrix = new Matrix4().setToOrtho2D(0, 0, screenW, screenH);
        shapeRenderer.setProjectionMatrix(uiMatrix);
        batch.setProjectionMatrix(uiMatrix);

        // Positions
        int healthX = margin;
        int healthY = margin;

        int hungerX = screenW - margin - barWidth;
        int hungerY = margin;

        // Enable blending for soft background rectangles
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // --- HEALTH bar background + fill (bottom-left) ---
        shapeRenderer.setColor(0f, 0f, 0f, 0.4f); // background
        shapeRenderer.rect(healthX, healthY, barWidth, barHeight);

        shapeRenderer.setColor(Color.RED);        // health fill
        shapeRenderer.rect(healthX, healthY, barWidth * healthRatio, barHeight);

        // --- HUNGER bar background + fill (bottom-right) ---
        shapeRenderer.setColor(0f, 0f, 0f, 0.4f); // background
        shapeRenderer.rect(hungerX, hungerY, barWidth, barHeight);

        shapeRenderer.setColor(Color.ORANGE);     // hunger fill
        shapeRenderer.rect(hungerX, hungerY, barWidth * hungerRatio, barHeight);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Draw labels
        batch.begin();
        Color oldColor = font.getColor().cpy();

        font.setColor(Color.WHITE);
        font.draw(batch, "HEALTH", healthX, healthY + barHeight + labelOffsetY);
        font.draw(batch, "HUNGER (E to eat)", hungerX, hungerY + barHeight + labelOffsetY);

        font.setColor(oldColor);
        batch.end();

        // --- Death overlay if health is 0 ---
        if (health <= 0f) {
            return drawDeathOverlay(shapeRenderer, batch, font, screenW, screenH);
        }

        return false;
    }

    /**
     * Draws a red translucent full-screen overlay with "You died!"
     * and a clickable "Respawn" button. Returns true if the button is clicked.
     */
    private boolean drawDeathOverlay(ShapeRenderer shapeRenderer,
                                     SpriteBatch batch,
                                     BitmapFont font,
                                     int screenW,
                                     int screenH) {

        // Full-screen translucent red overlay
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        Matrix4 uiMatrix = new Matrix4().setToOrtho2D(0, 0, screenW, screenH);
        shapeRenderer.setProjectionMatrix(uiMatrix);
        batch.setProjectionMatrix(uiMatrix);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.5f, 0f, 0f, 0.7f); // dark red, mostly opaque
        shapeRenderer.rect(0, 0, screenW, screenH);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Prepare text layout helper
        GlyphLayout layout = new GlyphLayout();

        batch.begin();
        Color oldColor = font.getColor().cpy();
        float oldScaleX = font.getData().scaleX;
        float oldScaleY = font.getData().scaleY;

        // "You died!" text (bigger, centered)
        String deathText = "You died!";
        font.getData().setScale(2f);
        font.setColor(Color.WHITE);

        layout.setText(font, deathText);
        float textX = (screenW - layout.width) / 2f;
        float textY = screenH / 2f + 60f;
        font.draw(batch, deathText, textX, textY);

        // Respawn button dimensions (centered below text)
        int buttonWidth = 220;
        int buttonHeight = 50;
        int buttonX = (screenW - buttonWidth) / 2;
        int buttonY = screenH / 2 - 40;

        batch.end();

        // Draw the button background with slight highlight
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.8f);
        shapeRenderer.rect(buttonX, buttonY, buttonWidth, buttonHeight);
        shapeRenderer.setColor(1f, 1f, 1f, 0.15f);
        shapeRenderer.rect(buttonX, buttonY + buttonHeight - 3, buttonWidth, 3);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Draw button label
        batch.begin();
        font.getData().setScale(1.5f);
        font.setColor(Color.WHITE);

        String btnText = "Respawn";
        layout.setText(font, btnText);
        float btnTextX = buttonX + (buttonWidth - layout.width) / 2f;
        float btnTextY = buttonY + buttonHeight / 2f + layout.height / 2f;
        font.draw(batch, btnText, btnTextX, btnTextY);

        // Restore font
        font.getData().setScale(oldScaleX, oldScaleY);
        font.setColor(oldColor);
        batch.end();

        // --- Click detection on Respawn button ---
        if (Gdx.input.justTouched()) {
            int mx = Gdx.input.getX();
            int my = screenH - Gdx.input.getY(); // convert from top-left to bottom-left

            if (mx >= buttonX && mx <= buttonX + buttonWidth &&
                my >= buttonY && my <= buttonY + buttonHeight) {
                return true;
            }
        }

        return false;
    }
}

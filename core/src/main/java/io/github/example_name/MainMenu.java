package io.github.example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import javax.swing.JOptionPane;

public class MainMenu extends ApplicationAdapter {

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Texture menuBackground;

    private Host host;
    private Client client;

    private Core core;
    private boolean inGame = false;
    private boolean coreCreated = false;

    private static class Button {
        float x, y, w, h;
        String label;
    }

    private Texture titleTexture;
    private float titleScale = 0.65f;

    private Button hostButton;
    private Button joinButton;
    private Button exitButton;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        menuBackground = new Texture(Gdx.files.internal("menu.png"));
        titleTexture = new Texture(Gdx.files.internal("menutitle.png"));

        createButtons();
    }

    private void createButtons() {
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        float buttonWidth = 260f;
        float buttonHeight = 60f;
        float spacing = 18f;

        float centerX = (screenW - buttonWidth) / 2f;
        float baseY = screenH / 2f - 120f;

        hostButton = new Button();
        hostButton.x = centerX;
        hostButton.y = baseY + buttonHeight + spacing;
        hostButton.w = buttonWidth;
        hostButton.h = buttonHeight;
        hostButton.label = "Host";

        joinButton = new Button();
        joinButton.x = centerX;
        joinButton.y = baseY;
        joinButton.w = buttonWidth;
        joinButton.h = buttonHeight;
        joinButton.label = "Join";

        exitButton = new Button();
        exitButton.x = centerX;
        exitButton.y = baseY - buttonHeight - spacing;
        exitButton.w = buttonWidth;
        exitButton.h = buttonHeight;
        exitButton.label = "Exit";
    }

    @Override
    public void render() {
        if (inGame) {
            if (!coreCreated) {
                core = new Core();
                core.create();
                coreCreated = true;
            }
            core.render();
            return;
        }

        renderMenu();
    }

    private void renderMenu() {
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);

        Matrix4 uiMatrix = new Matrix4().setToOrtho2D(0, 0, screenW, screenH);
        batch.setProjectionMatrix(uiMatrix);
        shapeRenderer.setProjectionMatrix(uiMatrix);

        batch.begin();
        batch.draw(menuBackground, 0, 0, screenW, screenH);
        batch.end();

        batch.begin();
        if (titleTexture != null) {
            float scale = titleScale;
            float titleW = titleTexture.getWidth() * scale;
            float titleH = titleTexture.getHeight() * scale;

            float titleX = (screenW - titleW) / 2f;
            float titleY = hostButton.y + hostButton.h + 50f;

            batch.draw(titleTexture, titleX, titleY, titleW, titleH);
        }
        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

        drawButtonPanel(hostButton);
        drawButtonPanel(joinButton);
        drawButtonPanel(exitButton);

        shapeRenderer.end();
        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);

        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.4f);

        drawButtonLabel(hostButton);
        drawButtonLabel(joinButton);
        drawButtonLabel(exitButton);

        font.getData().setScale(1.0f);
        batch.end();

        handleMenuInput();
    }

    private void drawButtonPanel(Button b) {
        shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
        shapeRenderer.rect(b.x, b.y, b.w, b.h);

        shapeRenderer.setColor(1f, 1f, 1f, 0.15f);
        shapeRenderer.rect(b.x, b.y + b.h - 4, b.w, 4);
    }

    private void drawButtonLabel(Button b) {
        GlyphLayout layout = new GlyphLayout(font, b.label);
        float textX = b.x + (b.w - layout.width) / 2f;
        float textY = b.y + (b.h + layout.height) / 2f;
        font.draw(batch, layout, textX, textY);
    }

    private void handleMenuInput() {
        if (!Gdx.input.justTouched()) return;

        int screenH = Gdx.graphics.getHeight();
        int mx = Gdx.input.getX();
        int my = screenH - Gdx.input.getY();

        if (isInside(mx, my, hostButton)) {
            String username = JOptionPane.showInputDialog(
                null,
                "Enter Username:",
                "Host Game",
                JOptionPane.QUESTION_MESSAGE
            );
            if (username == null || username.trim().isEmpty()) return;
            username = username.trim();

            if (!coreCreated) {
                core = new Core();
                core.create();
                coreCreated = true;
            }

            if (host == null) {
                host = new Host(
                    5000,
                    message -> Gdx.app.postRunnable(() -> {
                        if (core != null) core.receiveNetworkMessage(message);
                    })
                );
                host.start();
            }

            core.setLocalUsername(username);
            core.setNetwork(host, null);
            inGame = true;

        } else if (isInside(mx, my, joinButton)) {

            String username = JOptionPane.showInputDialog(
                null,
                "Enter Username:",
                "Join Game",
                JOptionPane.QUESTION_MESSAGE
            );
            if (username == null || username.trim().isEmpty()) return;
            username = username.trim();

            String ip = JOptionPane.showInputDialog(
                null,
                "Enter Host IP:",
                "Join Game",
                JOptionPane.QUESTION_MESSAGE
            );
            if (ip == null || ip.trim().isEmpty()) return;
            ip = ip.trim();

            if (!coreCreated) {
                core = new Core();
                core.create();
                coreCreated = true;
            }

            client = new Client(
                ip,
                5000,
                username,
                message -> Gdx.app.postRunnable(() -> {
                    if (core != null) core.receiveNetworkMessage(message);
                })
            );

            boolean connected = client.connect();
            if (!connected) {
                JOptionPane.showMessageDialog(
                    null,
                    "Failed to join: could not connect to host.",
                    "Join Failed",
                    JOptionPane.ERROR_MESSAGE
                );
                client = null;
                return;
            }

            core.setLocalUsername(username);
            core.setNetwork(null, client);
            inGame = true;

        } else if (isInside(mx, my, exitButton)) {
            Gdx.app.exit();
        }
    }

    private boolean isInside(int mx, int my, Button b) {
        return mx >= b.x && mx <= b.x + b.w &&
            my >= b.y && my <= b.y + b.h;
    }

    @Override
    public void resize(int width, int height) {
        if (!inGame) {
            createButtons();
        } else if (coreCreated) {
            core.resize(width, height);
        }
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (font != null) font.dispose();
        if (menuBackground != null) menuBackground.dispose();
        if (coreCreated && core != null) core.dispose();
    }
}

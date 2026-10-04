package dev.caecorthus.sparkfactionapi.client.inventory;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;

/**
 * Blue recolor of Wathe's strip texture, generated in memory from whatever strip the active resource packs provide; no
 * Wathe artwork is shipped. As a {@code ResourceTexture}, the texture manager rebuilds it on every resource reload.
 * Wathe 背包条材质的蓝色版本，在内存中由当前资源包提供的背包条生成，不随模组分发任何 Wathe 美术资源。
 * 作为 {@code ResourceTexture}，纹理管理器会在每次资源重载时重新生成它。
 */
public final class SecondRowTexture extends ResourceTexture {
    private static final Identifier ID = Identifier.of("sparkfactionapi", "dynamic/limited_inventory_second_row");
    private static final int STRIP_WIDTH = 176;
    private static final int STRIP_HEIGHT = 32;
    private static boolean registered;

    private SecondRowTexture(Identifier source) {
        super(source);
    }

    /** Render thread only; registers the texture on first use. / 仅限渲染线程；首次使用时注册纹理。 */
    public static Identifier id() {
        if (!registered) {
            registered = true;
            MinecraftClient.getInstance().getTextureManager()
                    .registerTexture(ID, new SecondRowTexture(LimitedInventoryScreen.BACKGROUND_TEXTURE));
        }
        return ID;
    }

    @Override
    protected TextureData loadTextureData(ResourceManager resourceManager) {
        TextureData data = TextureData.load(resourceManager, this.location);
        try {
            NativeImage image = data.getImage();
            if (image.getFormat() == NativeImage.Format.RGBA) {
                recolor(image);
            }
        } catch (IOException ignored) {
            // load() rethrows the stored exception; the texture manager then logs it and uses the missing texture.
            // load() 会重新抛出已保存的异常，纹理管理器随后记录日志并使用缺失纹理。
        }
        return data;
    }

    private static void recolor(NativeImage image) {
        int stripWidth = Math.min(STRIP_WIDTH, image.getWidth());
        int stripHeight = Math.min(STRIP_HEIGHT, image.getHeight());
        float darkest = Float.MAX_VALUE;
        float brightest = -Float.MAX_VALUE;
        for (int y = 0; y < stripHeight; y++) {
            for (int x = 0; x < stripWidth; x++) {
                int pixel = image.getColor(x, y);
                if (SecondRowPalette.alpha(pixel) != 0) {
                    float luminance = SecondRowPalette.luminance(pixel);
                    darkest = Math.min(darkest, luminance);
                    brightest = Math.max(brightest, luminance);
                }
            }
        }
        if (darkest > brightest) {
            return;
        }
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setColor(x, y, SecondRowPalette.recolor(image.getColor(x, y), darkest, brightest));
            }
        }
    }
}

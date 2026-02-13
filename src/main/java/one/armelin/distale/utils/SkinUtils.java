package one.armelin.distale.utils;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import one.armelin.distale.DisTale;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static one.armelin.distale.utils.ImageUtils.loadImageFromAsset;

public class SkinUtils {
    public static BufferedImage getSkinToneGradient(PlayerSkin playerSkin){
        CosmeticsModule cosmeticsModule = CosmeticsModule.get();
        CosmeticRegistry registry = cosmeticsModule.getRegistry();

        if(playerSkin.bodyCharacteristic != null){
            String skinTone = playerSkin.bodyCharacteristic.split("\\.")[1];
            return loadImageFromAsset(registry.getGradientSets().get("Skin").getGradients().get(skinTone).getTexture());
        }
        return null;
    }

    public static BufferedImage getSkinFaceGrayscale(PlayerSkin playerSkin){
        CosmeticsModule cosmeticsModule = CosmeticsModule.get();
        CosmeticRegistry registry = cosmeticsModule.getRegistry();

        if(playerSkin.face != null){
            return loadImageFromAsset(registry.getFaces().get(playerSkin.face).getGreyscaleTexture());
        }
        return null;
    }

    public static BufferedImage getSkinFace(PlayerSkin playerSkin){
        if(haveNull(
                playerSkin.bodyCharacteristic,
                playerSkin.face
        )){
            return null;
        }
        BufferedImage faceGrayscale = getSkinFaceGrayscale(playerSkin);
        BufferedImage skinToneGradient = getSkinToneGradient(playerSkin);
        return ImageUtils.applyGradientToSkin(skinToneGradient, faceGrayscale);
    }

    public static BufferedImage getSkinEyes(PlayerSkin playerSkin){
        CosmeticsModule cosmeticsModule = CosmeticsModule.get();
        CosmeticRegistry registry = cosmeticsModule.getRegistry();

        if(playerSkin.eyes != null){
            return loadImageFromAsset(registry.getEyes().get(playerSkin.eyes.split("\\.")[0]).getGreyscaleTexture());
        }
        return null;
    }

    public static BufferedImage getEyeBackground(PlayerSkin playerSkin){
        BufferedImage eye = getSkinEyes(playerSkin);
        if(eye != null) {
            BufferedImage background = eye.getSubimage(0, 0, 32, 16);
            if(playerSkin.eyes.startsWith("Demonic_Eyes")){
                BufferedImage eyeGradient = getEyeGradient(playerSkin);
                return ImageUtils.applyGradientToSkin(eyeGradient, background);
            }
            return background;
        }
        return null;
    }

    public static BufferedImage getEyeGrayscale(PlayerSkin playerSkin){
        BufferedImage eye = getSkinEyes(playerSkin);
        if(eye != null) {
            return eye.getSubimage(0, 16, 32, 16);
        }
        return null;
    }

    public static BufferedImage getEyeGradient(PlayerSkin playerSkin){
        CosmeticsModule cosmeticsModule = CosmeticsModule.get();
        CosmeticRegistry registry = cosmeticsModule.getRegistry();

        if(playerSkin.eyes != null){
            String eyeColor = playerSkin.eyes.split("\\.")[1];
            return loadImageFromAsset(registry.getGradientSets().get("Eyes_Gradient").getGradients().get(eyeColor).getTexture());
        }
        return null;
    }

    public static BufferedImage getEyes(PlayerSkin playerSkin){
        BufferedImage eyeGrayscale = getEyeGrayscale(playerSkin);
        BufferedImage eyeGradient = getEyeGradient(playerSkin);
        BufferedImage eyeWithGradient = ImageUtils.applyGradientToSkin(eyeGradient, eyeGrayscale);

        return ImageUtils.resizeByPercentage(eyeWithGradient, 80);
    }

    public static BufferedImage getMouth(PlayerSkin playerSkin){
        CosmeticsModule cosmeticsModule = CosmeticsModule.get();
        CosmeticRegistry registry = cosmeticsModule.getRegistry();

        if(playerSkin.mouth != null) {
            BufferedImage mouthGrayscale = loadImageFromAsset(registry.getMouths().get(playerSkin.mouth).getGreyscaleTexture());
            if(mouthGrayscale != null) {
                BufferedImage mouths = ImageUtils.applyGradientToSkin(getSkinToneGradient(playerSkin), mouthGrayscale);
                return mouths.getSubimage(3,4,14,4);
            }
        }
        return null;
    }

    public static BufferedImage getHairGradientByColor(String color){
        CosmeticsModule cosmeticsModule = CosmeticsModule.get();
        CosmeticRegistry registry = cosmeticsModule.getRegistry();

        return loadImageFromAsset(registry.getGradientSets().get("Hair").getGradients().get(color).getTexture());
    }

    public static BufferedImage getEyebrows(PlayerSkin playerSkin){
        CosmeticsModule cosmeticsModule = CosmeticsModule.get();
        CosmeticRegistry registry = cosmeticsModule.getRegistry();

        if(playerSkin.eyebrows != null) {
            BufferedImage eyebrowsGrayscale = loadImageFromAsset(registry.getEyebrows().get(playerSkin.eyebrows.split("\\.")[0]).getGreyscaleTexture());
            if(eyebrowsGrayscale != null) {
                BufferedImage eyebrowsColorGradient = getHairGradientByColor(playerSkin.eyebrows.split("\\.")[1]);
                BufferedImage eyebrows = ImageUtils.applyGradientToSkin(eyebrowsColorGradient, eyebrowsGrayscale);
                if(playerSkin.eyebrows.startsWith("Bushy")){
                    return eyebrows.getSubimage(3,0,15,8);
                }
                if(playerSkin.eyebrows.startsWith("Shaved")){
                    return eyebrows;
                }
                return eyebrows.getSubimage(1,0,16,6);
            }
        }
        return null;
    }

    public static BufferedImage buildFace(PlayerSkin playerSkin){
        if(haveNull(
                playerSkin.bodyCharacteristic,
                playerSkin.face,
                playerSkin.eyes,
                playerSkin.mouth
        )){
            return null;
        }

        BufferedImage skinFace = getSkinFace(playerSkin);
        BufferedImage eyeBackground = getEyeBackground(playerSkin);
        BufferedImage eyes = getEyes(playerSkin);
        BufferedImage l_eye = eyes.getSubimage(0, 0, 13, 13);
        BufferedImage r_eye = eyes.getSubimage(13, 0, 13, 13);
        BufferedImage mouth = getMouth(playerSkin);

        BufferedImage face = ImageUtils.createTransparentBackground(32, 32);
        face = ImageUtils.compose(face, eyeBackground, 0, 7);
        face = ImageUtils.compose(face, l_eye, 2, 10);
        face = ImageUtils.compose(face, r_eye, 17, 10);
        face = ImageUtils.compose(face, skinFace, 1, 0);
        face = ImageUtils.compose(face, mouth, 9, 22);

        if(playerSkin.eyebrows != null){
            BufferedImage l_eyebrow = getEyebrows(playerSkin);
            BufferedImage r_eyebrow = ImageUtils.flipHorizontally(l_eyebrow);
            if(playerSkin.eyebrows.startsWith("Shaved")) {
                BufferedImage eyebrows = l_eyebrow;
                l_eyebrow = eyebrows.getSubimage(5, 20, 11, 2);
                r_eyebrow = ImageUtils.flipHorizontally(eyebrows.getSubimage(5, 8, 11, 2));
                face = ImageUtils.compose(face, l_eyebrow, 3, 10);
                face = ImageUtils.compose(face, r_eyebrow, 18, 10);
            }else{
                if(playerSkin.eyebrows.startsWith("Bushy")){
                    face = ImageUtils.compose(face, l_eyebrow, 1, 6);
                    face = ImageUtils.compose(face, r_eyebrow, 16, 6);
                }else{
                    face = ImageUtils.compose(face, l_eyebrow, 0, 8);
                    face = ImageUtils.compose(face, r_eyebrow, 16, 8);
                }
            }
        }

        return ImageUtils.resizeByPercentage(face.getSubimage(1,0,30,28), 400);
    }

    private static boolean haveNull(Object... items) {
        for (Object item : items) {
            if (item == null) {
                return true;
            }
        }
        return false;
    }

    public static void generateSkin(PlayerRef ref, @Nullable World world){
        DisTale.LOGGER.atInfo().log("Player Valid? %s", ref.isValid());


        String playerName = ref.getUsername();
        DisTale.LOGGER.atInfo().log("Player %s is missing skin, generating...", playerName);
        Store<EntityStore> store = Objects.requireNonNull(ref.getReference()).getStore();

        CompletableFuture<Void> future = new CompletableFuture<>();

        if(world == null){
            world = store.getExternalData().getWorld();
        }


        world.execute(() -> {
            try {
                PlayerSkinComponent playerSkinComponent = store.getComponent(
                        ref.getReference(),
                        PlayerSkinComponent.getComponentType()
                );

                if(playerSkinComponent != null){
                    PlayerSkin playerSkin = playerSkinComponent.getPlayerSkin();
                    DisTale.LOGGER.atInfo().log("Player %s face image (base64): %s", playerName, ImageUtils.imageToBase64(SkinUtils.buildFace(playerSkin), "png"));
                    //DisTale.playerSkins.put(playerName, SkinUtils.buildFace(playerSkin));
                }
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            DisTale.LOGGER.atSevere().withCause(e).log("Error generating skin for player %s", playerName);
        }
    }
}
